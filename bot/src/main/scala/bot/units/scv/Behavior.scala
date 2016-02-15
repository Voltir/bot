package bot.units.scv

import bwapi.{Unit => BWUnit, _}
import rx._
import bot._


object Scv {
  sealed trait ScvRequest
  case class MoveTo(target: Position) extends ScvRequest
  case class Gather(target: BWUnit) extends ScvRequest

  sealed trait ScvState
  case object IsInactive extends ScvState
  case object IsIdle extends ScvState
  case object IsActive extends ScvState
}

class Scv(val self: BWUnit) {
  import Scv._

  val state: Var[ScvState] = Var(IsInactive)

  def setIdle(): Unit = {
    state() = IsIdle
  }

  def request(req: ScvRequest): Unit = req match {
    case MoveTo(target) if state.now == IsIdle =>
      state() = IsActive
      self.move(target)

    case Gather(target) if state.now == IsIdle =>
      state() = IsActive
      self.gather(target)

    case _ => //println("Ignored..")
  }

  def say(txt: String) = println(s"Scv (${self.uid}): $txt")
}

//object Behavior {
//  sealed trait ScvBehavior
//  case object Idle extends ScvBehavior
//  case object GatherMinerals extends ScvBehavior
//  case class BuildDepot(target: TilePosition) extends ScvBehavior
//}

//class Behavior(self: bwapi.Unit, player: Player, game: Game, freeSupply: Var[Int], minerals: Var[Int]) {
//  import Behavior._
//
//  val offer: Var[ScvBehavior] = Var(Idle)
//
//  val current: Var[ScvBehavior] = Var(Idle)
//
//  val behaveOn = Rx.unsafe {
//    current()
//  }
//
//  val wat = current.foreach { c =>
//    if(c != GatherMinerals) println(s"Scv: ${self.getID} -- " + c)
//  }
//
//  val offerOn = Rx.unsafe {
//    (minerals(),freeSupply())
//  }
//
//  def execute(behavior: ScvBehavior): Unit = {
//    current() = behavior
//  }
//
//  private def goGatherMinerals(): Unit = {
//    game.neutral().getUnits.filter(_.getType.isMineralField).sortBy(_.getDistance(self.getPosition)).headOption.map { a =>
//      self.gather(a,false)
//    }
//  }
//
//  def todoBetter(builder: BWUnit, target: UnitType, around: TilePosition): Option[TilePosition] = {
//    val maxDist = 3
//
//    val possible = for {
//      x <- (around.getX - maxDist) to (around.getX + maxDist)
//      y <- (around.getY - maxDist) to (around.getY + maxDist)
//      tile = new TilePosition(x,y)
//      if game.canBuildHere(tile,target)
//      inWay = game.getAllUnits.filter(_.getID != builder.getID).forall(u =>
//        Math.abs(u.getTilePosition.getX-x) < 4 && Math.abs(u.getTargetPosition.getY-y) < 4
//      )
//      if !inWay
//    } yield { tile }
//
//    possible.headOption
//  }
//
//  val offerObs = offerOn.triggerLater {
//    if(freeSupply.now <= 0 && minerals.now >= 100) {
//      //println("DO THIS!!!!!!!!!!!!!!!!: " + minerals.now + " -- " + freeSupply.now)
//      val buildTile = todoBetter(self,UnitType.Terran_Supply_Depot,player.getStartLocation)
//      //println("=================== " + buildTile)
//      buildTile.foreach { t =>
//        //println("DO OFFER!")
//        offer() = BuildDepot(t)
//      }
//    }
//  }
//
//  val currentObs = current.foreach {
//    case Idle => self.stop()
//    case GatherMinerals => goGatherMinerals()
//    case BuildDepot(at: TilePosition) => self.build(UnitType.Terran_Supply_Depot,at)
//  }
//}
