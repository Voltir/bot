package bot.common.notify

import bot.common.exec.Exec
import bot.common.exec.implicits._
import bwapi.{Unit => BWUnit, _}

object Notify {

  private var checks: List[(() => Boolean, () => Exec[Unit])] = List.empty

  def when(chk:() => Boolean)(action: => Unit) = {
    checks = (chk, () => Exec.schedule(1.frames)(action)) :: checks
  }

  def onFrame(): Unit = {
    val (execute,keep) = checks.partition(_._1())
    execute.foreach(_._2())
    checks = keep
  }
}
