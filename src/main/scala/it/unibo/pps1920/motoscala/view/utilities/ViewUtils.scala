package it.unibo.pps1920.motoscala.view.utilities

import it.unibo.pps1920.motoscala.view.utilities.ViewConstants.Window.{ScreenMinHeight, ScreenMinWidth}
import javafx.event.{ActionEvent, EventHandler}
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.stage.{Stage, StageStyle, WindowEvent}
import scalafx.scene.control.Button

protected[view] object ViewUtils {
  def createStage(
    scene: Scene,
    minDim: (Int, Int) = (ScreenMinWidth, ScreenMinHeight),
    icon: Image = new Image(ViewConstants.Window.StageIconPath),
    title: String = ViewConstants.Window.Window_title,
    onCloseRequest: EventHandler[WindowEvent] = _ => System.exit(0)
  ): Stage = {
    val stage = new Stage(StageStyle.DECORATED)
    stage.setScene(scene)
    stage.getIcons.add(icon)
    stage.setTitle(title)
    stage.show()
    stage setMinWidth minDim._1
    stage setMinHeight minDim._2
    stage.centerOnScreen()
    stage setOnCloseRequest onCloseRequest
    stage
  }
  def buttonFactory(bText: String, handler: EventHandler[ActionEvent], handler2: EventHandler[MouseEvent]): Button = {
    new Button {
      text = bText
      id = "Button"
      onAction = handler
      onMouseEntered = handler2
    }
  }
}