package commons.mapper

import java.sql.ResultSet

import scala.collection.JavaConversions.mapAsJavaMap

import commons.mapper.utils.{ JdbcUtils, TypeUtils }
import javassist.ClassPool

/**
 * @author Kai Han
 */
private[mapper] trait ResultSetMapper extends Mapper[ResultSet, AnyRef] {
	override def map(rs : ResultSet) : AnyRef
}

private[mapper] class ByMapToBeanMapper(mapToBeanMapper : MapToBeanMapper, paramTypeMap : java.util.Map[String, Class[_]]) extends ResultSetMapper {
	override def map(rs : ResultSet) : AnyRef = {
		val rsmd = rs.getMetaData();
		val columnCount = rsmd.getColumnCount();

		val mapOfColValues : java.util.Map[String, Any] = new java.util.HashMap[String, Any](columnCount * 2 / 3);

		var i = 1
		while (i <= columnCount) {
			val key = JdbcUtils.convertUnderscoreNameToPropertyName(JdbcUtils.lookupColumnName(rsmd, i));
			val clazz = paramTypeMap.get(key)
			val obj = JdbcUtils.getResultSetValue(rs, i, clazz)

			mapOfColValues.put(key, obj);

			i += 1;
		}

		mapToBeanMapper.map(mapOfColValues)
	}
}

private[mapper] class ResultSetToJavaMapMapper extends ResultSetMapper {
	override def map(rs : ResultSet) : java.util.Map[_, _] = {
		val rsmd = rs.getMetaData();
		val columnCount = rsmd.getColumnCount();

		val mapOfColValues : java.util.Map[String, Object] = new java.util.HashMap[String, Object]();

		var i = 1
		while (i <= columnCount) {
			val key = JdbcUtils.convertUnderscoreNameToPropertyName(JdbcUtils.lookupColumnName(rsmd, i));
			val obj = JdbcUtils.getResultSetValue(rs, i);

			mapOfColValues.put(key, obj);

			i += 1;
		}

		mapOfColValues;
	}
}

private[mapper] class ResultSetToScalaMapMapper extends ResultSetMapper {
	override def map(rs : ResultSet) : collection.Map[_, _] = {
		val rsmd = rs.getMetaData();
		val columnCount = rsmd.getColumnCount();

		val mapBuilder = Map.newBuilder[String, Object]

		var i = 1
		while (i <= columnCount) {
			val key = JdbcUtils.convertUnderscoreNameToPropertyName(JdbcUtils.lookupColumnName(rsmd, i));
			val obj = JdbcUtils.getResultSetValue(rs, i);

			mapBuilder += (key -> obj)

			i += 1;
		}

		mapBuilder.result();
	}
}

object ResultSetMapper extends MapperFactory[ResultSet, AnyRef] {

	private val classPool = ClassPool.getDefault()

	override def createMapper(clazz : Class[_]) : ResultSetMapper = {

		if (clazz == classOf[java.util.Map[_, _]]) {
			return new ResultSetToJavaMapMapper
		}

		if (clazz == classOf[collection.Map[_, _]]) {
			return new ResultSetToScalaMapMapper
		}

		if (classOf[java.util.Map[_, _]] isAssignableFrom clazz) {
			throw new RuntimeException("Java Map only support java.util.Map")
		}

		if (classOf[collection.Map[_, _]] isAssignableFrom clazz) {
			throw new RuntimeException("Scala Map only support scala.collection.Map")
		}

		val typeInfo = TypeUtils.extractTypeInfo(clazz)

		val argMap = typeInfo._1.map(arg => (arg.paramName, arg.paramType)).toMap[String, Class[_]]
		val methodMap = typeInfo._2.map(method => (TypeUtils.fieldName(method.getName), method.getParameterTypes.head)).toMap[String, Class[_]]

		val paramTypeMap = new java.util.HashMap[String, Class[_]]
		paramTypeMap.putAll(argMap)
		paramTypeMap.putAll(methodMap)

		val mapToBeanMapper = MapToBeanMapper.doCreateMapper(clazz, typeInfo._1, typeInfo._2)
		new ByMapToBeanMapper(mapToBeanMapper, paramTypeMap)
	}

}

