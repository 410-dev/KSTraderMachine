package liblks.utils;

import liblks.security.CoreSHA;

import java.util.function.UnaryOperator;

public class StringDeriver {

    private static String constructor(String data, int[] blocks, String delimiters, UnaryOperator<String> nextChainBuilder) {
        int startAt = 0;
        StringBuilder sb = new StringBuilder();
        String lastBlockGenerated = data;
        for(int idx : blocks) {
            while (startAt >= data.length() || idx + startAt >= data.length()) {
                lastBlockGenerated = nextChainBuilder.apply(lastBlockGenerated);
                data += lastBlockGenerated;
            }
            sb.append(data, startAt, idx + startAt);
            sb.append(delimiters);
            startAt = idx;
        }
        return sb.substring(0, sb.toString().length() - delimiters.length());
    }

    public static String deriveStringFrom(String seed, int[] blocks, String delimiters) {
        String data = CoreSHA.hash512(seed);
        return constructor(data, blocks, delimiters, s -> new StringBuilder(CoreSHA.hash512(s)).reverse().toString());
    }

    public static String deriveUnpredictableStringFrom(String seed, int[] blocks, String delimiters) {
        String data = CoreSHA.hash512(seed, String.valueOf(System.currentTimeMillis()));
        return constructor(data, blocks, delimiters, s -> CoreSHA.hash512(s, String.valueOf(System.currentTimeMillis())));
    }
}
