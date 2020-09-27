package it.unibo.pps1920.motoscala.controller.mediation

import scala.reflect.ClassTag

trait Mediator extends EventSubject
object Mediator {
  import Types._
  private class MediatorImpl extends Mediator {
    private var observers: Map[Observer, EventType] = Map()

    override def publishEvent[T: ClassTag](ev: T): Unit = observers
      .filter(o => o._2.isAssignableFrom(implicitly[ClassTag[T]].runtimeClass))
      .map(_._1.asInstanceOf[EventObserver[T]])
      .foreach(_.notify(ev))
    override def subscribe[T: ClassTag](observer: EventObserver[T]*): Unit =
      observers = observers ++ observer.map(o => o -> implicitly[ClassTag[T]].runtimeClass).toSet
    override def unsubscribe[T](observer: EventObserver[T]*): Unit = observers = observers -- observer
  }
  object Types {
    type EventType = Class[_]
    type Observer = EventObserver[_]
  }
  def apply(): Mediator = new MediatorImpl()
}


