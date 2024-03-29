package it.unibo.pps1920.motoscala.controller

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem}
import com.typesafe.config.ConfigFactory
import it.unibo.pps1920.motoscala
import it.unibo.pps1920.motoscala.controller.Controller.Constants.maxPlayers
import it.unibo.pps1920.motoscala.controller.MagicValues.AkkaValues._
import it.unibo.pps1920.motoscala.controller.MagicValues.Messages.{KickedText, KickedTitle}
import it.unibo.pps1920.motoscala.controller.managers.audio.MediaEvent.{SetVolumeEffect, SetVolumeMusic}
import it.unibo.pps1920.motoscala.controller.managers.audio.{MediaEvent, SoundAgent}
import it.unibo.pps1920.motoscala.controller.managers.file.DataManager
import it.unibo.pps1920.motoscala.controller.mediation.Mediator
import it.unibo.pps1920.motoscala.ecs.entities.BumperCarEntity
import it.unibo.pps1920.motoscala.engine.Engine
import it.unibo.pps1920.motoscala.model.Difficulties.difficultiesList
import it.unibo.pps1920.motoscala.model.Level.LevelData
import it.unibo.pps1920.motoscala.model.MatchSetup.MultiPlayerSetup
import it.unibo.pps1920.motoscala.model.Scores.ScoresData
import it.unibo.pps1920.motoscala.model.Settings.SettingsData
import it.unibo.pps1920.motoscala.model.{Level, NetworkAddr}
import it.unibo.pps1920.motoscala.multiplayer.actors.{ClientActor, ServerActor}
import it.unibo.pps1920.motoscala.multiplayer.messages.ActorMessage._
import it.unibo.pps1920.motoscala.multiplayer.messages.DataType
import it.unibo.pps1920.motoscala.multiplayer.messages.MessageData.LobbyData
import it.unibo.pps1920.motoscala.view.events.ViewEvent
import it.unibo.pps1920.motoscala.view.events.ViewEvent.{LevelSetupData, LevelSetupEvent, ScoreDataEvent, SettingsDataEvent, _}
import it.unibo.pps1920.motoscala.view.{ObserverUI, showSimplePopup}
import javafx.application.Platform
import org.slf4j.LoggerFactory

import scala.collection.immutable.HashMap

trait Controller extends ActorController with SoundController with EngineController with ObservableUI {
}
object Controller {

  def apply(): Controller = new ControllerImpl()
  private class ControllerImpl private[Controller](
    override val mediator: Mediator = Mediator()) extends Controller {

    private val logger = LoggerFactory getLogger classOf[ControllerImpl]
    private val dataManager: DataManager = new DataManager()
    private val soundAgent: SoundAgent = SoundAgent()
    override var score: Int = 0
    private var config: com.typesafe.config.Config = _ //ConfigFactory.load(SystemConfiguration)
    private var akkaSystem: Option[ActorSystem] = None //ActorSystem(SystemName, config)
    private var engine: Option[Engine] = None
    private var observers: Set[ObserverUI] = Set()
    private var levels: List[LevelData] = List()
    private var clientActor: Option[ActorRef] = None
    private var serverActor: Option[ActorRef] = None
    private var matchSetupMp: Option[MultiPlayerSetup] = None
    private var multiplayerStatus: Boolean = false
    private var actualSettings: SettingsData = loadSettings()

    soundAgent.start()
    dataManager.initAppDirectory()

    setAudioVolume(actualSettings.musicVolume, actualSettings.effectVolume)

    override def attachUI(obs: ObserverUI*): Unit = observers = observers ++ obs
    override def detachUI(obs: ObserverUI*): Unit = observers = observers -- obs
    override def start(): Unit = {
      score = 0
      engine.get.start()
    }
    override def pause(): Unit = engine.get.pause()
    override def resume(): Unit = engine.get.resume()
    override def stop(): Unit = {
      if (engine.isDefined) {
        engine.get.stop()
        engine = None
      }
    }
    override def redirectSoundEvent(me: MediaEvent): Unit = {
      if (serverActor.isDefined) {
        serverActor.get ! PlayMediaMessage(me)
      }
      soundAgent.enqueueEvent(me)
    }
    override def loadSetting(): Unit = notifyUI(SettingsDataEvent(actualSettings))

    override def lobbyInfoChanged(
      level: Option[Int] = None, difficult: Option[Int] = None,
      isStatusChanged: Boolean = false): Unit = {
      if (isStatusChanged) multiplayerStatus = !multiplayerStatus

      if (serverActor.isDefined && matchSetupMp.isDefined) {
        difficult.foreach(matchSetupMp.get.difficulty = _)
        level.foreach(matchSetupMp.get.level = _)

        matchSetupMp.get.setPlayerStatus(serverActor.get, multiplayerStatus)
        serverActor.get.tell(
          LobbyDataActorMessage(
            LobbyData(
              difficulty = Some(matchSetupMp.get.difficulty), level = Some(matchSetupMp.get.level), readyPlayers = this
                .matchSetupMp.get.getPlayerStatus)), serverActor.get)
        notifyUI(LobbyDataEvent(LobbyData(readyPlayers = matchSetupMp.get.getPlayerStatus)))
      } else
        clientActor.get ! ReadyActorMessage(multiplayerStatus)
    }
    override def kickSomeone(name: String): Unit = {
      serverActor.get ! KickActorMessage(matchSetupMp.get.removePlayer(name))
      notifyUI(LobbyDataEvent(LobbyData(readyPlayers = matchSetupMp.get.getPlayerStatus)))
    }
    /*Used by Client Actor*/
    override def gameStart(): Unit = notifyUI(LoadLevelEvent())
    override def getLobbyData: DataType.LobbyData =
      LobbyData(Some(matchSetupMp.get.difficulty),
                Some(matchSetupMp.get.level), matchSetupMp.get.getPlayerStatus)
    override def tryJoinLobby(ip: String, port: String): Unit = {
      shutdownMultiplayer()
      val config = ConfigFactory.load(SystemConfiguration)
      akkaSystem = Some(ActorSystem(SystemName, config))
      clientActor = Some(akkaSystem.get.actorOf(ClientActor.props(this), ClientActorName))
      clientActor.get ! TryJoin(s"$ActorSelectionProtocol$ip:$port$ActorSelectionPath", actualSettings.name)
    }
    override def becomeHost(): Unit = {
      shutdownMultiplayer()
      loadAllLevels()
      config = ConfigFactory
        .parseString(s"""${AkkaCanonicalName}=${NetworkAddr.getLocalIPAddress}""")
        .withFallback(ConfigFactory.load(SystemConfiguration))
      akkaSystem = Some(ActorSystem(SystemName, config))

      serverActor = Some(akkaSystem.get.actorOf(ServerActor.props(this), ServerActorName))
      matchSetupMp = Some(MultiPlayerSetup(numPlayers = maxPlayers))
      matchSetupMp.get.tryAddPlayer(serverActor.get, actualSettings.name)
      notifyUI(SetupLobbyEvent(NetworkAddr.getLocalIPAddress, akkaSystem.get.asInstanceOf[ExtendedActorSystem].provider
        .getDefaultAddress.port.get.toString, actualSettings.name, levels.map(_.index), difficultiesList
                                 .map(_.number)))
    }
    override def loadAllLevels(): Unit = {
      levels = dataManager.loadLvl()
      notifyUI(LevelDataEvent(levels))
    }
    override def shutdownMultiplayer(): Unit = {

      multiplayerStatus = false
      if (serverActor.isDefined) akkaSystem.get.stop(serverActor.get)
      if (clientActor.isDefined) akkaSystem.get.stop(clientActor.get)
      if (akkaSystem.isDefined) akkaSystem.get.terminate()
      matchSetupMp = None
      serverActor = None
      clientActor = None
      akkaSystem = None
    }
    override def joinResult(result: Boolean): Unit = {
      if (!result) shutdownMultiplayer()
      notifyUI(JoinResultEvent(result))
    }
    override def sendToLobbyStrategy[T](strategy: MultiPlayerSetup => T): T = strategy.apply(matchSetupMp.get)
    override def sendToViewStrategy(strategy: ObserverUI => Unit): Unit = observers
      .foreach(obs => Platform.runLater(() => strategy(obs)))
    override def gotKicked(): Unit = {
      notifyUI(LeaveLobbyEvent())
      showSimplePopup(KickedTitle, KickedText)
      shutdownMultiplayer()
    }
    override def leaveLobby(): Unit = {
      if (serverActor.isDefined)
        serverActor.get ! CloseLobbyActorMessage()
      else if (clientActor.isDefined)
        clientActor.get ! LeaveEvent(clientActor.get)
    }
    override def startMultiplayerGame(): Unit = {
      serverActor.get ! GameStartActorMessage()
      setupGame(matchSetupMp.get.level)
    }
    override def setupGame(level: Level): Unit = {
      logger info s"level selected: $level"

      //Get selected level
      val lvl = levels.filter(_.index == level).head

      var playerNum = 1
      //Get the correct number of bumbper car
      val players = if (serverActor.isDefined) {
        playerNum = matchSetupMp.get.numReadyPlayers()
        (0 to playerNum).map(_ => BumperCarEntity(UUID.randomUUID())).toList
      } else
        List(BumperCarEntity(UUID.randomUUID()))
      //Get the be removed from level
      val entitiesToRemove = lvl.entities.filter(_.isInstanceOf[Level.Player]).slice(playerNum, maxPlayers)
      lvl.entities = lvl.entities.filterNot(entitiesToRemove.contains(_))

      //Create one instance of engine
      engine = Option(motoscala.engine.GameEngine(this, players, if (matchSetupMp.isDefined) matchSetupMp.get
        .difficulty else actualSettings.diff))

      //Start engine
      engine.get.init(lvl)


      var setups: List[LevelSetupData] = List()
      //Prepare all data for level to sent
      players.slice(1, players.size)
        .foreach(player => setups = setups :+ LevelSetupData(lvl, isSinglePlayer = false, isHosting = false, player))
      //Send to sel
      notifyUI(LevelSetupEvent(LevelSetupData(lvl, isSinglePlayer = serverActor
        .isEmpty, isHosting = serverActor.isDefined, players.head)))

      //Send to clients if present
      if (serverActor.isDefined) serverActor.get ! SetupsForClientsMessage(setups)
    }
    override def saveStats(): Unit = {
      if (serverActor.isEmpty && clientActor.isEmpty) {
        var loadedStats = dataManager.loadScore().getOrElse(ScoresData(HashMap())).scoreTable
        if (loadedStats.getOrElse(actualSettings.name, 0) < score)
          loadedStats = loadedStats.updated(actualSettings.name, score)
        dataManager.saveScore(ScoresData(loadedStats))
      }
    }
    override def loadStats(): Unit = notifyUI(ScoreDataEvent(dataManager.loadScore()
                                                               .getOrElse(ScoresData(HashMap()))))
    private def notifyUI(ev: ViewEvent): Unit = {
      observers.foreach(obs => {
        Platform.runLater(() => obs.notify(ev))
      })
    }
    override def saveSettings(newSettings: SettingsData): Unit = {
      actualSettings = newSettings
      dataManager.saveSettings(actualSettings)
      setAudioVolume(actualSettings.musicVolume, actualSettings.effectVolume)
    }
    private def setAudioVolume(musicVolume: Double, effect: Double): Unit = {
      soundAgent.enqueueEvent(SetVolumeMusic(musicVolume))
      soundAgent.enqueueEvent(SetVolumeEffect(effect))
    }
    private def loadSettings(): SettingsData = dataManager.loadSettings().getOrElse(SettingsData())
  }
  object Constants {
    final val maxPlayers = 4
  }
}

private object MagicValues {
  object AkkaValues {

    final val SystemConfiguration = "application"
    final val SystemName = "MotoSystem"
    final val ClientActorName = "Client"
    final val ServerActorName = "Server"
    final val ActorSelectionPath = "/user/Server*"
    final val ActorSelectionProtocol = s"akka://$SystemName@"
    final val AkkaDivider = ":"
    final val AkkaCanonicalName = "akka.remote.artery.canonical.hostname"

  }

  object Messages {

    final val KickedTitle = "Sorry, i hate you"
    final val KickedText = "You have been kicked"
  }
}

