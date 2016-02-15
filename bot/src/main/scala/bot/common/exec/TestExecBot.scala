package bot.common.exec

import bwapi.{Unit => BWUnit, _}
import implicits._

class TestExecBot extends DefaultBWListener {

  private val mirror = new Mirror
  private var game: Game = null

  def run(): Unit = {
    mirror.getModule.setEventListener(this)
    mirror.startGame()
  }

  def dbgAssert(asserts: Boolean)(any: Any *): Unit = {
    if(!asserts) {
      game.printf(any.mkString("\n"))
      game.pauseGame()
    }
  }
  override def onStart(): Unit = {
    game = mirror.getGame

    val lol = Exec.schedule(50.frames) {
      dbgAssert(game.getFrameCount == 50)("Not Frame 50!")
      10
    }

    val wurt = lol.map { a =>
      dbgAssert(game.getFrameCount == 50)("Not Frame 50!")
      a * 10
    }

    wurt.map { b =>
      dbgAssert(game.getFrameCount == 50)("Not Frame 50!")
      "Yay"
    }

    val wurt2 = wurt.flatMap { total =>
      dbgAssert(game.getFrameCount == 50)("Not Frame 50!")
      Exec.schedule(50.frames) {
        dbgAssert(game.getFrameCount == 100)("Not Frame 100!")
        101
      }
    }

    wurt2.map { qqq =>
      dbgAssert(game.getFrameCount == 100)("Not Frame 100!")
      qqq*2
    }

    val wurt3 = wurt2.flatMap { zzz =>
      dbgAssert(game.getFrameCount == 100)("Not Frame 100!")
      Exec.schedule(25.frames) {
        dbgAssert(game.getFrameCount == 125)("Not Frame 125!")
        Exec.schedule(5.frames) { println("===== ALL DONE, EVERYTHIN GOOD!! ====")
          dbgAssert(game.getFrameCount == 130)("NOT FRAME 130???")
          game.leaveGame()
        }
      }
    }

    val omg = for {
      a <- Exec.schedule(10.frames) { println("** 10!"); 10 }
      b <- Exec.schedule(10.frames) { println("** 20!"); 20 }
      c <- Exec.schedule(10.frames) { println("** 30!"); 30 }
    } yield {
      dbgAssert(game.getFrameCount == 30)("Frame was not 30!")
      a + b + c
    }
  }

  override def onFrame(): Unit = {
    //Update The Scheduler
    RunningExecutionContext.setFrame(game.getFrameCount)

    if(game.getFrameCount == 15) {
      Exec.schedule(10.frames) {
        dbgAssert(game.getFrameCount == 25)("FRAME SHOULD BE 25 HERE!")
        println("ALLLLL GOOOOD!")
      }
    }
  }
}
