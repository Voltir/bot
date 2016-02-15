package bot.coordinators

import bot._
import bot.common.exec.implicits._
import bot.common.exec.{Exec, FiniteFrames}
import bot.common.notify.Notify
import bot.resources.ActiveMineral
import bot.units.scv.Scv
import bot.units.commnadcenter.{Upcoming, NonePlanned, CommandCenter}
import bwapi.{Unit => BWUnit, _}
import scala.collection.mutable.{Map => MutMap}
import scala.collection.JavaConversions._
import rx._

class MineralCoordinator(parent: HackyCoordinator)(implicit ctx: Ctx.Owner) {

  private val minerals: Var[List[ActiveMineral]] = Var(List.empty)

  private val assignments: Var[MutMap[UnitId,ActiveMineral]] = Var(MutMap.empty)

  private val MAX_GATHERER_DEMAND = 2

  val scvDemand: Rx[Int] = Rx {
    (minerals().size * MAX_GATHERER_DEMAND) - minerals().map(_.assigned().size).sum
  }

  def onStart(): Unit = {
    parent.self.getUnits.find(_.getType == UnitType.Terran_Command_Center).foreach { base =>
      parent.game.neutral()
        .getUnits
        .filter(_.getType.isMineralField)
        .sortBy(_.getDistance(base.getPosition)).take(8).reverse.foreach { nearest =>
          minerals() = new ActiveMineral(nearest,this) :: minerals.now
      }
    }
  }

  def assign(scv: Scv): Unit = {
    minerals.now.sortBy(_.numAssigned).headOption.foreach { patch =>
      assignments.now.put(scv.self.uid,patch)
      patch.assign(scv)
      assignments.propagate()
    }
  }

  def unassign(thenDo: Scv => Unit) = {
    minerals.now.headOption.foreach { eh =>
      eh.assigned.now.reverse.headOption.foreach { scv =>
        eh.unassign(scv.self.uid)
        thenDo(scv)
      }
    }
  }
}

class HackyCoordinator(val self: Player, val game: Game)(implicit ctx: Ctx.Owner) {

  //todo rename that..
  val scvs: MutMap[UnitId,bot.units.scv.Scv] = MutMap.empty

  val miner = new MineralCoordinator(this)

  var cc: CommandCenter = null

  val freeSupply: Var[Int] = Var(0)
  val minerals: Var[Int] = Var(0)

  //First attempt at estimating time until supply block
  lazy val estTimeToSupplyBlock: Rx[FiniteFrames] = Rx {
    val free = freeSupply()
    val upcoming = cc.upcomingSupplyCost()
    val (remaining,used) = upcoming match {
      case NonePlanned => (Option.empty[FiniteFrames],0)
      case Upcoming(at,amt) => (Option(at - game.now),amt)
    }
    val unused = free - used

    val result = remaining.map(_ + cc.estTimeToSpend(unused)).getOrElse(cc.estTimeToSpend(unused))
    println("EST TIME TO SUPPLY BLOCK IS: " + result + s" (Frame: ${game.now + result}) ")
    println("Free is: " + free)
    println("Used is: " + used)
    println("Unused is: " + unused)
    println("Time to spend unused: " + cc.estTimeToSpend(unused))
    println("Upcoming: " + upcoming)
    println("Remainng: " + remaining)
    println("Used: " + used)
    println("\n")
    result
  }


  lazy val handleScvDemand = Rx {
    println("DEMAND IS: " + miner.scvDemand())
    if(cc.state() == CommandCenter.IsIdle && miner.scvDemand() > 0) {
      Notify.when(() => self.minerals() >= 50) {
        cc.request(CommandCenter.EnqueueSCV)
      }
    }
  }





  //Hacking in simple building logic for the time being
  def todoBetter(builder: BWUnit, target: UnitType, around: TilePosition): Option[TilePosition] = {
    val maxDist = 3

    val possible = for {
      x <- (around.getX - maxDist) to (around.getX + maxDist)
      y <- (around.getY - maxDist) to (around.getY + maxDist)
      tile = new TilePosition(x,y)
      if game.canBuildHere(tile,target)
      inWay = game.getAllUnits.filter(_.getID != builder.getID).forall(u =>
        Math.abs(u.getTilePosition.getX-x) < 4 && Math.abs(u.getTargetPosition.getY-y) < 4
      )
      if !inWay
    } yield { tile }

    possible.headOption
  }

  lazy val buildADepot = Rx {
    minerals()
    if((estTimeToSupplyBlock()+25.frames) <= UnitType.Terran_Supply_Depot.buildTime().frames) {
      println("======= ITS TIME TO BUILD A DEEPOT ==========")
      miner.unassign { scv =>
        todoBetter(scv.self,UnitType.Terran_Supply_Depot,self.getStartLocation).foreach { tile =>
          scv.self.move(tile.toPosition)
          Notify.when(() => self.minerals() >= 100) {
            scv.self.build(UnitType.Terran_Supply_Depot, tile)
            Notify.when(() => scv.self.isIdle) {
              miner.assign(scv)
            }
          }
        }
      }
    }
  }

  lazy val lolwat = Rx {
    if(minerals() >= 150 && self.supplyUsed() > 10) {
      miner.unassign { scv =>
        todoBetter(scv.self, UnitType.Terran_Barracks, self.getStartLocation).foreach { tile =>
          Notify.when(() => self.minerals() >= 150) {
            scv.self.build(UnitType.Terran_Barracks, tile)
            Notify.when(() => scv.self.isIdle) {
              miner.assign(scv)
            }
          }
        }
      }
    }
  }

  def release(scv: Scv) = {
    miner.assign(scv)
  }

  def onStart(): Unit = {
    miner.onStart()
    self.getUnits.collect {
      case base if base.getType == UnitType.Terran_Command_Center =>
        cc = new CommandCenter(base,this)

      case worker if worker.getType.isWorker =>
        val scv = new Scv(worker)
        scvs.put(worker.uid, scv)
        miner.assign(scv)
    }
    estTimeToSupplyBlock
    handleScvDemand
    //buildADepot
    //lolwat
  }

  def onFrame(): Unit = {
    freeSupply() = self.supplyTotal() - self.supplyUsed()
    minerals() = self.minerals()

    self.getUnits.filter(_.isCompleted).collect {
      //New Worker Appeared!
      case worker if worker.getType.isWorker && !scvs.contains(worker.uid) =>
        val scv = new Scv(worker)
        scvs.put(worker.uid,scv)
        miner.assign(scv)

      case barracks if barracks.getType == UnitType.Terran_Barracks =>
        if(!barracks.isTraining && freeSupply.now > 0 && minerals.now >= 75) {
          barracks.train(UnitType.Terran_Marine)
        }

      case wwwr if wwwr.getType == UnitType.Terran_Command_Center =>
        wwwr.drawBoxBounds(game)

      case building if building.getType == UnitType.Buildings =>
        println("Building?????")
        building.drawBoxBounds(game)
    }

    //Any SCV Idle should be set to idle
    scvs.filter(_._2.self.isIdle).foreach(_._2.setIdle())
  }
}
