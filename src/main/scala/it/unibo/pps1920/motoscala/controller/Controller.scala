package it.unibo.pps1920.motoscala.controller

import java.util.UUID
import java.util.UUID.randomUUID

import it.unibo.pps1920.motoscala
import it.unibo.pps1920.motoscala.controller.managers.audio.{Clips, Media, SoundAgent}
import it.unibo.pps1920.motoscala.controller.mediation.Mediator
import it.unibo.pps1920.motoscala.ecs.components.Shape.Circle
import it.unibo.pps1920.motoscala.engine.Engine
import it.unibo.pps1920.motoscala.model.Level
import it.unibo.pps1920.motoscala.model.Level.{Coordinate, LevelData}
import it.unibo.pps1920.motoscala.view.ObserverUI
import it.unibo.pps1920.motoscala.view.events.ViewEvent.LevelDataEvent
import it.unibo.pps1920.motoscala.view.utilities.ViewConstants
import org.slf4j.LoggerFactory

trait Controller extends ActorController with SoundController with ObservableUI {
}

object Controller {
  def apply(): Controller = new ControllerImpl()
  private class ControllerImpl private[Controller]() extends Controller {
    private val logger = LoggerFactory getLogger classOf[ControllerImpl]
    private val mediator = Mediator()
    private val myUuid: UUID = randomUUID()
    private val soundAgent: SoundAgent = SoundAgent()
    private var engine: Option[Engine] = None
    private var observers: Set[ObserverUI] = Set()
    private var levels: List[LevelData] = List()
    override def attachUI(obs: ObserverUI*): Unit = observers = observers ++ obs
    override def detachUI(obs: ObserverUI*): Unit = observers = observers -- obs
    override def setupGame(level: Level): Unit = {
      logger info s"level selected: $level"
      engine = Option(motoscala.engine.GameEngine(mediator, myUuid))
      engine.get.init(levels.filter(data => data.index == level).head)
    }
    override def start(): Unit = engine.get.start()
    override def getMediator: Mediator = mediator
    override def loadAllLevels(): Unit = {
      levels = List(LevelData(0, Coordinate(ViewConstants.Canvas.CanvasWidth, ViewConstants.Canvas.CanvasHeight),
                              List(Level.Player(Coordinate(50, 50), Circle(25), Coordinate(0, 0), 10),
                                   Level.Enemy1(Coordinate(50, 50), Circle(25), Coordinate(0, 0), 10))))
      observers.foreach(o => o.notify(LevelDataEvent(levels)))
    }
    override def pause(): Unit = engine.get.pause()
    override def resume(): Unit = engine.get.resume()
    override def stop(): Unit = {
      engine.get.stop()
      engine = None
    }


    override def playSoundTrack(med: Media): Unit = this.soundAgent.enqueueEvent(ev => this.soundAgent.playMusic(med))
    override def playEffect(clip: Clips): Unit = this.soundAgent.enqueueEvent(ev => this.soundAgent.playClip(clip))
    override def setVolumeSoundTrack(value: Double): Unit = this.soundAgent.setVolumeMusic(_)
    override def setVolumeEffect(value: Double): Unit = this.soundAgent.setVolumeEffect(_)
    override def stopMusic(): Unit = this.soundAgent.stopMusic()
    override def restartMusic(): Unit = this.soundAgent.restartMusic()
    override def pauseMusic(): Unit = this.soundAgent.pauseMusic()
  }
}


