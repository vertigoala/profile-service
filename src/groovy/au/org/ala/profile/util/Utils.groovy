package au.org.ala.profile.util

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4

class Utils {

    static final double DEFAULT_MAP_LATITUDE = -27
    static final double DEFAULT_MAP_LONGITUDE = 133.6
    static final double DEFAULT_MAP_ZOOM = 3
    static final String DEFAULT_MAP_BASE_LAYER = "https://{s}.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={token}"
    static final String UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

    static final String CHAR_ENCODING = "utf-8"

    /**
     * Whitelists safe characters to prevent possible regex injection. Escapes regex characters (like .) that are whitelisted
     *
     * @param regex The regex string to make safe
     * @return A safe version of the string
     */
    static sanitizeRegex(String regex) {
        regex?.replaceAll(/[^0-9a-zA-Z'\.",\-\(\)& ]/, "")?.replaceAll(/\.+/, ".")?.replaceAll(/([\.\-\(\)])/, "\\\\\$1")
    }

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

    static boolean isUuid(String str) {
        str =~ UUID_REGEX
    }
}
