package it.unibo.pps1920.motoscala.engine

import it.unibo.pps1920.motoscala.engine.GameStatus._
import org.slf4j.LoggerFactory

trait Engine extends UpdatableEngine {
  def init(): Unit
  def start(): Unit
  def pause(): Unit
  def resume(): Unit
  def stop(): Unit
}


object GameEngine {
  def apply(): Engine = new GameEngineImpl()

  private class GameEngineImpl extends Engine {
    private val logger = LoggerFactory getLogger classOf[Engine]
    private var gameLoop: Option[GameCycle] = None

    override def tick(): Unit = {Thread.sleep(16) }

    override def init(): Unit = {
      gameLoop = Some(GameLoop(60, this))
    }
    override def start(): Unit = {
      gameLoop match {
        case Some(loop) =>
          loop.status match {
            case Stopped => loop.start()
            case _ => logger error "GameLoop already started"
          }
        case None => logger error "Loop not initialized"
      }

    }
    override def pause(): Unit = gameLoop match {
      case Some(loop) => loop.pause()
      case None => logger error "Loop not initialized"
    }
    override def resume(): Unit = gameLoop match {
      case Some(loop) => loop.unPause()
      case None => logger error "Loop not initialized"
    }
    override def stop(): Unit = gameLoop match {
      case Some(loop) => loop.halt()
      case None => logger error "Loop not initialized"
    }
  }

}

