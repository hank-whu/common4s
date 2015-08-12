package commons.mapper.utils

import java.lang.reflect.{ Method, Modifier }
import java.sql.Date
import java.util.Date

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.{ Mirror, TermName, runtimeMirror, termNames, typeOf }

import commons.mapper.ArgWithDefault

/**
 * @author Kai Han
 */
private[mapper] object TypeUtils {

	private val dateParsePatterns = Array(
		"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss",
		"yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd'T'HH:mm:ss",
		"HH:mm:ss"
	)

	val dateTimeClass = try { Class.forName("org.joda.time.DateTime") } catch { case t : Throwable => null }
	lazy val dateTimeClassConstructor = dateTimeClass.getConstructor(classOf[Long])

	def fieldName(methodName : String) : String = {
		if (methodName.endsWith("_$eq")) methodName.substring(0, methodName.length() - 4)
		else if (methodName.startsWith("set")) methodName.substring(3, 4).toLowerCase() + methodName.substring(4)
		else methodName
	}

	def convertType(clazz : Class[_], value : Any) : Any = {
		if (clazz == null || value == null) {
			return null
		}

		if (clazz.isInstance(value)) {
			return value
		}

		if (clazz == classOf[Int] || clazz == classOf[java.lang.Integer]) {
			if (value.isInstanceOf[java.lang.Integer] || value.isInstanceOf[Int]) return value
			else if (value.isInstanceOf[String]) return java.lang.Integer.parseInt(value.asInstanceOf[String])
		} else if (clazz == classOf[Long] || clazz == classOf[java.lang.Long]) {
			if (value.isInstanceOf[java.lang.Long] || value.isInstanceOf[Long]) return value
			else if (value.isInstanceOf[String]) return java.lang.Long.parseLong(value.asInstanceOf[String])
		} else if (clazz == classOf[Float] || clazz == classOf[java.lang.Float]) {
			if (value.isInstanceOf[java.lang.Float] || value.isInstanceOf[Float]) return value
			else if (value.isInstanceOf[String]) return java.lang.Float.parseFloat(value.asInstanceOf[String])
		} else if (clazz == classOf[Double] || clazz == classOf[java.lang.Double]) {
			if (value.isInstanceOf[java.lang.Double] || value.isInstanceOf[Double]) return value
			else if (value.isInstanceOf[String]) return java.lang.Double.parseDouble(value.asInstanceOf[String])
		} else if (clazz == classOf[Boolean] || clazz == classOf[java.lang.Boolean]) {
			if (value.isInstanceOf[java.lang.Boolean] || value.isInstanceOf[Boolean]) return value
			else if (value.isInstanceOf[String]) return java.lang.Boolean.parseBoolean(value.asInstanceOf[String])
		} else if (clazz == classOf[Byte] || clazz == classOf[java.lang.Byte]) {
			if (value.isInstanceOf[java.lang.Byte] || value.isInstanceOf[Byte]) return value
			else if (value.isInstanceOf[String]) return java.lang.Byte.parseByte(value.asInstanceOf[String])
		} else if (clazz == classOf[Short] || clazz == classOf[java.lang.Short]) {
			if (value.isInstanceOf[java.lang.Short] || value.isInstanceOf[Short]) return value
			else if (value.isInstanceOf[String]) return java.lang.Short.parseShort(value.asInstanceOf[String])
		} else if (clazz == classOf[Char] || clazz == classOf[java.lang.Character]) {
			if (value.isInstanceOf[java.lang.Character] || value.isInstanceOf[Char]) return value
			else if (value.isInstanceOf[String]) return java.lang.Integer.parseInt(value.asInstanceOf[String]).asInstanceOf[Char]
		} else if (clazz == classOf[java.util.Date] || clazz == classOf[java.sql.Date] || clazz == dateTimeClass) {
			if (value.isInstanceOf[java.util.Date] && clazz == classOf[java.sql.Date]) {
				return new java.sql.Date(value.asInstanceOf[java.util.Date].getTime)
			} else if (value.isInstanceOf[java.util.Date] && clazz == dateTimeClass) {
				return dateTimeClassConstructor.newInstance(Long.box(value.asInstanceOf[java.util.Date].getTime)).asInstanceOf[Object]
			} else if (value.isInstanceOf[java.sql.Date] && clazz == classOf[java.util.Date]) {
				return value
			} else if (value.isInstanceOf[java.sql.Date] && clazz == dateTimeClass) {
				return dateTimeClassConstructor.newInstance(Long.box(value.asInstanceOf[java.util.Date].getTime)).asInstanceOf[Object]
			} else if (value.isInstanceOf[String]) {
				val date = DateUtils.parseDateWithLeniency(value.asInstanceOf[String], dateParsePatterns, false)

				if (clazz == classOf[java.sql.Date]) {
					return new java.sql.Date(date.getTime)
				}

				if (clazz == classOf[java.util.Date]) {
					return date
				}

				if (clazz == dateTimeClass) {
					val obj = Long.box(date.getTime)
					return dateTimeClassConstructor.newInstance(obj).asInstanceOf[Object]
				}
			}
		}

		throw new ClassCastException(value.getClass().getName + "无法转型为" + clazz.getName)
	}

	def extractTypeInfo[T](clazz : Class[T]) : (List[ArgWithDefault], List[Method]) = {
		val rm : Mirror = runtimeMirror(clazz.getClassLoader)
		val classSymbol = rm.classSymbol(clazz)

		if (classSymbol.isJava) {
			extractJavaTypeInfo(clazz)
		} else {
			extractScalaTypeInfo(clazz)
		}
	}

	private def extractJavaTypeInfo[T](clazz : Class[T]) : (List[ArgWithDefault], List[Method]) = {
		val constructors = clazz.getConstructors
			.filter(m => Modifier.isPublic(m.getModifiers))
			.toList

		if (constructors.isEmpty) {
			throw new RuntimeException(clazz + " has no public constructor")
		}

		val constructor = constructors.minBy(_.getParameterTypes.length)

		val args = constructor.getParameterTypes.map(clazz => {
			val default = clazz match {
				case t if t == classOf[Boolean] => Boolean.box(false)
				case t if t == classOf[Byte] => Byte.box(0.toByte)
				case t if t == classOf[Short] => Short.box(0.toShort)
				case t if t == classOf[Char] => Char.box(0.toChar)
				case t if t == classOf[Int] => Int.box(0)
				case t if t == classOf[Long] => Long.box(0L)
				case t if t == classOf[Float] => Float.box(0F)
				case t if t == classOf[Double] => Double.box(0D)
				case _ => null
			}

			new ArgWithDefault(clazz, null, default)
		}).toList

		val setMethods = clazz.getDeclaredMethods
			.filter(method => Modifier.isPublic(method.getModifiers))
			.filterNot(method => Modifier.isStatic(method.getModifiers))
			.filter(_.getName.startsWith("set"))
			.filter(_.getName.length > 3)
			.filter(_.getParameterTypes.length == 1)
			.toList

		(args, setMethods)
	}

	private def extractScalaTypeInfo[T](clazz : Class[T]) : (List[ArgWithDefault], List[Method]) = {
		val rm : Mirror = runtimeMirror(clazz.getClassLoader)
		val classSymbol = rm.classSymbol(clazz)

		val classMirror = rm.reflectClass(classSymbol)
		val alternatives = classSymbol.typeSignature.decl(termNames.CONSTRUCTOR).alternatives

		val constructorSymbol = alternatives.find(_.asMethod.isPrimaryConstructor).getOrElse(null)
		if (constructorSymbol == null) {
			throw new RuntimeException(clazz + " has no PrimaryConstructor")
		}

		val constructor = constructorSymbol.asMethod
		val constructorMirror = classMirror.reflectConstructor(constructor)

		lazy val module = classSymbol.companion.asModule
		lazy val companion = rm.reflectModule(module).instance
		lazy val companionMirror = rm.reflect(companion)

		val args = constructor.paramLists.flatten.zipWithIndex.map(paramWithIndex => {
			val param = paramWithIndex._1

			val paramName = param.name.toString

			val default = if (param.asTerm.isParamWithDefault) {
				val index = paramWithIndex._2 + 1
				val defaultTermName = TermName("$lessinit$greater$default$" + index)
				val methodSymbol = classSymbol.typeSignature.companion.decl(defaultTermName).asMethod
				companionMirror.reflectMethod(methodSymbol)()
			} else {
				param.typeSignature match {
					case t if t =:= typeOf[Boolean] => Boolean.box(false)
					case t if t =:= typeOf[Byte] => Byte.box(0.toByte)
					case t if t =:= typeOf[Short] => Short.box(0.toShort)
					case t if t =:= typeOf[Char] => Char.box(0.toChar)
					case t if t =:= typeOf[Int] => Int.box(0)
					case t if t =:= typeOf[Long] => Long.box(0L)
					case t if t =:= typeOf[Float] => Float.box(0F)
					case t if t =:= typeOf[Double] => Double.box(0D)
					case t if t =:= typeOf[Object] => null.asInstanceOf[AnyRef]
					case _ => null.asInstanceOf[AnyRef]
				}
			}

			ArgWithDefault(rm.runtimeClass(param.typeSignature.erasure), paramName, default)
		}).toList

		val setMethodSet = args.map(_.paramName + "_$eq").toSet

		val setMethods = clazz.getDeclaredMethods
			.filter(method => Modifier.isPublic(method.getModifiers))
			.filterNot(method => Modifier.isStatic(method.getModifiers))
			.filter(_.getName.endsWith("_$eq"))
			.filter(_.getName.length > 4)
			.filter(_.getParameterTypes.length == 1)
			.filter(method => !setMethodSet.contains(method.getName))
			.toList

		(args, setMethods)
	}
}