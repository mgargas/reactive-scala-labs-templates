package EShop.lab3

import EShop.lab2.CartActor
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class CartTest
  extends TestKit(ActorSystem("CartTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  implicit val timeout: Timeout = 1.second

  trait CartActorTest {
    val actorRef = TestActorRef(new CartActor())
    val actor    = actorRef.underlyingActor
    val item     = "Item"

  }

  //use GetItems command which was added to make test easier
  it should "add item properly" in new CartActorTest {
    actorRef ! CartActor.AddItem(item)
    (actorRef ? CartActor.GetItems).mapTo[Seq[Any]].futureValue shouldBe Seq(item)
  }

  it should "be empty after adding and removing the same item" in new CartActorTest {
    actorRef ! CartActor.AddItem(item)
    actorRef ! CartActor.RemoveItem(item)
    (actorRef ? CartActor.GetItems).mapTo[Seq[Any]].futureValue shouldBe Seq.empty
  }

  //asynchronous
  it should "start checkout" in {
    val item = "Machupichu gold"
    val cartActor = system.actorOf(CartActor.props())
    cartActor ! CartActor.AddItem(item)
    cartActor ! CartActor.StartCheckout
    expectMsg(_: CartActor.CheckoutStarted)
  }
}
