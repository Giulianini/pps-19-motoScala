package it.unibo.pps1920.motoscala.ecs.managers

import java.util.UUID

import it.unibo.pps1920.motoscala.ecs.{Component, Entity}
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EntityManagerTest extends AnyWordSpec with BeforeAndAfterAll with Matchers {
  private var entityManager: EntityManager = _
  private var entity1: Entity = _

  override def beforeAll(): Unit = {
    entityManager = EntityManager()
    entity1 = TestEntity(UUID.randomUUID())
  }

  override def afterAll(): Unit = {
  }

  "An EntityManager" when {
    "created" should {
      "be empty" in {
        entityManager.entities.isEmpty shouldBe true
      }
      "create an entity" in {
        entityManager.createEntity(entity1)
        entityManager.entities.size shouldBe 1
      }
      "remove an entity" in {
        entityManager.removeEntity(entity1)
        entityManager.entities.isEmpty shouldBe true
      }
      "remove no entity" in {
        entityManager.removeEntity(entity1)
        entityManager.entities.isEmpty shouldBe true
      }
    }
    "entity registered" should {
      "sign an entity" in {
        entityManager.signEntity(entity1, Set(classOf[Comp1], classOf[Comp2]))
      }
      "signed entity must have 2 component signed" in {
        entityManager.getEntitySignature(entity1).get shouldBe Set(classOf[Comp1], classOf[Comp2])
      }
    }
  }

  final case class Comp1() extends Component
  final case class Comp2() extends Component
  final case class TestEntity(_uuid: UUID) extends Entity {
    override def uuid: UUID = _uuid
  }
}
