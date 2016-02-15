package bot.common.exec

case class FiniteFrames(num: Int) extends AnyVal {
  def +(in: FiniteFrames): FiniteFrames = FiniteFrames(num + in.num)
  def <=(in: FiniteFrames): Boolean = FiniteFrames.ordering.lteq(this,in)
}

object FiniteFrames {
  implicit val ordering: Ordering[FiniteFrames] = new Ordering[FiniteFrames] {
    override def compare(a: FiniteFrames, b: FiniteFrames) = a.num.compare(b.num)
  }
}

sealed trait GameFrame
case class DefiniteFrame(f: Int) extends GameFrame {
  def +(in: FiniteFrames): DefiniteFrame = DefiniteFrame(f + in.num)
  def -(in: DefiniteFrame): FiniteFrames = FiniteFrames(f - in.f)
}
case class After[T](next: Exec[Exec[T]]) extends GameFrame

object GameFrame {
  implicit object  ordering extends Ordering[GameFrame] {
    override def compare(a: GameFrame, b: GameFrame) = (a,b) match {
      case (DefiniteFrame(x),DefiniteFrame(y)) => Ordering[Int].reverse.compare(x,y)
      case _ =>
        println("THIS SHALL NOT HAPPEN!!!!!!!!!!!!!!!!!!")
        ???
    }
  }

}

trait BwapiExecutionContext {
  def enqueue[T](interval: FiniteFrames, action: => T): Exec[T]
  def current: DefiniteFrame
}

object RunningExecutionContext  extends BwapiExecutionContext  {

  import scala.collection.mutable

  private val queue: mutable.PriorityQueue[Exec[_]] = mutable.PriorityQueue.empty[Exec[_]]

  private var now: DefiniteFrame = DefiniteFrame(0)

  override def current = now

  def setFrame(count: Int): Unit = {
    now = DefiniteFrame(count)
    executeNow()
  }

  override def enqueue[T](interval: FiniteFrames, action: => T): Exec[T] = {
    val at = now + interval
    val exec = Exec(at,action)
    queue += exec
    exec
  }

  private def executeNow(): Unit = {
    queue.headOption.filter(_.when == now).foreach { head =>
      queue.dequeue()
      head.value
      executeNow()
    }
  }
}