package commons.mapper.utils

import java.sql.{ Blob, Clob, Date, ResultSet, ResultSetMetaData, Time, Timestamp }
import java.util.Date

/**
 * copy from org.springframework.jdbc.support.JdbcUtils
 */
private[mapper] object JdbcUtils {

	val dateTimeClass = try { Class.forName("org.joda.time.DateTime") } catch { case t : Throwable => null }
	lazy val dateTimeClassConstructor = dateTimeClass.getConstructor(classOf[Long])

	/**
	 * Retrieve a JDBC column value from a ResultSet, using the specified value type.
	 * <p>Uses the specifically typed ResultSet accessor methods, falling back to
	 * {@link #getResultSetValue(java.sql.ResultSet, int)} for unknown types.
	 * <p>Note that the returned value may not be assignable to the specified
	 * required type, in case of an unknown type. Calling code needs to deal
	 * with this case appropriately, e.g. throwing a corresponding exception.
	 * @param rs is the ResultSet holding the data
	 * @param index is the column index
	 * @param requiredType the required value type (may be {@code null})
	 * @return the value object
	 * @throws SQLException if thrown by the JDBC API
	 */
	def getResultSetValue(rs : ResultSet, index : Int, requiredType : Class[_]) : Object = {
		if (requiredType == null) {
			return getResultSetValue(rs, index);
		}

		// Explicitly extract typed value, as far as possible.
		if (classOf[String].equals(requiredType)) {
			return rs.getString(index);
		}

		var value : Object = null

		if (classOf[Boolean].equals(requiredType) || classOf[java.lang.Boolean].equals(requiredType)) {
			value = Boolean.box(rs.getBoolean(index));
		} else if (classOf[Byte].equals(requiredType) || classOf[java.lang.Byte].equals(requiredType)) {
			value = Byte.box(rs.getByte(index));
		} else if (classOf[Short].equals(requiredType) || classOf[java.lang.Short].equals(requiredType)) {
			value = Short.box(rs.getShort(index));
		} else if (classOf[Int].equals(requiredType) || classOf[java.lang.Integer].equals(requiredType)) {
			value = Int.box(rs.getInt(index));
		} else if (classOf[Long].equals(requiredType) || classOf[java.lang.Long].equals(requiredType)) {
			value = Long.box(rs.getLong(index));
		} else if (classOf[Float].equals(requiredType) || classOf[java.lang.Float].equals(requiredType)) {
			value = Float.box(rs.getFloat(index));
		} else if (classOf[Double].equals(requiredType) || classOf[java.lang.Double].equals(requiredType)
			|| classOf[java.lang.Number].equals(requiredType)) {

			value = Double.box(rs.getDouble(index));
		}

		if (value != null) {
		} else if (classOf[BigDecimal].equals(requiredType)) {
			return rs.getBigDecimal(index);
		} else if (classOf[java.sql.Date].equals(requiredType)) {
			return rs.getDate(index);
		} else if (classOf[java.sql.Time].equals(requiredType)) {
			return rs.getTime(index);
		} else if (classOf[java.sql.Timestamp].equals(requiredType) || classOf[java.util.Date].equals(requiredType)) {
			return rs.getTimestamp(index);
		} else if (classOf[Array[Byte]].equals(requiredType)) {
			return rs.getBytes(index);
		} else if (classOf[Blob].equals(requiredType)) {
			return rs.getBlob(index);
		} else if (classOf[Clob].equals(requiredType)) {
			return rs.getClob(index);
		} else if (dateTimeClass != null && dateTimeClass.equals(requiredType)) {
			val timestamp = rs.getTimestamp(index)
			if (timestamp == null) return null
			val obj = Long.box(timestamp.getTime)
			return dateTimeClassConstructor.newInstance(obj).asInstanceOf[Object]
		} else {
			try {
				return rs.getObject(index, requiredType).asInstanceOf[AnyRef];
			} catch {
				case t : Throwable => t.printStackTrace() // todo: handle error
			}

			// Fall back to getObject without type specification...
			return getResultSetValue(rs, index);
		}

		// Perform was-null check if necessary (for results that the JDBC driver returns as primitives).
		return if (rs.wasNull()) null else value;
	}

	/**
	 * Retrieve a JDBC column value from a ResultSet, using the most appropriate
	 * value type. The returned value should be a detached value object, not having
	 * any ties to the active ResultSet: in particular, it should not be a Blob or
	 * Clob object but rather a byte array or String representation, respectively.
	 * <p>Uses the {@code getObject(index)} method, but includes additional "hacks"
	 * to get around Oracle 10g returning a non-standard object for its TIMESTAMP
	 * datatype and a {@code java.sql.Date} for DATE columns leaving out the
	 * time portion: These columns will explicitly be extracted as standard
	 * {@code java.sql.Timestamp} object.
	 * @param rs is the ResultSet holding the data
	 * @param index is the column index
	 * @return the value object
	 * @throws SQLException if thrown by the JDBC API
	 * @see java.sql.Blob
	 * @see java.sql.Clob
	 * @see java.sql.Timestamp
	 */
	def getResultSetValue(rs : ResultSet, index : Int) : Object = {
		var obj = rs.getObject(index);
		var className : String = null;
		if (obj != null) {
			className = obj.getClass().getName();
		}
		if (obj.isInstanceOf[Blob]) {
			val blob = obj.asInstanceOf[Blob];
			obj = blob.getBytes(1, blob.length().toInt);
		} else if (obj.isInstanceOf[Clob]) {
			val clob = obj.asInstanceOf[Clob];
			obj = clob.getSubString(1, clob.length().toInt);
		} else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
			obj = rs.getTimestamp(index);
		} else if (className != null && className.startsWith("oracle.sql.DATE")) {
			val metaDataClassName = rs.getMetaData().getColumnClassName(index);
			if ("java.sql.Timestamp".equals(metaDataClassName) || "oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
				obj = rs.getTimestamp(index);
			} else {
				obj = rs.getDate(index);
			}
		} else if (obj != null && obj.isInstanceOf[java.sql.Date]) {
			if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index))) {
				obj = rs.getTimestamp(index);
			}
		}
		return obj;
	}

	/**
	 * Determine the column name to use. The column name is determined based on a
	 * lookup using ResultSetMetaData.
	 * <p>This method implementation takes into account recent clarifications
	 * expressed in the JDBC 4.0 specification:
	 * <p><i>columnLabel - the label for the column specified with the SQL AS clause.
	 * If the SQL AS clause was not specified, then the label is the name of the column</i>.
	 * @return the column name to use
	 * @param resultSetMetaData the current meta data to use
	 * @param columnIndex the index of the column for the look up
	 * @throws SQLException in case of lookup failure
	 */
	def lookupColumnName(resultSetMetaData : ResultSetMetaData, columnIndex : Int) : String = {
		var name = resultSetMetaData.getColumnLabel(columnIndex);

		if (name == null || name.length() < 1) {
			name = resultSetMetaData.getColumnName(columnIndex);
		}

		return name;
	}

	/**
	 * Convert a column name with underscores to the corresponding property name using "camel case".  A name
	 * like "customer_number" would match a "customerNumber" property name.
	 * @param name the column name to be converted
	 * @return the name using "camel case"
	 */
	def convertUnderscoreNameToPropertyName(name : String) : String = {
		val result = new StringBuilder(name.length());
		var nextIsUpper = false;

		if (name != null && name.length() > 0) {
			if (name.length() > 1 && name.substring(1, 2).equals("_")) {
				result.append(name.substring(0, 1).toUpperCase());
			} else {
				result.append(name.substring(0, 1).toLowerCase());
			}

			val length = name.length()
			var i = 1
			while (i < length) {
				val s = name.substring(i, i + 1);
				if (s.equals("_")) {
					nextIsUpper = true;
				} else {
					if (nextIsUpper) {
						result.append(s.toUpperCase());
						nextIsUpper = false;
					} else {
						result.append(s.toLowerCase());
					}
				}

				i += 1
			} //while end
		} //if end

		return result.toString();
	}
}