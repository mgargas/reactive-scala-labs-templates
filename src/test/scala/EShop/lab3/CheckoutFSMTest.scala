package EShop.lab3

import EShop.lab2.{Cart, CartFSM, Checkout, CheckoutFSM}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestFSMRef, TestKit}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class CheckoutFSMTest
  extends TestKit(ActorSystem("CheckoutTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  trait CheckoutFSMActorTest {}

  it should "Send close confirmation to cart" in {
    val item                                       = "Item"
    val cart                                       = TestFSMRef(new CartFSM())
    val mustBeTypedProperly: TestActorRef[CartFSM] = cart
    val checkoutActor                              = system.actorOf(CheckoutFSM.props(cart))

    checkoutActor ! Checkout.StartCheckout
    checkoutActor ! Checkout.SelectDeliveryMethod("method")
    checkoutActor ! Checkout.SelectPayment("payment")
    checkoutActor ! Checkout.ReceivePayment

    assert(cart.stateName == CartFSM.Status.Empty)
    assert(cart.stateData == Cart.empty)
  }

}
