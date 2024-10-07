package util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class ParsingUtils {
    private ParsingUtils() {

    }

    public static String getLoggedMessage(String message, Object...params) {
        if (StringUtils.isBlank(message)) {
            return "";
        }
        if (ArrayUtils.isEmpty(params)) {
            return message;
        }
        for (Object param : params) {
            if (param == null) {
                message = message.replaceFirst("\\{}", "null");
            } else {
                message = message.replaceFirst("\\{}", param.toString());
            }
        }
        return message;
    }

}
