package EShop.lab4

import EShop.lab2.{Cart, Checkout}
import akka.actor.{Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.concurrent.duration._

object PersistentCartActor {

  def props(persistenceId: String) = Props(new PersistentCartActor(persistenceId))
}

class PersistentCartActor(
  val persistenceId: String
) extends PersistentActor {

  import EShop.lab2.CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5.seconds

  private def scheduleTimer: Cancellable =
    context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)(context.system.dispatcher)


  override def receiveCommand: Receive = empty

  private def updateState(event: Event, timer: Option[Cancellable] = None): Unit = {
    timer.foreach(_.cancel())
    event match {
      case CartExpired | CheckoutClosed       => context become empty
      case CheckoutCancelled(cart)            => context become nonEmpty(cart, scheduleTimer)
      case ItemAdded(item, cart)              => context become nonEmpty(cart.addItem(item), scheduleTimer)
      case CartEmptied                        => context become empty
      case ItemRemoved(item, cart) if cart.contains(item) && cart.size == 1            => context become empty
      case ItemRemoved(item, cart) if cart.contains(item) => context become nonEmpty(cart.removeItem(item), scheduleTimer)
      case CheckoutStarted(_, cart) => context become inCheckout(cart)
    }
  }

  def empty: Receive = LoggingReceive.withLabel("[State: empty]") {
    case AddItem(item) =>
      persist(ItemAdded(item, Cart.empty))(event => updateState(event, None))
    case GetItems => sender ! Cart.empty
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive.withLabel("[State: nonEmpty]") {
    case AddItem(item) =>
      persist(ItemAdded(item, cart))(event => updateState(event, Some(timer)))
    case RemoveItem(item) if cart.contains(item) && cart.size == 1 =>
      persist(CartEmptied)(event => updateState(event, Some(timer)))
    case RemoveItem(item) if cart.contains(item) =>
      persist(ItemRemoved(item, cart))(event => updateState(event, Some(timer)))
    case StartCheckout =>
      val persistentCheckoutActor = context.actorOf(PersistentCheckout.props(self, "persistent-checkout"), "checkout")
      val event = CheckoutStarted(persistentCheckoutActor, cart)
      persist(event)(_ => {
        persistentCheckoutActor ! Checkout.StartCheckout
        sender() ! event
        updateState(event, Some(timer))
      })
    case ExpireCart => persist(CartExpired)(event => updateState(event, Some(timer)))
    case GetItems   => sender() ! cart.items
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive.withLabel("[State: inCheckout]") {
    case CloseCheckout  => persist(CheckoutClosed)(updateState(_))
    case CancelCheckout => persist(CheckoutCancelled(cart))(updateState(_))
  }

  override def receiveRecover: Receive = {
    case event: Event     => updateState(event)
    case _: SnapshotOffer => log.error("Received unhandled snapshot offer")
  }
}
