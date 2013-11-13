package com.vozcom.voxel

import com.badlogic.gdx.graphics.PerspectiveCamera

/**
  * Voxel definition.
  */
case class Voxel(x: Int, y: Int, z: Int)

class VoxelEngine(sizeX: Int, sizeY: Int, sizeZ: Int) {
  private var (posX, posY, posZ) = (0, 0, 0)

  private var viewVolume = updateVolume()

  private def f(x: Int, z: Int): Int = {

    val tx = math.sin(x.toFloat / (sizeX * 1.4)) * sizeX / 13
    val ty = math.sin(z.toFloat / (sizeX * 1.4)) * sizeX / 13
    val t = tx * ty

    if (x == 0 || x == sizeX || z == 0 || z == sizeZ)
      20
    else
      0

  }

  def updateVolumePosition(x: Int, y: Int, z: Int): Unit = {
    posX = x
    posY = y
    posZ = z
    viewVolume = updateVolume()
  }

  private def updateVolume(): Vector[Voxel] = {
    var v = Vector.empty[Voxel]
    for (x ← posX to (posX + sizeX)) {
      for (z ← posZ to (posZ + sizeZ)) {
        v = v :+ Voxel(x - posX, f(x, z), z - posZ)
      }
    }
    v
  }

  def currentVolume() = viewVolume
}
