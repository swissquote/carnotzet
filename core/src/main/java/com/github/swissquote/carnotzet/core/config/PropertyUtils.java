package com.github.swissquote.carnotzet.core.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public final class PropertyUtils {

	// Utility classes have no public constructors.
	private PropertyUtils() {
	}

	// Inspired by java.utils.Properties#store0, but sorts lines in lexicographical order and doesn't output comments in the file
	public static void outputCleanPropFile(Properties props, Path path) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.ISO_8859_1))) {
			synchronized (props) {
				List<String> keys = Collections.list(props.keys()).stream().map(Object::toString).collect(Collectors.toList());
				Collections.sort(keys);
				for (String key : keys) {
					String val = (String) props.get(key);
					key = saveConvert(key, true, true);
					/* No need to escape embedded and trailing spaces for value, hence
					 * pass false to flag.
					 */
					val = saveConvert(val, false, true);
					bw.write(key + "=" + val);
					bw.write('\n');
				}
			}
			bw.flush();
		}
	}

	// Copied from java.util.Properties (and made static)
	private static String saveConvert(String theString,
			boolean escapeSpace,
			boolean escapeUnicode) {
		int len = theString.length();
		int bufLen = len * 2;
		if (bufLen < 0) {
			bufLen = Integer.MAX_VALUE;
		}
		StringBuffer outBuffer = new StringBuffer(bufLen);

		for (int x = 0; x < len; x++) {
			char aChar = theString.charAt(x);
			// Handle common case first, selecting largest block that
			// avoids the specials below
			if ((aChar > 61) && (aChar < 127)) {
				if (aChar == '\\') {
					outBuffer.append('\\');
					outBuffer.append('\\');
					continue;
				}
				outBuffer.append(aChar);
				continue;
			}
			switch (aChar) {
				case ' ':
					if (x == 0 || escapeSpace) {
						outBuffer.append('\\');
					}
					outBuffer.append(' ');
					break;
				case '\t':
					outBuffer.append('\\');
					outBuffer.append('t');
					break;
				case '\n':
					outBuffer.append('\\');
					outBuffer.append('n');
					break;
				case '\r':
					outBuffer.append('\\');
					outBuffer.append('r');
					break;
				case '\f':
					outBuffer.append('\\');
					outBuffer.append('f');
					break;
				case '=': // Fall through
				case ':': // Fall through
				case '#': // Fall through
				case '!':
					outBuffer.append('\\');
					outBuffer.append(aChar);
					break;
				default:
					if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
						outBuffer.append('\\');
						outBuffer.append('u');
						outBuffer.append(toHex((aChar >> 12) & 0xF));
						outBuffer.append(toHex((aChar >> 8) & 0xF));
						outBuffer.append(toHex((aChar >> 4) & 0xF));
						outBuffer.append(toHex(aChar & 0xF));
					} else {
						outBuffer.append(aChar);
					}
			}
		}
		return outBuffer.toString();
	}

	/**
	 * Convert a nibble to a hex character
	 *
	 * @param nibble the nibble to convert.
	 */
	private static char toHex(int nibble) {
		return HEX_DIGIT[(nibble & 0xF)];
	}

	/**
	 * A table of hex digits
	 */
	private static final char[] HEX_DIGIT = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
	};

}
