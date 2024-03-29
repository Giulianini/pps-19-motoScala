package it.unibo.pps1920.motoscala.engine

/**
 * Possible gameloop statuses
 */
object GameStatus {

  sealed trait GameStatus

  case object Running extends GameStatus

  case object Paused extends GameStatus

  case object Stopped extends GameStatus

}
