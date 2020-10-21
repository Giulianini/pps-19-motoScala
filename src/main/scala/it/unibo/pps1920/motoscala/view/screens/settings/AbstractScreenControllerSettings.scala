package it.unibo.pps1920.motoscala.view.screens.settings

import java.lang
import java.util.function.UnaryOperator

import it.unibo.pps1920.motoscala.controller.ObservableUI
import it.unibo.pps1920.motoscala.model.Difficulties.{EASY, HARD, MEDIUM}
import it.unibo.pps1920.motoscala.model.Settings.SettingsData
import it.unibo.pps1920.motoscala.view.ViewFacade
import it.unibo.pps1920.motoscala.view.screens.ScreenController
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.{Slider, TextField, TextFormatter}
import javafx.scene.layout.{AnchorPane, BorderPane}
import javafx.util.StringConverter

/** Abstract ScreenController dedicated to show settings menu.
 *
 * @param viewFacade the view facade
 * @param controller the controller
 */
abstract class AbstractScreenControllerSettings(
  protected override val viewFacade: ViewFacade,
  protected override val controller: ObservableUI) extends ScreenController(viewFacade, controller) {
  private val NameMazLength: Int = 6
  private val DefaultName: String = "Player"
  @FXML protected var root: BorderPane = _
  @FXML protected var mainAnchorPane: AnchorPane = _
  @FXML protected var effectSlider: Slider = _
  @FXML protected var diffSlider: Slider = _
  @FXML protected var musicSlider: Slider = _
  @FXML protected var textPlayerName: TextField = _
  @FXML override def initialize(): Unit = {
    assertNodeInjected()
    extendButtonBackBehaviour()
    initBackButton()
    initSlider()
    initTextField()
  }

  private def assertNodeInjected(): Unit = {
    assert(root != null, "fx:id=\"root\" was not injected: check your FXML file 'Settings.fxml'.")
    assert(mainAnchorPane != null, "fx:id=\"mainAnchorPane\" was not injected: check your FXML file 'Settings.fxml'.")
    assert(diffSlider != null, "fx:id=\"diffSlider\" was not injected: check your FXML file 'Settings.fxml'.")
    assert(musicSlider != null, "fx:id=\"volumeSlider\" was not injected: check your FXML file 'Settings.fxml'.")
    assert(effectSlider != null, "fx:id=\"effectSlider\" was not injected: check your FXML file 'Settings.fxml'.")
    assert(textPlayerName != null, "fx:id=\"textPlayerName\" was not injected: check your FXML file 'Settings.fxml'.")
  }
  private def initSlider(): Unit = {
    musicSlider.setOnMouseReleased(_ => sendStats())
    effectSlider.setOnMouseReleased(_ => sendStats())

    diffSlider.setLabelFormatter(new StringConverter[lang.Double]() {
      override def toString(`object`: lang.Double): String = `object` match {
        case n if n <= 1.0d => EASY.name
        case n if n <= 2.0d => MEDIUM.name
        case n if n <= 3.0d => HARD.name
      }
      override def fromString(string: String): lang.Double = string match {
        case str if str == EASY.name => 1.0d
        case str if str == MEDIUM.name => 2.0d
        case str if str == HARD.name => 3.0d
      }
    })
  }
  private def sendStats(): Unit = {
    controller
      .saveSettings(SettingsData(this.musicSlider.getValue.toFloat, this.effectSlider.getValue.toFloat, this.diffSlider
        .getValue.toInt, if (this
        .textPlayerName.getText.isBlank) DefaultName else this.textPlayerName.getText))
  }
  private def initTextField(): Unit = {
    val portFormatter: UnaryOperator[javafx.scene.control.TextFormatter.Change] = formatter => {
      val text: String = formatter.getControlNewText
      if (text.length <= NameMazLength || text.isEmpty) {
        formatter
      } else {
        null
      }
    }
    this.textPlayerName.setTextFormatter(new TextFormatter(portFormatter))
  }
  private def extendButtonBackBehaviour(): Unit = {
    buttonBack.addEventHandler[ActionEvent](ActionEvent.ACTION, _ => {
      sendStats()
    })
  }
  def displaySettings(settings: SettingsData): Unit = {
    this.effectSlider.setValue(settings.effectVolume)
    this.musicSlider.setValue(settings.musicVolume)
    this.diffSlider.setValue(settings.diff)
    this.textPlayerName.setText(settings.name)
  }


}
