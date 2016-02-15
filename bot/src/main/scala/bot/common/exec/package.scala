package bot.common

package object exec {

  object implicits {
    implicit val ec = exec.RunningExecutionContext

    implicit val wwwww = FiniteFrames.ordering

    implicit class IntExt(val i: Int) extends AnyVal {
      def frames: FiniteFrames = FiniteFrames(i)
    }
  }

}
