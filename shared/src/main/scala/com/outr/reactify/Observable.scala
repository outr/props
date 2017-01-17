package com.outr.reactify

trait Observable[T] {
  private var observers = Set.empty[T => Unit]

  lazy val distinct: Observable[T] = new DistinctObservable[T](this)

  def attach(f: T => Unit): T => Unit = synchronized {
    observers += f
    f
  }

  def detach(f: T => Unit): Unit = synchronized {
    observers -= f
  }

  def changes(listener: ChangeListener[T]): T => Unit = attach(ChangeListener.createFunction(listener, None))

  protected def fire(value: T): Unit = observers.foreach { obs =>
    obs(value)
  }
}