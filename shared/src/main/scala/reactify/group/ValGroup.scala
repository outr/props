package reactify.group

import reactify.{State, Val}

case class ValGroup[T](name: Option[String], items: List[Val[T]]) extends Val[T] with Group[T, Val[T]] {
  override def state: State[T] = ???
}