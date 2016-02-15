package bot

import bot.coordinators.HackyCoordinator
import bot.resources.ActiveMineral
import bot.units.scv.Scv
import rx._
import bwapi.{Unit => BWUnit, _}
import bwta.BWTA
import bwta.BaseLocation
import scala.collection.mutable.{Map => MutMap}
import scala.collection.JavaConversions._
import bot.common.exec.implicits._
import bot.common.exec.Exec
case class UnitId(id: Int) extends AnyVal

class TestBot1(implicit ctx: Ctx.Owner) extends DefaultBWListener {

  private val mirror = new Mirror

  private var game: Game = null

  private var self: Player = null

  private var hacky: HackyCoordinator = null

  def run(): Unit = {
    mirror.getModule.setEventListener(this)
    mirror.startGame()
  }

 // private var testMinerals: List[ActiveMineral] = List.empty

  override def onStart(): Unit = {
    game = mirror.getGame
    self = game.self()


    println("Analyze map...")
    BWTA.readMap()
    BWTA.analyze()

//    self.getUnits.find(_.getType == UnitType.Terran_Command_Center).foreach { base =>
//      game.neutral()
//        .getUnits
//        .filter(_.getType.isMineralField)
//        .sortBy(_.getDistance(base.getPosition)).take(8).reverse.foreach { nearest =>
//          testMinerals = new ActiveMineral(nearest) :: testMinerals
//        }
//    }

    hacky = new HackyCoordinator(self,game)

    hacky.onStart()
  }


//  //val scvs = MutMap.empty[UnitId, scv.Behavior]
//  val scvs = MutMap.empty[UnitId,scv.Behavior2]
//
//  val freeSupply: Var[Int] = Var(5)
//
//  val minerals: Var[Int] = Var(0)
//
//  var todoBetterBuilderCount = 0



  override def onFrame(): Unit = {
    //Update The Scheduler
    common.exec.RunningExecutionContext.setFrame(game.getFrameCount)
    common.notify.Notify.onFrame()

    hacky.onFrame()

    game.drawTextScreen(10, 10, "Frame: " + game.getFrameCount)

//    freeSupply() = self.supplyTotal() - self.supplyUsed()
//
//    game.neutral().getUnits.filter(_.getType.isMineralField).foreach { mineral =>
//      val r =  UnitType.Resource_Mineral_Field.dimensionUp()
//      game.drawCircleMap(mineral.getPosition,r,Color.Red)
//    }
//
//    self.getUnits.filter(_.isCompleted).foreach { dude =>
//
////      if(dude.getType == UnitType.Terran_Command_Center && self.minerals() >= 50 && freeSupply.now > 0) {
////        dude.train(UnitType.Terran_SCV)
////      }
//
//      if(dude.getType.isWorker && !scvs.contains(dude.uid)) {
//        val b = new Behavior2(dude)
//        scvs.put(dude.uid,b)
//        b.setIdle()
//        testMinerals.sortBy(_.numAssigned).headOption.foreach(_.assign(b))
//      }
////      //Dude is scv
////      if(dude.getType.isWorker && !scvs.contains(UnitId(dude.getID))) {
////        val b = new scv.Behavior(dude,self,game,freeSupply,minerals)
////        scvs.put(UnitId(dude.getID),b)
////        b.execute(scv.Behavior.GatherMinerals)
////      } else if (dude.getType.isWorker && dude.isIdle && !dude.isBeingConstructed) {
////        scvs.get(UnitId(dude.getID)).foreach { wat =>
////          if(wat.current.now == scv.Behavior.Idle)
////            wat.execute(scv.Behavior.GatherMinerals)
////          else {
////            wat.current.recalc()
////          }
////        }
////      }
////    }
//
////    if(freeSupply.now == 0) {
////      scvs.flatMap(d => d._2.offer.now match {
////        case build @ scv.Behavior.BuildDepot(_) => Option(d)
////        case _ => None
////      }).headOption.foreach { d =>
////        //todoBetterBuilderCount += 1
////        d._2.execute(d._2.offer.now)
////      }
//    }
//
//    //Any SCV Idle should be set to idle
//    scvs.filter(_._2.self.isIdle).foreach(_._2.setIdle())
//
//    minerals() = self.minerals()
  }
}
