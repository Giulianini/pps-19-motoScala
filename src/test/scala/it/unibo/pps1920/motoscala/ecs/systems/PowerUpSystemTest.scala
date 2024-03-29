package it.unibo.pps1920.motoscala.ecs.systems

import java.util.UUID

import it.unibo.pps1920.motoscala.controller.EngineController
import it.unibo.pps1920.motoscala.controller.mediation.Event.SoundEvent
import it.unibo.pps1920.motoscala.controller.mediation.Mediator
import it.unibo.pps1920.motoscala.ecs.System
import it.unibo.pps1920.motoscala.ecs.components.PowerUpEffect.{JumpPowerUp, SpeedBoostPowerUp}
import it.unibo.pps1920.motoscala.ecs.components._
import it.unibo.pps1920.motoscala.ecs.core.Coordinator
import it.unibo.pps1920.motoscala.ecs.entities.{BumperCarEntity, JumpPowerUpEntity, WeightPowerUpEntity}
import it.unibo.pps1920.motoscala.ecs.util.Vector2
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class PowerUpSystemTest extends AnyWordSpec with BeforeAndAfterAll with Matchers {
  var coordinator: Coordinator = _
  var powerup: System = _
  var controller: EngineController = _
  val pid: UUID = UUID.randomUUID()
  val entity: BumperCarEntity = BumperCarEntity(pid)
  val ep1: JumpPowerUpEntity = JumpPowerUpEntity(UUID.randomUUID())
  val ep2: JumpPowerUpEntity = JumpPowerUpEntity(UUID.randomUUID())
  val ep3: WeightPowerUpEntity = WeightPowerUpEntity(UUID.randomUUID())
  val pos: PositionComponent = PositionComponent((0, 0))
  val vel: VelocityComponent = VelocityComponent((0, 20), (20, 20))
  val col: CollisionComponent = CollisionComponent(10, mass = 2, oldSpeed = (1.0, 2.0))
  var jmp: JumpComponent = JumpComponent()
  val pUp1: PowerUpComponent = PowerUpComponent(effect = SpeedBoostPowerUp(2, isActive = false, _.dot(2)))
  val pUp2: PowerUpComponent = PowerUpComponent(effect = JumpPowerUp(2))
  val pUp3: PowerUpComponent = PowerUpComponent(effect = PowerUpEffect.WeightBoostPowerUp(2, isActive = false, _ * 2))
  val pos2: PositionComponent = PositionComponent((10, 10))

  override def beforeAll(): Unit = {
    controller = new EngineControllerMock(Mediator())
    coordinator = Coordinator()
    powerup = PowerUpSystem(coordinator, controller, 60)
    coordinator
      .registerComponentType(classOf[PositionComponent])
      .registerComponentType(classOf[VelocityComponent])
      .registerComponentType(classOf[CollisionComponent])
      .registerComponentType(classOf[JumpComponent])
      .registerComponentType(classOf[PowerUpComponent])

      .registerSystem(powerup)

      .addEntity(entity)
      .addEntityComponent(entity, pos)
      .addEntityComponent(entity, vel)
      .addEntityComponent(entity, col)
      .addEntityComponent(entity, jmp)

      .addEntity(ep1)
      .addEntityComponent(ep1, pUp1)

      .addEntity(ep2).addEntityComponent(ep2, pUp2)

      .addEntity(ep3).addEntityComponent(ep3, pUp3)

  }
  "A powerupSystem" when {
    "updating" should {
      "make the player go faster" in {
        vel.defVel = (1, 1)
        pUp1.entity = Some(entity)
        coordinator.updateSystems()
        vel.defVel shouldBe Vector2(2, 2)
      }
      "make the player jump for the right amount of ticks" in {
        jmp.isActive shouldBe false
        pUp2.entity = Some(entity)
        coordinator.updateSystems()
        jmp.isActive shouldBe true
        coordinator.updateSystems()
        jmp.isActive shouldBe false
      }
      "make the player gain weight for the right amount of ticks" in {
        col.mass shouldBe 2
        pUp3.entity = Some(entity)
        coordinator.updateSystems()
        col.mass shouldBe 4
        coordinator.updateSystems()
        col.mass shouldBe 2
      }
    }
  }
  final class EngineControllerMock(_mediator: Mediator) extends EngineController {
    override def mediator: Mediator = _mediator
    override def redirectSoundEvent(me: SoundEvent): Unit = {}
  }
}
