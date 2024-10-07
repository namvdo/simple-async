package util;

import exception.SimpleAsyncException;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class GeneratorUtils {
    private GeneratorUtils() {

    }


    public static final char[] ALPHA_NUMERIC = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    public static String getRandomString(int len) {
        if (len <= 0) throw new SimpleAsyncException("len must be positive");
        String[] sb = new String[len];
        Arrays.fill(sb, null);
        for(int i = 0; i < len; i++) {
            int ranCharIdx = ThreadLocalRandom.current().nextInt(0, ALPHA_NUMERIC.length);
            char randomChar = ALPHA_NUMERIC[ranCharIdx];
            sb[i] = String.valueOf(randomChar);
        }
        return String.join("", sb);
    }

}
