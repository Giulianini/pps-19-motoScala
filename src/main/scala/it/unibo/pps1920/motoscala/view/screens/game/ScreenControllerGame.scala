package it.unibo.pps1920.motoscala.view.screens.game

import it.unibo.pps1920.motoscala.controller.ObservableUI
import it.unibo.pps1920.motoscala.controller.mediation.Event.{EndData, EntityData, LifeData}
import it.unibo.pps1920.motoscala.controller.mediation.{Displayable, Event, Mediator}
import it.unibo.pps1920.motoscala.view.ViewFacade
import it.unibo.pps1920.motoscala.view.events.ViewEvent
import it.unibo.pps1920.motoscala.view.events.ViewEvent.LevelSetupEvent
import javafx.application.Platform

/** Screen controller for game FXML.
 *
 * @param viewFacade the view facade
 * @param controller the controller
 */
protected[view] final class ScreenControllerGame(protected override val viewFacade: ViewFacade,
                                                 protected override val controller: ObservableUI)
  extends AbstractScreenControllerGame(viewFacade, controller) with Displayable {

  private val mediator: Mediator = controller.mediator
  mediator.subscribe(this)
  //######################## From Mediator
  override def notifyDrawEntities(players: Set[Option[EntityData]],
                                  entities: Set[EntityData]): Unit = handleDrawEntities(players, entities)
  override def notifyEntityLife(data: LifeData): Unit = handleEntityLife(data)
  override def notifyLevelEnd(data: EndData): Unit = handleLevelEnd(data)
  override def execute(runnable: Runnable): Unit = Platform.runLater(runnable)
  //######################## From ViewFacade
  override def notify(ev: ViewEvent): Unit = ev match {
    case LevelSetupEvent(data) => handleLevelSetup(data)
    case _ => logger info "Unexpected message"
  }
  //######################## To Mediator
  override def sendCommandEvent(event: Event.CommandEvent): Unit = mediator.publishEvent(event)
}
