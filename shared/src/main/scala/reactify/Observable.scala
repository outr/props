package reactify

import scala.concurrent.{Future, Promise}

/**
  * Observable, as the name suggests, observes values being fired against it. This is the core functionality of Reactify
  * and provides the infrastructure used by Channel, Val, Var, Prop, and Dep.
  *
  * @tparam T the type of value this Observable will receive
  */
trait Observable[T] {
  private[reactify] var observers = List.empty[Listener[T]]

  /**
    * Attaches a function to listen to values fired against this Observable.
    *
    * @param f function listener
    * @return the supplied function. This reference is useful for detaching the function later
    */
  def attach(f: T => Unit): Listener[T] = observe(new FunctionListener[T](f))

  def observe(listener: Listener[T]): Listener[T] = synchronized {
    observers = listener :: observers
    listener
  }

  /**
    * Works like `attach`, but doesn't receive the fired value.
    *
    * @param f function to invoke on fire
    * @return listener
    */
  def on(f: => Unit): Listener[T] = attach(_ => f)

  /**
    * Detaches a function from listening to this Observable.
    *
    * @param listener function listener that was previously attached
    */
  def detach(listener: Listener[T]): Unit = synchronized {
    observers = observers.filterNot(_ eq listener)
  }

  /**
    * Invokes the listener only one time and then detaches itself. If supplied, the condition filters the scenarios in
    * which the listener will be invoked.
    *
    * @param f the function listener
    * @param condition the condition under which the listener will be invoked. Defaults to always return true.
    */
  def once(f: T => Unit, condition: T => Boolean = (t: T) => true): Listener[T] = {
    var listener: Listener[T] = null
    listener = new FunctionListener[T](f) {
      override def apply(value: T): Unit = if (condition(value)) {
        detach(listener)
        super.apply(value)
      }
    }
    listener
  }

  /**
    * Returns a Future[T] that represents the value of the next firing of this Observable.
    *
    * @param condition the condition under which the listener will be invoked. Defaults to always return true.
    */
  def future(condition: T => Boolean = (t: T) => true): Future[T] = {
    val promise = Promise[T]
    once(promise.success, condition)
    promise.future
  }

  /**
    * Works similarly to `attach`, but also references the previous value that was fired. This is useful when you need
    * to handle changes, not just new values.
    *
    * @param listener the ChangeListener
    * @return the listener attached. This can be passed to `detach` to remove this listener
    */
  def changes(listener: ChangeListener[T]): Listener[T] = attach(ChangeListener.createFunction(listener, None))

  protected[reactify] def fire(value: T): Unit = fireRecursive(value, Invocation().reset(), observers)

  final protected def fireRecursive(value: T, invocation: Invocation, observers: List[Listener[T]]): Unit = {
    if (observers.nonEmpty && !invocation.isStopped) {
      val listener = observers.head
      listener(value)

      fireRecursive(value, invocation, observers.tail)
    }
  }

  /**
    * Clears all attached observers from this Observable.
    */
  def clear(): Unit = synchronized {
    observers = List.empty
  }

  /**
    * Cleans up all cross references in preparation for releasing for GC.
    */
  def dispose(): Unit = {
    clear()
  }
}