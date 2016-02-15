import bot.common.exec.FiniteFrames

package object bot {

  implicit class UnitExt(val u: bwapi.Unit) extends AnyVal {
    def uid: UnitId = UnitId(u.getID)
    //def say(txt: String) = println(s""

    def drawBoxBounds(game: bwapi.Game) = {
      val center = u.getPosition
      val tl = new bwapi.Position(u.getLeft,u.getTop)
      val br = new bwapi.Position(u.getRight,u.getBottom)
      game.drawBoxMap(tl,br,bwapi.Color.Green)
      game.drawTextMap(u.getPosition,s"Center: ${u.getPosition.getX},${u.getPosition.getY})")
    }
  }

  implicit class GameExt(val g: bwapi.Game) extends AnyVal {
    def now: common.exec.DefiniteFrame = common.exec.DefiniteFrame(g.getFrameCount)
  }

}
