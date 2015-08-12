# common4s
common utils for scala

## Example

```scala
package example

import commons.mapper.Mappers

object EasyExample {
	def main(args : Array[String]) : Unit = {
		var map : collection.Map[String, Any] = 
			Map("id" -> 9527L, "name" -> "Hank", "age" -> 21, "sex" -> 1.toShort)

		var bean : AnyRef = Mappers.mapToBean[ScalaStudent](map)
		println(bean)

		bean = Mappers.mapToBean[JavaStudent](map)
		println(bean)

		map = Map("id" -> "9527", "name" -> "Hank", "age" -> "21", "sex" -> "1")

		bean = Mappers.mapToBean[ScalaStudent](map, true)
		println(bean)

		map = Mappers.beanToMap(bean)
		println(map)

		println

		map = Map("id" -> 9527L, "name" -> "Hank", "age" -> 21, "sex" -> 1.toShort)
		ExecTimeUtils.repeatExec(100000, Mappers.mapToBean[ScalaStudent](map))
		ExecTimeUtils.time("mapToBean", 1000000, Mappers.mapToBean[ScalaStudent](map))

		println

		ExecTimeUtils.repeatExec(100000, Mappers.beanToMap(bean))
		ExecTimeUtils.time("beanToMap", 1000000, Mappers.beanToMap(bean))
	}
}
```