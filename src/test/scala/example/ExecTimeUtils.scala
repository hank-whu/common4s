package example

import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION
import scala.math.BigInt
import scala.math.BigInt.{ int2bigInt, long2bigInt }
import java.text.NumberFormat

/**
 * @author Kai Han
 */
object ExecTimeUtils {

	private def format(number : Long) = NumberFormat.getInstance().format(number)

	def time[T](tag : String, repeat : Int, func : => T) : T = {
		assert(repeat > 0)

		printf("%s-begin\r\n", tag)
		val startTime = System.currentTimeMillis()

		val result = repeatExec(repeat, func)

		val runTime = System.currentTimeMillis() - startTime

		if (repeat == 1) printf("%s-finish, totalTime:%dms\r\n", tag, runTime)
		else printf("%s-finish, repeat:%s, totalTime:%sms, avgTime:%sns, QPS:%s/s\r\n",
			tag,
			format(repeat),
			format(runTime),
			format((BigInt(1000 * 1000) * runTime / repeat).toLong),
			format(1000L * repeat / runTime))

		result
	}

	def repeatExec[T](repeat : Int, func : => T) : T = {
		assert(repeat > 0)

		var result = func

		var condition = 1
		while (condition < repeat) {
			condition += 1
			result = func
		}

		result
	}

}