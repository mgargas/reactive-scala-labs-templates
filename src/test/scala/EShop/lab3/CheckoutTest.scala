package EShop.lab3

import EShop.lab2.Checkout
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class CheckoutTest
  extends TestKit(ActorSystem("CheckoutTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  it should "Send close confirmation to cart" in {
    val item          = "Item"
    val cart          = TestProbe()
    val checkoutActor = system.actorOf(Checkout.props(cart.ref))

    checkoutActor ! Checkout.StartCheckout
    checkoutActor ! Checkout.SelectDeliveryMethod("method")
    checkoutActor ! Checkout.SelectPayment("payment")
    checkoutActor ! Checkout.ReceivePayment

    cart.expectMsg(Checkout.CheckOutClosed)

  }

}
