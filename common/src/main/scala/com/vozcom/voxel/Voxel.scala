package com.vozcom.voxel

import scala.xml.{ XML, Elem }
import com.vozcom.utils.Bresenham

/**
  * Voxel definition.
  */
case class Voxel(x: Int, y: Int, z: Int)

class VoxelEngine(sizeX: Int, sizeY: Int, sizeZ: Int) {
  private var (posX, posY, posZ) = (0, 0, 0)

  val parser = new WorldParser()
  val osm = parser.loadOSM(XML.load(classOf[WorldParser].getClassLoader.getResource("building.osm")))

  val (min, max) = osm.boundingBox()
  val widthX = max._1 - min._1
  val widthZ = max._2 - min._2

  val sx = widthX / sizeX
  val sz = widthZ / sizeZ

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

  private def updateVolume(): Set[Voxel] = {
    def createNeighbours(voxel: Voxel, count: Int): Set[Voxel] = {
      if (count == 0) {
        return Set(voxel)
      }
      val s = Set(Voxel(voxel.x + 1, voxel.y, voxel.z), Voxel(voxel.x - 1, voxel.y, voxel.z), Voxel(voxel.x, voxel.y, voxel.z + 1), Voxel(voxel.x, voxel.y, voxel.z - 1))

      s ++ createNeighbours(s.toList(0), count - 1) ++ createNeighbours(s.toList(1), count - 1) ++ createNeighbours(s.toList(2), count - 1) ++ createNeighbours(s.toList(3), count - 1)
    }

    var v = Set.empty[Voxel]

    osm.ways.values map {
      way ⇒
        for (i ← 0 until way.nodes.size - 1) {

          val start = osm.nodes(way.nodes(i))
          val end = osm.nodes(way.nodes(i + 1))

          val it = Bresenham.bresenham(
            math.round(start.lat / sx),
            math.round(start.lon / sz),
            math.round(end.lat / sx),
            math.round(end.lon / sz))

          it.foreach(i ⇒ {
            val vo = Voxel(i.x, 1, i.y)
            v = v + vo
            if (way.tags.contains(OSMParser.Tag("building", "yes"))) {
              for (n ← 2 until 25) {
                v = v + Voxel(i.x, n, i.y)
              }
            } else {
              v = v ++ createNeighbours(vo, 8)
            }
          })
        }
    }
    v
  }

  def currentVolume() = viewVolume
}

object OSMParser {

  case class Node(id: Int, lat: Float, lon: Float, tags: List[Tag])

  case class Way(id: Int, nodes: List[Int], tags: List[Tag])

  case class Tag(key: String, value: String)

  case class OSM(nodes: Map[Int, Node], ways: Map[Int, Way]) {

    def boundingBox() = {
      var min = (nodes.head._2.lat, nodes.head._2.lon)
      var max = (nodes.head._2.lat, nodes.head._2.lon)

      nodes map {
        node ⇒
          val current = (node._2.lat, node._2.lon)
          min = (math.min(current._1, min._1), math.min(current._2, min._2))
          max = (math.max(current._1, max._1), math.max(current._2, max._2))
      }
      (min, max)
    }
  }

}

class WorldParser {
  def loadOSM(root: Elem) = {
    val nodeEntries = (root \ "node") map {
      node ⇒
        val id = (node \ "@id").text.toInt
        val lat = (node \ "@lat").text.toFloat
        val lon = (node \ "@lon").text.toFloat
        val tags = (node \ "tag") map {
          tag ⇒
            val key = (tag \ "@k").text
            val value = (tag \ "@v").text
            OSMParser.Tag(key, value)
        }
        id -> OSMParser.Node(id, lat, lon, tags.toList)
    }
    val nodeMap = nodeEntries.toMap

    val wayEntries = (root \ "way") map {
      way ⇒
        val id = (way \ "@id").text.toInt
        val nodeRefs = (way \ "nd") map {
          nd ⇒
            (nd \ "@ref").text.toInt
        }
        val tags = (way \ "tag") map {
          tag ⇒
            val key = (tag \ "@k").text
            val value = (tag \ "@v").text
            OSMParser.Tag(key, value)
        }
        id -> OSMParser.Way(id, nodeRefs.toList, tags.toList)
    }

    val wayMap = wayEntries.toMap

    OSMParser.OSM(nodeMap, wayMap)
  }
}
