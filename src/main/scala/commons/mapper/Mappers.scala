package commons.mapper

import java.sql.ResultSet

import scala.collection.JavaConversions.{ mapAsJavaMap, mapAsScalaMap }
import scala.collection.concurrent.TrieMap

/**
 * @author Kai Han
 */
object Mappers {

	private val beanToMapMapperCache = new TrieMap[Class[_], BeanToMapMapper]
	private val mapToBeanMapperCache = new TrieMap[Class[_], MapToBeanMapper]
	private val autoConvertTypeMapToBeanMapperCache = new TrieMap[Class[_], MapToBeanMapper]
	private val resultSetMapperCache = new TrieMap[Class[_], ResultSetMapper]

	def beanToMap(any : AnyRef) : collection.Map[String, Any] = {
		val map = beanToMapMapperCache
			.getOrElseUpdate(any.getClass, BeanToMapMapper.createMapper(any.getClass))
			.map(any)

		mapAsScalaMap(map)
	}

	def mapToBean[T](map : collection.Map[String, Any])(implicit classTag : scala.reflect.ClassTag[T]) : T = {
		mapToBean(map, false)
	}

	def mapToBean[T](map : collection.Map[String, Any], autoConvert : Boolean)(implicit classTag : scala.reflect.ClassTag[T]) : T = {
		val clazz = classTag.runtimeClass

		val mapper =
			if (!autoConvert) mapToBeanMapperCache.getOrElseUpdate(clazz, MapToBeanMapper.createMapper(classTag.runtimeClass))
			else autoConvertTypeMapToBeanMapperCache.getOrElseUpdate(clazz, MapToBeanMapper.createMapper(classTag.runtimeClass, true))

		mapper.map(mapAsJavaMap(map)).asInstanceOf[T]
	}

	def resultSetToBean[T](rs : ResultSet)(implicit classTag : scala.reflect.ClassTag[T]) : T = {
		val clazz = classTag.runtimeClass
		resultSetMapperCache.getOrElseUpdate(clazz, ResultSetMapper.createMapper(clazz)).map(rs).asInstanceOf[T]
	}

	def resultSetToMap(rs : ResultSet) : collection.Map[String, Any] = {
		resultSetToBean[collection.Map[String, Any]](rs)
	}
}