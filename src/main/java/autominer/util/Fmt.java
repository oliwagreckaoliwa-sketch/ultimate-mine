package autominer.util;

import java.util.Locale;

public final class Fmt {
    private Fmt() {}

    public static String time(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }

    public static String num(long n) {
        return String.format(Locale.ROOT, "%,d", n).replace(',', ' ');
    }

    /** "custom_rock" -> "Custom Rock". */
    public static String prettify(String path) {
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (char ch : path.toCharArray()) {
            if (ch == '_' || ch == '/' || ch == '-') {
                sb.append(' ');
                up = true;
            } else if (up) {
                sb.append(Character.toUpperCase(ch));
                up = false;
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
