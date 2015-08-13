package commons.mapper

import java.lang.reflect.{ Field, Method }
import java.util.UUID

import javassist.{ ClassPool, CtClass, CtConstructor, CtMethod, Modifier }

/**
 * @author Kai Han
 */
private[mapper] trait BeanToMapMapper extends Mapper[AnyRef, java.util.Map[String, Any]] {
	override def map(bean : AnyRef) : java.util.Map[String, Any]
}

object BeanToMapMapper extends MapperFactory[AnyRef, java.util.Map[String, Any]] {
	private val classPool = ClassPool.getDefault()

	override def createMapper(clazz : Class[_]) : BeanToMapMapper = {

		val ctClass = classPool.makeClass(s"commons.mapper.BeanToMapMapper_${UUID.randomUUID().toString().replace("-", "")}")
		ctClass.setInterfaces(Array[CtClass](classPool.get(classOf[Mapper[AnyRef, java.util.Map[String, Any]]].getName()), classPool.get(classOf[BeanToMapMapper].getName())))

		var constructor = new CtConstructor(null, ctClass)
		constructor.setModifiers(Modifier.PUBLIC)
		constructor.setBody("{}")
		ctClass.addConstructor(constructor)

		val methods = clazz.getDeclaredMethods()
			.filter(method => Modifier.isPublic(method.getModifiers))
			.filterNot(method => Modifier.isStatic(method.getModifiers))
			.filter(method => method.getParameterTypes.length == 0)
			.filter(method => method.getReturnType != Void.TYPE)

		val fieldSet = clazz.getDeclaredFields().map(_.getName).toSet

		val javaGetMethods = methods
			.filter(method => method.getName.startsWith("get"))
			.filter(method => method.getName.length() > 3)

		val scalaGetMethods = methods
			.filter(method => fieldSet.contains(method.getName))

		val getMethods = javaGetMethods ++ scalaGetMethods

		val publicField = clazz.getDeclaredFields
			.filter(f => Modifier.isPublic(f.getModifiers))
			.filterNot(f => Modifier.isStatic(f.getModifiers))
			.filterNot(f => Modifier.isTransient(f.getModifiers))

		val Type = clazz.getName
		val instance = "instance"

		val setValueStr =
			(
				javaGetMethods.map(method => s"""map.put("${fieldName(method.getName)}", ${boxValue(method, instance)});""") ++
				scalaGetMethods.map(method => s"""map.put("${method.getName}", ${boxValue(method, instance)});""") ++
				publicField.map(field => s"""map.put("${field.getName}",${boxValue(field, instance)});""")
			).mkString("\r\n				")

		{
			val toMapSrc =
				s"""
			public java.util.Map map(Object bean){
				if(bean == null){
					return java.util.Collections.emptyMap();
				}

				if(!(bean instanceof ${Type})){
					return java.util.Collections.emptyMap();
				}

				${Type} ${instance} = (${Type})bean;

				java.util.Map map = new java.util.HashMap();
				${setValueStr}

				return map;
			}
			"""

			val toBeanMethod = CtMethod.make(toMapSrc, ctClass)
			ctClass.addMethod(toBeanMethod);
		}

		{
			val toMapSrc =
				s"""
			public Object map(Object bean){
				if(bean == null){
					return java.util.Collections.emptyMap();
				}

				if(!(bean instanceof ${Type})){
					return java.util.Collections.emptyMap();
				}

				${Type} ${instance} = (${Type})bean;

				java.util.Map map = new java.util.HashMap();
				${setValueStr}

				return map;
			}
			"""

			val toBeanMethod = CtMethod.make(toMapSrc, ctClass)
			ctClass.addMethod(toBeanMethod);
		}

		val childClazz = ctClass.toClass()
		var obj = childClazz.newInstance()

		obj.asInstanceOf[BeanToMapMapper]
	}

	private def fieldName(methodName : String) : String = {
		methodName.substring(3, 4).toLowerCase() + methodName.substring(4)
	}

	private def boxValue(method : Method, instance : String) : String = {
		val methodName = method.getName
		val value = s"${instance}.${method.getName}()"

		boxValue(method.getReturnType, value)
	}

	private def boxValue(field : Field, instance : String) : String = {
		val fieldName = field.getName
		val value = s"${instance}.${fieldName}"

		boxValue(field.getType, value)
	}

	private def boxValue(clazz : Class[_], value : String) = {
		clazz match {
			case t if t == classOf[Boolean] => s"java.lang.Boolean.valueOf(${value})"
			case t if t == classOf[Byte] => s"java.lang.Byte.valueOf(${value})"
			case t if t == classOf[Short] => s"java.lang.Short.valueOf(${value})"
			case t if t == classOf[Char] => s"java.lang.Character.valueOf(${value})"
			case t if t == classOf[Int] => s"java.lang.Integer.valueOf(${value})"
			case t if t == classOf[Long] => s"java.lang.Long.valueOf(${value})"
			case t if t == classOf[Float] => s"java.lang.Float.valueOf(${value})"
			case t if t == classOf[Double] => s"java.lang.Double.valueOf(${value})"
			case _ => value
		}
	}
}