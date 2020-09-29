package it.unibo.pps1920.motoscala.controller.managers.audio

import java.nio.file.Path
import java.util.concurrent.ArrayBlockingQueue

import scalafx.scene.media.{AudioClip, MediaPlayer}

final class ConcreteSoundAgent extends SoundAgentLogic with SoundAgent {

  private var clips: Map[Clips, AudioClip] = Map()
  private var medias: Map[Media, MediaPlayer] = Map()
  private var blockingQueue: ArrayBlockingQueue[MediaEvent] = new ArrayBlockingQueue[MediaEvent](100)
  private var actualMusicPlayer: Option[MediaPlayer] = None
  private var actualClipPlayer: Option[AudioClip] = None
  private var volumeMusic: Double = 1.0
  private var volumeEffect: Double = 1.0

  override def playMusic(media: Media): Unit = {
    require(medias.isDefinedAt(media))
    val Musicplayer: MediaPlayer = medias(media)
    Musicplayer setVolume volumeMusic
    Musicplayer play()
  }

  override def playClip(clip: Clips): Unit = {
    require(clips.isDefinedAt(clip))
    val clipPlayer: AudioClip = clips(clip)
    clipPlayer setVolume volumeEffect
    clipPlayer play()
  }

  override def setVolumeMusic(value: Double): Unit = this.volumeMusic = _
  override def setVolumeEffect(value: Double): Unit = this.volumeEffect = _

  override def stopMusic(): Unit = this.actualClipPlayer.foreach(_.stop())

  override def restartMusic(): Unit = this.actualClipPlayer.foreach(_.stop())

  override def pauseMusic(): Unit = this.actualClipPlayer.foreach(_.stop())

  override def enqueueEvent(ev: MediaEvent): Unit = this.blockingQueue.add(ev)

  override def setClips(clips: Map[Media, Path]): Unit = {
    ???
  }
  override def setMediaPath(medias: Map[Media, Path]): Unit = {
    ???
  }

}
