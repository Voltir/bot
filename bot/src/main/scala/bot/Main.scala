package bot

import bot.common.exec.TestExecBot

object Main {
  import rx.Ctx.Owner.Unsafe._

  def main(args: Array[String]) = new TestBot1().run()
  //def main(args: Array[String]) = new TestExecBot().run()

}