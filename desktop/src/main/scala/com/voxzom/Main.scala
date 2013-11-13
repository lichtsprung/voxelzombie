package com.voxzom

import com.badlogic.gdx.backends.lwjgl._
import com.vozcom.VoxelZombieMain

object Main extends App {
  val cfg = new LwjglApplicationConfiguration()
  cfg.title = "VoxelZombie"
  cfg.height = 600
  cfg.width = 1066
  cfg.useGL20 = true
  cfg.forceExit = true
  new LwjglApplication(new VoxelZombieMain(), cfg)
}
