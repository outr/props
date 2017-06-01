package reactify

import java.util.concurrent.atomic.AtomicBoolean

trait StateChannel[T] extends State[T] with Channel[T] {
  def bind[V](that: StateChannel[V], setNow: BindSet = BindSet.LeftToRight)
             (implicit t2v: T => V, v2t: V => T): Binding[T, V] = {
    setNow match {
      case BindSet.LeftToRight => that := t2v(this)
      case BindSet.RightToLeft => this := v2t(that)
      case BindSet.None => // Nothing
    }
    val changing = new AtomicBoolean(false)
    val leftToRight = this.attach { t =>
      if (changing.compareAndSet(false, true)) {
        try {
          that := v2t(get)
        } finally {
          changing.set(false)
        }
      }
    }
    val rightToLeft = that.attach { t =>
      if (changing.compareAndSet(false, true)) {
        try {
          StateChannel.this := t2v(that.get)
        } finally {
          changing.set(false)
        }
      }
    }
    new Binding(this, that, leftToRight, rightToLeft)
  }
}