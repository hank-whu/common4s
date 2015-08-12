package commons.mapper.utils

import java.text.{ ParseException, ParsePosition, SimpleDateFormat }
import java.util.Date

/**
 * @author Kai Han
 */
private[mapper] object DateUtils {

	/**
	 * copy form apache-commons
	 * <p>Parses a string representing a date by trying a variety of different parsers.</p>
	 *
	 * <p>The parse will try each parse pattern in turn.
	 * A parse is only deemed successful if it parses the whole of the input string.
	 * If no parse patterns match, a ParseException is thrown.</p>
	 *
	 * @param str  the date to parse, not null
	 * @param parsePatterns  the date format patterns to use, see SimpleDateFormat, not null
	 * @param lenient Specify whether or not date/time parsing is to be lenient.
	 * @return the parsed date
	 * @throws IllegalArgumentException if the date string or pattern array is null
	 * @throws ParseException if none of the date patterns were suitable
	 * @see java.util.Calender#isLenient()
	 */
	def parseDateWithLeniency(str : String, parsePatterns : Array[String], lenient : Boolean) : Date = {
		if (str == null || parsePatterns == null) {
			throw new IllegalArgumentException("Date and Patterns must not be null");
		}

		val parser = new SimpleDateFormat();
		parser.setLenient(lenient);
		val pos = new ParsePosition(0);

		for (parsePattern <- parsePatterns) {

			var pattern = parsePattern;

			// LANG-530 - need to make sure 'ZZ' output doesn't get passed to SimpleDateFormat
			if (parsePattern.endsWith("ZZ")) {
				pattern = pattern.substring(0, pattern.length() - 1);
			}

			parser.applyPattern(pattern);
			pos.setIndex(0);

			var str2 = str;
			// LANG-530 - need to make sure 'ZZ' output doesn't hit SimpleDateFormat as it will ParseException
			if (parsePattern.endsWith("ZZ")) {
				str2 = str.replaceAll("([-+][0-9][0-9]):([0-9][0-9])$", "$1$2");
			}

			val date = parser.parse(str2, pos);
			if (date != null && pos.getIndex() == str2.length()) {
				return date;
			}
		}

		throw new ParseException("Unable to parse the date: " + str, -1);
	}
}