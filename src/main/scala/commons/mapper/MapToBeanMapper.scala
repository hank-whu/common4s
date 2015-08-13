package commons.mapper

import java.lang.reflect.Method
import java.util.UUID

import commons.mapper.utils.TypeUtils
import javassist.{ ClassClassPath, ClassPool, CtClass, CtConstructor, CtMethod, Modifier }

/**
 * @author Kai Han
 */
private[mapper] trait MapToBeanMapper extends Mapper[java.util.Map[String, Any], AnyRef] {
	override def map(m : java.util.Map[String, Any]) : AnyRef
}

private[mapper] abstract class AbstractMapToBeanMapper extends MapToBeanMapper {
	private var defaultValues : Array[Object] = null

	def getDefaultValue(index : Int) : Object = defaultValues(index)

	def setDefaultValues(values : java.util.ArrayList[Object]) : Unit = {
		if (values != null) this.defaultValues = values.toArray()
	}

	override def map(m : java.util.Map[String, Any]) : AnyRef
}

object MapToBeanMapper extends MapperFactory[java.util.Map[String, Any], AnyRef] {
	private val classPool = ClassPool.getDefault()
	classPool.insertClassPath(new ClassClassPath(this.getClass()))

	override def createMapper(clazz : Class[_]) : MapToBeanMapper = {
		createMapper(clazz, false)
	}

	def createMapper(clazz : Class[_], autoConvertType : Boolean) : MapToBeanMapper = {
		val typeInfo = TypeUtils.extractTypeInfo(clazz)
		doCreateMapper(clazz, typeInfo._1, typeInfo._2, autoConvertType)
	}

	private[mapper] def doCreateMapper(clazz : Class[_], args : List[ArgWithDefault], setMethods : List[Method], autoConvertType : Boolean = false) : MapToBeanMapper = {
		val ctClass = classPool.makeClass(s"commons.mapper.MapToBeanMapper_${UUID.randomUUID().toString().replace("-", "")}")
		ctClass.setInterfaces(Array[CtClass](classPool.get(classOf[Mapper[java.util.Map[String, Any], AnyRef]].getName()), classPool.get(classOf[MapToBeanMapper].getName())))
		ctClass.setSuperclass(classPool.get(classOf[AbstractMapToBeanMapper].getName()))

		var constructor = new CtConstructor(null, ctClass)
		constructor.setModifiers(Modifier.PUBLIC)
		constructor.setBody("{}")
		ctClass.addConstructor(constructor)

		//private Type newInstance(java.util.Map[String, Any] map)
		val defaultValues = addNewInstanceMethod(ctClass, clazz, args, autoConvertType)

		//private void modifyValues(Type bean, java.util.Map map) 
		addModifyValuesMethod(ctClass, clazz, setMethods, autoConvertType)

		//public Object map(java.util.Map map) 
		addMapToBeanMethod(ctClass, clazz)

		val childClazz = ctClass.toClass()

		var obj = childClazz.newInstance()

		val mapper = obj.asInstanceOf[AbstractMapToBeanMapper]
		mapper.setDefaultValues(defaultValues)

		mapper
	}

	private def fixArgs(args : List[ArgWithDefault]) : List[ArgWithDefault] = {
		args.zipWithIndex.map(argWithIndex => {
			val arg = argWithIndex._1
			val index = argWithIndex._2

			if (arg.paramName != null) arg
			else arg.copy(paramName = s"_arg_${index}")
		})
	}

	/**
	 * private Type newInstance(java.util.Map map)
	 */
	private def addNewInstanceMethod(ctClass : CtClass, clazz : Class[_], args : List[ArgWithDefault], autoConvertType : Boolean) : java.util.ArrayList[Object] = {
		val Type = clazz.getName

		val defaultValues = new java.util.ArrayList[Object]()

		val fixedArgs = fixArgs(args)

		val declaration = fixedArgs
			.map(arg => declareField(arg, defaultValues))
			.mkString("\r\n				").trim()

		def convertType(clazz : Class[_]) =
			if (autoConvertType) {
				s"obj = commons.mapper.utils.TypeUtils.convertType(${clazz.getName}.class, obj);"
			} else {
				""
			}

		val setValues = args.filter(_.paramName != null).map(arg => {
			s"""
					obj = map.get("${arg.paramName}");
					if(obj != null){
						${convertType(arg.paramType)}
						${arg.paramName} = ${unboxValue(arg.paramType, "obj")};
					}
			"""
		}).mkString("").trim()

		val argValues = fixedArgs.map(arg => arg.paramName).mkString(", ")

		val newInstanceSrc =
			s"""
			private ${Type} newInstance(java.util.Map map){
				${declaration}
		
				if(map != null || map.size() == 0){
					Object obj = null;

					${setValues}
				}
		
				${Type} instance = new ${Type}(${argValues});

				return instance;
			}
			"""

		val newInstanceMethod = CtMethod.make(newInstanceSrc, ctClass)
		ctClass.addMethod(newInstanceMethod)

		defaultValues
	}

	/**
	 * private void modifyValues(Type bean, java.util.Map map)
	 */
	private def addModifyValuesMethod(ctClass : CtClass, clazz : Class[_], setMethods : List[Method], autoConvertType : Boolean) : Unit = {

		val Type = clazz.getName

		def convertType(clazz : Class[_]) =
			if (autoConvertType) {
				s"obj = commons.mapper.utils.TypeUtils.convertType(${clazz.getName}.class, obj);"
			} else {
				""
			}

		val setValues = setMethods.map(method => {
			val key = TypeUtils.fieldName(method.getName)
			val clazz = method.getParameterTypes.head

			s"""
				obj = map.get("${key}");
				if(obj != null){
					${convertType(clazz)}
					bean.${method.getName}(${unboxValue(clazz, "obj")});
				}
			"""
		}).mkString("").trim()

		val modifyValuesSrc =
			s"""
			private void modifyValues(${Type} bean, java.util.Map map){
				if(map == null || map.size() == 0){
					return;
				}

				Object obj = null;

				${setValues}
			}
			"""

		val modifyValuesMethod = CtMethod.make(modifyValuesSrc, ctClass)
		ctClass.addMethod(modifyValuesMethod)
	}

	/**
	 * public Object map(java.util.Map map)
	 */
	private def addMapToBeanMethod(ctClass : CtClass, clazz : Class[_]) : Unit = {

		val Type = clazz.getName //

		{
			val toBeanSrc =
				s"""
			public Object map(java.util.Map m){
				${Type} bean = newInstance(m);
				modifyValues(bean, m);
				return bean;
			}
			"""

			val toBeanMethod = CtMethod.make(toBeanSrc, ctClass)
			ctClass.addMethod(toBeanMethod)
		}

		{
			val toBeanSrc =
				s"""
			public Object map(Object obj){
				return map((java.util.Map) obj);
			}
			"""

			val toBeanMethod = CtMethod.make(toBeanSrc, ctClass)
			ctClass.addMethod(toBeanMethod)
		}
	}

	private def unboxValue(paramType : Class[_], value : String) : String = {
		paramType match {
			case t if t == classOf[Boolean] => s"((Boolean)${value}).booleanValue()"
			case t if t == classOf[Byte] => s"((Byte)${value}).byteValue()"
			case t if t == classOf[Short] => s"((Short)${value}).shortValue()"
			case t if t == classOf[Char] => s"((Character)${value}).charValue()"
			case t if t == classOf[Int] => s"((Integer)${value}).intValue()"
			case t if t == classOf[Long] => s"((Long)${value}).longValue()"
			case t if t == classOf[Float] => s"((Float)${value}).floatValue()"
			case t if t == classOf[Double] => s"((Double)${value}).doubleValue()"
			case _ => s"(${paramType.getName})${value}"
		}
	}

	private def declareField(argWithDefault : ArgWithDefault, defaultValues : java.util.ArrayList[AnyRef]) : String = {
		val paramName = argWithDefault.paramName
		val paramType = argWithDefault.paramType
		val defaultValue = argWithDefault.defaultValue

		if (defaultValue == null) return s"${paramType.getName} ${paramName} = null;"

		val value = paramType match {
			case t if t == classOf[Boolean] => String.valueOf(defaultValue)
			case t if t == classOf[Byte] => s"(byte)${defaultValue}"
			case t if t == classOf[Short] => s"(short)${defaultValue}"
			case t if t == classOf[Char] => s"(char)${defaultValue}"
			case t if t == classOf[Int] => String.valueOf(defaultValue)
			case t if t == classOf[Long] => s"${defaultValue}L"
			case t if t == classOf[Float] => s"${defaultValue}F"
			case t if t == classOf[Double] => s"${defaultValue}D"
			case _ => {
				defaultValues.add(defaultValue.asInstanceOf[AnyRef])
				s"(${paramType.getName})getDefaultValue(${defaultValues.size() - 1})"
			}
		}

		s"${paramType.getName} ${paramName} = ${value};"
	}

}