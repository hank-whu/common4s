package commons.mapper

/**
 * @author Kai Han
 */
trait MapperFactory[T, R] {

	/**
	 * create a new mapper, very very slow!
	 */
	def createMapper(clazz : Class[_]) : Mapper[T, R]

	/**
	 * create a new mapper, very very slow!
	 */
	def createMapper[C]()(implicit classTag : scala.reflect.ClassTag[C]) : Mapper[T, R] = {
		createMapper(classTag.runtimeClass)
	}
}