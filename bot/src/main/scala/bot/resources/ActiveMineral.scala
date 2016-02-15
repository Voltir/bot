package bot.resources

import bot._
import bot.common.exec.Exec
import bot.common.exec.implicits._
import bot.common.notify.Notify
import bot.coordinators.MineralCoordinator
import bwapi.{Unit => BWUnit, _}
import rx._

class ActiveMineral(self: BWUnit, parent: MineralCoordinator)(implicit ctx: Ctx.Owner) {
  import bot.units.scv.Scv._


  val assigned: Var[List[bot.units.scv.Scv]] = Var(List.empty)

  private val currentlyGathering: Var[Option[bot.units.scv.Scv]] = Var(None)

  private def pleaseGather(scv: bot.units.scv.Scv): Unit = {
    scv.request(Gather(self))
    Notify.when(() => self.isBeingGathered) {
      currentlyGathering() = Some(scv)
    }
  }

  def numAssigned: Int = assigned.now.size

  def assign(gatherer: bot.units.scv.Scv) = {
    if(assigned.now.isEmpty) pleaseGather(gatherer)
    else gatherer.request(MoveTo(self.getPosition))
    assigned() = gatherer :: assigned.now
  }

  def unassign(uid: UnitId) = {
    assigned() = assigned.now.filter(_.self.uid != uid)
  }

  private val behave = assigned.map { mine =>
    mine.foreach(_.state())
    if(currentlyGathering.now.isEmpty) {
      mine.headOption.filter(_.state() == IsIdle).foreach(pleaseGather)
    }
  }

  val wat = currentlyGathering.foreach { _.foreach { gatherer =>
    //SCV Now Actively Gathering (animation started)
    Notify.when(() => gatherer.self.isCarryingMinerals) {
      currentlyGathering() = None
      assigned() = assigned.now.filter(_.self.getID != gatherer.self.getID) ::: List(gatherer)
      Notify.when(() => !gatherer.self.isCarryingMinerals) {
        gatherer.self.stop()
        Exec.schedule(1.frames)(gatherer.request(MoveTo(self.getPosition)))
      }
    }
  }}
}
