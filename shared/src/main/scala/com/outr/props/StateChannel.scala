package com.outr.props

trait StateChannel[T] extends Observable[T] {
  protected def state: T

  def attachAndFire(f: T => Unit): T => Unit = {
    attach(f)
    fire(get)
    f
  }

  def get: T = {
    StateChannel.contextFired(this)
    state
  }

  def value: T = get
}

object StateChannel {
  private val context = new ThreadLocal[Option[StateChannelContext]] {
    override def initialValue(): Option[StateChannelContext] = None
  }

  private[props] def contextualized[R](f: => R): R = {
    val oldValue = context.get()
    context.set(Some(new StateChannelContext))
    try {
      f
    } finally {
      context.set(oldValue)
    }
  }

  private[props] def contextFired[T](channel: StateChannel[T]): Unit = context.get().foreach { c =>
    channel match {
      case o: Observable[_] => c.observables += o
      case _ => // Not an observable
    }
  }

  private[props] def contextObservables(): Set[Observable[_]] = context.get().getOrElse(throw new RuntimeException("Not within a context!")).observables
}