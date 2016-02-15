package bot.common.exec

class Exec[T] private[exec](val when: GameFrame, run:  => T) {
  private [exec] lazy val value = run

  def map[B](f: T => B)(implicit ec: BwapiExecutionContext): Exec[B] = {
    when match {
      case df @ DefiniteFrame(_) =>
        val interval = df - ec.current
        ec.enqueue(interval,f(value))
      case af @ After(e) =>
        e.flatMap(_.map(_ => f(value)))
    }
  }

  def flatMap[B](f: T => Exec[B])(implicit ec: BwapiExecutionContext): Exec[B] = {
    when match {
      case df @ DefiniteFrame(_) =>
        val interval = df - ec.current
        val warp = ec.enqueue(interval,f(value))
        new Exec(After(warp),warp.value.value)
      case af @ After(e) =>
        e.flatMap(_.flatMap(_ => f(value)))
    }
  }
}

object Exec {
  implicit object ordering extends Ordering[Exec[_]] {
    override def compare(a: Exec[_], b: Exec[_]) = GameFrame.ordering.compare(a.when,b.when)
  }

  private [exec] def apply[T](at: GameFrame, action: => T): Exec[T] = new Exec(at, action)

  def schedule[T](interval: FiniteFrames)(action: => T)(implicit ec: BwapiExecutionContext): Exec[T] = {
    ec.enqueue(interval, action)
  }
}
