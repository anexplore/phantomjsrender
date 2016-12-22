package com.fd.phantomjsrender.utils;

import java.util.Iterator;

/**
 * Copy From Apache Http Core Utils
 * @author Apach Http Core Utils
 */
public final class TextUtils {

    /**
     * Returns true if the parameter is null or of zero length
     */
    public static boolean isEmpty(final CharSequence s) {
        if (s == null) {
            return true;
        }
        return s.length() == 0;
    }

    /**
     * Returns true if the parameter is null or contains only whitespace
     */
    public static boolean isBlank(final CharSequence s) {
        if (s == null) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsBlanks(final CharSequence s) {
        if (s == null) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }
    
	public static String join(CharSequence separator, Iterable<?> strings) {
		Iterator<?> i = strings.iterator();
		if (!i.hasNext()) {
			return "";
		}
		StringBuilder sb = new StringBuilder(i.next().toString());
		while (i.hasNext()) {
			sb.append(separator);
			sb.append(i.next().toString());
		}
		return sb.toString();
	}

	public static String join(char separator, Iterable<?> strings) {
		return join(separator + "", strings);
	}

	/**
	 * Concatenates strings, using a separator.
	 *
	 * @param separator
	 *            to join with
	 * @param strings
	 *            to join
	 * @return the joined string
	 */
	public static String join(CharSequence separator, String[] strings) {
		// Ideally we don't have to duplicate the code here if array is
		// iterable.
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String s : strings) {
			if (first) {
				first = false;
			} else {
				sb.append(separator);
			}
			sb.append(s);
		}
		return sb.toString();
	}

	public static String join(char separator, String[] strings) {
		return join(separator + "", strings);
	}
}
