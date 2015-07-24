package au.org.ala.profile.util

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4

class Utils {

    static final String CHAR_ENCODING = "utf-8"

    static cleanupText(str) {
        if (str) {
            str = unescapeHtml4(str)
            // preserve line breaks as new lines, remove all other html
            str = str.replaceAll(/<p\/?>|<br\/?>/, "\n").replaceAll(/<.+?>/, "").replaceAll(" +", " ").trim()
        }
        return str
    }

    static String enc(String str) {
        URLEncoder.encode(str, CHAR_ENCODING)
    }
}
