package it.unibo.pps1920.motoscala.controller

import java.util.UUID
import java.util.UUID.randomUUID

import akka.actor.ExtendedActorSystem
import it.unibo.pps1920.motoscala
import it.unibo.pps1920.motoscala.controller.managers.audio._
import it.unibo.pps1920.motoscala.controller.managers.file.DataManager
import it.unibo.pps1920.motoscala.controller.mediation.Mediator
import it.unibo.pps1920.motoscala.ecs.components.Shape.Circle
import it.unibo.pps1920.motoscala.engine.Engine
import it.unibo.pps1920.motoscala.model.Level.{Coordinate, LevelData}
import it.unibo.pps1920.motoscala.model.Scores.ScoresData
import it.unibo.pps1920.motoscala.model.Settings.SettingsData
import it.unibo.pps1920.motoscala.model.{Level, NetworkAddr}
import it.unibo.pps1920.motoscala.multiplayer.actors.{ClientActor, ServerActor}
import it.unibo.pps1920.motoscala.multiplayer.messages.ActorMessage._
import it.unibo.pps1920.motoscala.multiplayer.messages.MessageData.LobbyData
import it.unibo.pps1920.motoscala.view.events.ViewEvent._
import it.unibo.pps1920.motoscala.view.utilities.ViewConstants
import it.unibo.pps1920.motoscala.view.{JavafxEnums, ObserverUI}
import org.slf4j.LoggerFactory

import scala.collection.immutable.HashMap

trait Controller extends ActorController with SoundController with ObservableUI {
}

object Controller {
  def apply(): Controller = new ControllerImpl()

  import akka.actor.{ActorRef, ActorSystem}
  import com.typesafe.config.ConfigFactory
  private class ControllerImpl private[Controller]() extends Controller {
    import it.unibo.pps1920.motoscala.model.{MultiPlayerSetup, SinglePlayerSetup}
    import it.unibo.pps1920.motoscala.multiplayer.messages.DataType
    private val soundController: SoundController = null
    private val logger = LoggerFactory getLogger classOf[ControllerImpl]
    private val mediator = Mediator()
    private val myUuid: UUID = randomUUID()

    private val soundAgent: SoundAgent = SoundAgent()
    private val dataManager: DataManager = new DataManager()
    //campi che arrivano dal fu ConcreteActorController
    private val config = ConfigFactory.load("application")
    private val system = ActorSystem("MotoSystem", config)
    private var engine: Option[Engine] = None
    private var observers: Set[ObserverUI] = Set()
    this.soundAgent.start();
    this.dataManager.initAppDirectory()
    private var levels: List[LevelData] = List()
    private var actualSettings: SettingsData = loadSettings()
    private var clientActor: Option[ActorRef] = None
    private var serverActor: Option[ActorRef] = None
    private var matchSetupMp: Option[MultiPlayerSetup] = None
    private var matchSetupSp: Option[SinglePlayerSetup] = None
    private var status: Boolean = false

    override def attachUI(obs: ObserverUI*): Unit = observers = observers ++ obs
    override def detachUI(obs: ObserverUI*): Unit = observers = observers -- obs
    override def setupGame(level: Level): Unit = {
      logger info s"level selected: $level"
      engine = Option(motoscala.engine.GameEngine(mediator, myUuid, this))
      engine.get.init(levels.filter(data => data.index == level).head)
    }

    override def start(): Unit = engine.get.start()
    override def getMediator: Mediator = mediator
    override def loadAllLevels(): Unit = {
      levels = List(LevelData(0, Coordinate(ViewConstants.Canvas.CanvasWidth, ViewConstants.Canvas.CanvasHeight),

                              List(Level.Player(Coordinate(50, 50), Circle(25), Coordinate(0, 0), Coordinate(10, 10)),
                                   Level
                                     .RedPupa(Coordinate(90, 50), Circle(25), Coordinate(0, 0), Coordinate(20, 20)))))
      observers.foreach(o => o.notify(LevelDataEvent(levels)))
    }
    override def pause(): Unit = engine.get.pause()
    override def resume(): Unit = engine.get.resume()
    override def stop(): Unit = {
      engine.get.stop()
      engine = None
    }


    override def redirectSoundEvent(me: MediaEvent): Unit = this.soundAgent.enqueueEvent(me)

    override def loadStats(): Unit = observers
      .foreach(observer => observer.notify(ScoreDataEvent(this.dataManager.loadScore()
                                                            .getOrElse(ScoresData(HashMap("GINO" -> 100000, "GINO2" -> 100000))))))
    override def loadSetting(): Unit = observers
      .foreach(observer => observer.notify(SettingsDataEvent(this.actualSettings)))

    override def saveStats(newSettings: SettingsData): Unit = {
      this.actualSettings = newSettings
      this.dataManager.saveSettings(this.actualSettings)
    }
    override def setSelfReady(): Unit = {
      this.status = !this.status
      if (serverActor.isDefined) {
        this.matchSetupMp.get.setPlayerStatus(serverActor.get, this.status)
        this.serverActor.get
          .tell(LobbyDataActorMessage(LobbyData(readyPlayers = this.matchSetupMp.get.readyPlayers)), this.serverActor
            .get)
        observers
          .foreach(observer => observer
            .notify(LobbyDataEvent(LobbyData(readyPlayers = this.matchSetupMp.get.readyPlayers))))
      } else {
        this.clientActor.get ! ReadyActorMessage(this.status)
      }
    }

    override def kickSomeone(name: String): Unit = {
      this.serverActor.get ! KickActorMessage(this.matchSetupMp.get.removePlayer(name))
      observers
        .foreach(observer => observer
          .notify(LobbyDataEvent(LobbyData(readyPlayers = this.matchSetupMp.get.readyPlayers))))
    }
    /*Used by Client Actor*/
    override def gameStart(): Unit = ???
    override def gameEnd(): Unit = {

    }
    override def getLobbyData: DataType.LobbyData = LobbyData(Some(matchSetupMp.get.difficulty), Some(matchSetupMp.get
                                                                                                        .mode), matchSetupMp
                                                                .get.readyPlayers)
    override def tryJoinLobby(ip: String, port: String): Unit = {
      this.clientActor = Some(system.actorOf(ClientActor.props(this), "Client"))
      this.clientActor.get ! TryJoin(s"akka://MotoSystem@${ip}:${port}/user/Server*", this.actualSettings.name)
    }
    override def becomeHost(): Unit = {
      serverActor = Some(system.actorOf(ServerActor.props(this), "Server"))
      matchSetupMp = Some(MultiPlayerSetup(1, mode = true, 10))
      matchSetupMp.get.tryAddPlayer(serverActor.get, this.actualSettings.name)
      observers
        .foreach(observer => observer
          .notify(SetupLobbyEvent(NetworkAddr.getLocalIPAddress, system.asInstanceOf[ExtendedActorSystem].provider
            .getDefaultAddress.port.get.toString, this.actualSettings.name)))
    }
    override def joinResult(result: Boolean): Unit = {
      if (!result) {
        this.shutdownMultiplayer()
      }
      this.observers.foreach(obs => {
        obs.notify(JoinResultEvent(result))
      })
    }
    override def sendToLobbyStrategy[T](strategy: MultiPlayerSetup => T): T = {
      strategy.apply(this.matchSetupMp.get)
    }
    override def sendToViewStrategy(strategy: ObserverUI => Unit): Unit = {
      observers.foreach(o => strategy.apply(o))
    }
    override def gotKicked(): Unit = {
      this.observers.foreach(obs => {
        obs.notify(LeaveLobbyEvent())
        obs.notify(ShowDialogEvent("Sorry, i hate you", "You have been kicked", JavafxEnums.SHORT_DURATION, JavafxEnums
          .ERROR_NOTIFICATION))
        this.shutdownMultiplayer()
      })
    }
    override def shutdownMultiplayer(): Unit = {

      if (this.serverActor.isDefined) {
        this.system.stop(this.serverActor.get)
      } else if (this.clientActor.isDefined) {
        this.system.stop(this.clientActor.get)
      }
      this.serverActor = None
      this.clientActor = None
    }
    override def leaveLobby(): Unit = {
      if (this.serverActor.isDefined) {
        this.serverActor.get ! CloseLobbyActorMessage()
      } else if (this.clientActor.isDefined) {
        this.clientActor.get ! LeaveEvent(this.clientActor.get)
      }
    }
    private def loadSettings(): SettingsData = this.dataManager.loadSettings().getOrElse(SettingsData())
  }
}


