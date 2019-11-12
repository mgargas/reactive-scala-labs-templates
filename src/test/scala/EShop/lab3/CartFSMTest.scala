package EShop.lab3

import EShop.lab2.{Cart, CartActor, CartFSM}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestFSMRef, TestKit}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class CartFSMTest
  extends TestKit(ActorSystem("CartTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  trait CartFSMActorTest {
    val fsm                                        = TestFSMRef(new CartFSM())
    val mustBeTypedProperly: TestActorRef[CartFSM] = fsm
    val item                                       = "Item"
  }
  //use GetItems command which was added to make test easier
  it should "add item properly" in new CartFSMActorTest {
    assert(fsm.stateName == CartFSM.Status.Empty)
    assert(fsm.stateData == Cart.empty)
    fsm ! CartActor.AddItem(item)
    assert(fsm.stateName == CartFSM.Status.NonEmpty)
    assert(fsm.stateData == Cart(Seq(item)))
  }

  it should "be empty after adding and removing the same item" in new CartFSMActorTest {
    assert(fsm.stateName == CartFSM.Status.Empty)
    assert(fsm.stateData == Cart.empty)
    fsm ! CartActor.AddItem(item)
    fsm ! CartActor.RemoveItem(item)
    assert(fsm.stateName == CartFSM.Status.Empty)
    assert(fsm.stateData == Cart.empty)
  }

  it should "start checkout" in new CartFSMActorTest {
    assert(fsm.stateName == CartFSM.Status.Empty)
    assert(fsm.stateData == Cart.empty)
    fsm ! CartActor.AddItem(item)
    assert(fsm.stateName == CartFSM.Status.NonEmpty)
    assert(fsm.stateData == Cart(Seq(item)))
    fsm ! CartActor.StartCheckout
    assert(fsm.stateName == CartFSM.Status.InCheckout)
    assert(fsm.stateData == Cart(Seq(item)))
  }
}
