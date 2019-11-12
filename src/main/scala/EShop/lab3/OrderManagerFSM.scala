package EShop.lab3

import EShop.lab2.{CartActor, Checkout}
import EShop.lab3.OrderManager._
import akka.actor.FSM

class OrderManagerFSM extends FSM[State, Data] {

  startWith(Uninitialized, Empty)

  when(Uninitialized) {
    case Event(AddItem(item), _) =>
      val cartActor = context.actorOf(CartActor.props())
      cartActor ! CartActor.AddItem(item)
      sender() ! Done
      goto(Open) using CartData(cartActor)
  }

  when(Open) {
    case Event(Buy, CartData(cartRef)) =>
      cartRef ! CartActor.StartCheckout
      stay() using CartDataWithSender(cartRef, sender())
    case Event(CartActor.CheckoutStarted(checkoutRef, _), CartDataWithSender(_, sender)) =>
      sender ! Done
      goto(InCheckout) using InCheckoutData(checkoutRef)
  }

  when(InCheckout) {
    case Event(SelectDeliveryAndPaymentMethod(delivery, payment), InCheckoutData(checkoutRef)) =>
      checkoutRef ! Checkout.SelectDeliveryMethod(delivery)
      checkoutRef ! Checkout.SelectPayment(payment)
      stay() using InCheckoutDataWithSender(checkoutRef, sender())
    case Event(Checkout.PaymentStarted(paymentRef), InCheckoutDataWithSender(_, sender)) =>
      sender ! Done
      goto(InPayment) using InPaymentData(paymentRef)
  }

  when(InPayment) {
    case Event(Pay, InPaymentData(paymentRef)) =>
      paymentRef ! Payment.DoPayment
      stay() using InPaymentDataWithSender(paymentRef, sender())
    case Event(Payment.PaymentConfirmed, InPaymentDataWithSender(_, sender)) =>
      sender ! Done
      goto(Finished)
  }

  when(Finished) {
    case _ =>
      sender ! "order manager finished job"
      stay()
  }

}
