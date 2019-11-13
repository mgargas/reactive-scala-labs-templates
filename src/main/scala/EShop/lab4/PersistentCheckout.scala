package EShop.lab4

import EShop.lab3.Payment
import akka.actor.{ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.concurrent.duration._

object PersistentCheckout {

  def props(cartActor: ActorRef, persistenceId: String) =
    Props(new PersistentCheckout(cartActor, persistenceId))
}

class PersistentCheckout(
  cartActor: ActorRef,
  val persistenceId: String
) extends PersistentActor {

  import EShop.lab2.Checkout._
  val timerDuration     = 1.seconds
  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)

  override def receiveCommand: Receive = LoggingReceive.withLabel("State: receiveCommand") {
    case StartCheckout => persist(CheckoutStarted)(event => updateState(event))
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive.withLabel("[State: selectingDelivery]") {
    case CancelCheckout => persist(CheckoutCancelled)(event => updateState(event, Some(timer)))
    case ExpireCheckout => persist(CheckoutCancelled)(event => updateState(event, Some(timer)))
    case SelectDeliveryMethod(method) =>
      persist(DeliveryMethodSelected(method))(event => updateState(event, Some(timer)))
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive.withLabel("[State: processingPayment]") {
    case CancelCheckout => persist(CheckoutCancelled)(event => updateState(event, Some(timer)))
    case ExpireCheckout => persist(CheckoutCancelled)(event => updateState(event, Some(timer)))
    case SelectPayment(payment) =>
      val paymentActor = context.actorOf(Payment.props(payment, sender(), self))
      sender() ! PaymentStarted(paymentActor)
      persist(PaymentStarted(paymentActor))(event => updateState(event))
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive.withLabel("[State: processingPayment]") {
    case CancelCheckout => persist(CheckoutCancelled)(event => updateState(event, Some(timer)))
    case ExpireCheckout => persist(CheckoutCancelled)(event => updateState(event, Some(timer)))
    case ReceivePayment =>
      cartActor ! CheckOutClosed
      persist(CheckOutClosed)(event => updateState(event, Some(timer)))
  }

  def cancelled: Receive = LoggingReceive.withLabel("[State: cancelled]") {
    case _ => log.info("Checkout already cancelled")
  }

  def closed: Receive = LoggingReceive.withLabel("[State: closed]") {
    case _ => log.info("Checkout already closed")
  }

  override def receiveRecover: Receive = {
    case event: Event     => updateState(event)
    case _: SnapshotOffer => log.error("Received unhandled snapshot offer")
  }

  private def scheduleCheckoutTimer: Cancellable =
    scheduler.scheduleOnce(timerDuration, self, ExpireCheckout)(context.system.dispatcher)

  private def schedulePaymentTimer: Cancellable =
    scheduler.scheduleOnce(timerDuration, self, ExpirePayment)(context.system.dispatcher)

  private def updateState(event: Event, maybeTimer: Option[Cancellable] = None): Unit = {
    maybeTimer.foreach(_.cancel())
    event match {
      case CheckoutStarted           => context become selectingDelivery(scheduleCheckoutTimer)
      case DeliveryMethodSelected(_) => context become selectingPaymentMethod(scheduleCheckoutTimer)
      case CheckOutClosed            => context become closed
      case CheckoutCancelled         => context become cancelled
      case PaymentStarted(_)         => context become processingPayment(schedulePaymentTimer)
    }
  }
}
