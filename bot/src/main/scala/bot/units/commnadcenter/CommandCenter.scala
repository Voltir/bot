package bot.units.commnadcenter

import bot._
import bot.common.exec.{FiniteFrames, DefiniteFrame, Exec}
import bot.common.exec.implicits._
import bot.common.notify.Notify
import bot.units.commnadcenter.CommandCenter.{IsIdle, EnqueueSCV}
import bwapi.{Unit => BWUnit, _}
import bot.coordinators.HackyCoordinator
import rx._

sealed trait UpcomingSupplyCost
case object NonePlanned extends UpcomingSupplyCost
case class Upcoming(at: DefiniteFrame, amt: Int) extends UpcomingSupplyCost

class CommandCenter(self: BWUnit, coord: HackyCoordinator)(implicit ctx: Ctx.Owner)  {

  val scvT = UnitType.Terran_SCV

  val state: Var[CommandCenter.State] = Var(IsIdle)

  val upcomingSupplyCost: Var[UpcomingSupplyCost] = Var(NonePlanned)

  def estTimeToSpend(supply: Int): FiniteFrames = {
    (scvT.buildTime()*(supply/scvT.supplyRequired())).frames
  }

  private def shouldTrain(): Boolean = !self.isTraining && self.canTrain()

  def request(req: CommandCenter.Request): Unit = req match {
    case EnqueueSCV if shouldTrain() =>

      self.train(scvT)

      state() = CommandCenter.IsActive

      upcomingSupplyCost() = Upcoming(
        coord.game.now + scvT.buildTime().frames,
        scvT.supplyRequired()
      )

      Exec.schedule((scvT.buildTime()+10).frames) {
        println("FINISHIEHJSHD?")
        println(self.isTraining)
        if(!self.isTraining) { state() = IsIdle }
      }

    case _ => println("COMMAND CENTER INVALID COMMAND!")
  }

}

object CommandCenter {
  sealed trait Request
  case object EnqueueSCV extends Request

  sealed trait State
  case object IsDead extends State
  case object IsActive extends State
  case object IsIdle extends State
}
