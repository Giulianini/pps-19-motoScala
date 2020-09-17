package it.unibo.pps1920.motoscala.controller

import it.unibo.pps1920.motoscala.view.ObserverUI

trait UpdatableUI extends SoundController {
  type Level = Int
  def attachUI(obs: ObserverUI*): Unit
  def detachUI(obs: ObserverUI*): Unit
  def startGame(level: Level): Unit
}


