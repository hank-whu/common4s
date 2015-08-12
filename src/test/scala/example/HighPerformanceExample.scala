package example

import scala.collection.JavaConversions.mapAsJavaMap

import commons.mapper.{ BeanToMapMapper, MapToBeanMapper }

/**
 * @author Kai Han
 */
object HighPerformanceExample {
	val mapToBeanMapper = MapToBeanMapper.createMapper(classOf[ScalaStudent])
	val beanToMapMapper = BeanToMapMapper.createMapper(classOf[ScalaStudent])

	def main(args : Array[String]) : Unit = {
		mapToBeanBeanchmark()
		println
		beanToMapBeanchmark()
	}

	def mapToBeanBeanchmark() = {
		val map = Map("id" -> 9527L, "name" -> "Hank", "age" -> 21, "sex" -> 1.toShort, "rank" -> 100)
		val jmap = mapAsJavaMap(map)

		ExecTimeUtils.repeatExec(100000, mapToBeanMapper.map(jmap))
		ExecTimeUtils.time("MapToBeanMapper", 1000000, mapToBeanMapper.map(jmap))
	}

	def beanToMapBeanchmark() = {
		val bean = ScalaStudent(9527, "Hank", 21, 1, 100)

		ExecTimeUtils.repeatExec(100000, beanToMapMapper.map(bean))
		ExecTimeUtils.time("BeanToMapMapper", 1000000, beanToMapMapper.map(bean))
	}

}

