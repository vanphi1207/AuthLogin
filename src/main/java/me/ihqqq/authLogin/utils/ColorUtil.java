package me.ihqqq.authLogin.utils;

public final class ColorUtil {

    private ColorUtil() {}

    public static String colorize(String msg) {
        if (msg == null) return "";
        return msg.replace("&", "§");
    }
}
