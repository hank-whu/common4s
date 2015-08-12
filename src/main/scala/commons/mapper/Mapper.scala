package commons.mapper

/**
 * @author Kai Han
 */
trait Mapper[T, R] {
	def map(t : T) : R
}