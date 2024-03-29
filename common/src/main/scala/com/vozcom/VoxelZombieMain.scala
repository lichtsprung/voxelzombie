package com.vozcom

import com.badlogic.gdx.{ Gdx, Game }
import com.badlogic.gdx.graphics.{ GL10, Color, PerspectiveCamera }
import com.badlogic.gdx.graphics.g3d.utils.{ CameraInputController, ModelBuilder }
import com.badlogic.gdx.graphics.g3d._
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import scala.Some
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.vozcom.voxel.VoxelEngine
import com.badlogic.gdx.Input.Keys

object ModelInstances {
  private var models = Map.empty[String, Model]

  def register(name: String, model: Model) = {
    models = models + (name -> model)
  }

  def apply(name: String): Option[ModelInstance] = {
    for (model ← models.get(name)) {
      return Some(new ModelInstance(model))
    }
    None
  }

  def dispose(): Unit = {
    models.values foreach (_.dispose())
  }
}

class VoxelZombieMain extends Game {
  var camera: Option[PerspectiveCamera] = None

  var modelBatch: Option[ModelBatch] = None

  var environment: Option[Environment] = None

  var camInputController: Option[CameraInputController] = None

  var boxInstances = Vector.empty[ModelInstance]

  val sizeX = 128
  val sizeY = 128
  val sizeZ = 128

  val voxelEngine = new VoxelEngine(sizeX, sizeY, sizeZ)

  var posX = 0
  var posY = 0
  var posZ = 0

  override def create() {
    camera = Some(new PerspectiveCamera(45, Gdx.graphics.getWidth, Gdx.graphics.getHeight))
    modelBatch = Some(new ModelBatch())
    environment = Some(new Environment())

    for (cam ← camera) {
      cam.position.set(-24.083656f, 16.624105f, -4.416327f)
      cam.direction.set(0.6534194f, -0.7569949f, 0.0019258386f)
      cam.near = 0.1f
      cam.far = 500f
      cam.update()
    }

    for {
      cam ← camera
    } {
      val controller = new CameraInputController(cam)
      camInputController = Some(controller)
      Gdx.input.setInputProcessor(controller)
    }

    for (e ← environment) {
      e.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
      e.add(new DirectionalLight().set(0.7f, 0.7f, 0.7f, -1f, -500f, -0.2f))
    }

    val modelBuilder = new ModelBuilder()

    ModelInstances.register("green-box", modelBuilder.createBox(0.2f, 0.2f, 0.2f, new Material(ColorAttribute.createDiffuse(Color.GREEN)), Usage.Position | Usage.Normal))
    ModelInstances.register("dark-gray-box", modelBuilder.createBox(0.2f, 0.2f, 0.2f, new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)), Usage.Position | Usage.Normal))
    ModelInstances.register("light-gray-box", modelBuilder.createBox(0.2f, 0.2f, 0.2f, new Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY)), Usage.Position | Usage.Normal))

  }

  private def handleInput(): Unit = {
    if (Gdx.input.isKeyPressed(Keys.UP))
      posZ += 1
    if (Gdx.input.isKeyPressed(Keys.DOWN))
      posZ -= 1
    if (Gdx.input.isKeyPressed(Keys.LEFT))
      posX += 1
    if (Gdx.input.isKeyPressed(Keys.RIGHT))
      posX -= 1
    if (Gdx.input.isKeyPressed(Keys.SPACE)) {
      camera.map(c ⇒ println(c.position))
      camera.map(c ⇒ println(c.direction))
    }

  }

  override def render() = {
    handleInput()

    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth, Gdx.graphics.getHeight)
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT)

    for {
      cam ← camera
      mb ← modelBatch
      e ← environment
      camController ← camInputController
    } {
      camController.update()
      mb.begin(cam)
      for {
        voxel ← voxelEngine.currentVolume()
        e ← environment
        box ← ModelInstances("green-box")
      } {
        box.transform.setToTranslation(voxel.x * 0.205f - sizeX / 2 * 0.205f, voxel.y * 0.205f, voxel.z * 0.205f - sizeZ / 2 * 0.205f)
        mb.render(box, e)
      }
      mb.end()
    }
  }

  override def dispose() = {
    ModelInstances.dispose()
    modelBatch map (_.dispose())
  }
}
