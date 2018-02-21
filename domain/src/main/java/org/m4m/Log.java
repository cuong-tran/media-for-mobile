package org.m4m;

/**
 * Created by Marcin on 25.10.2017.
 */

public class Log {

    private static final Log instance = new Log();
    public static ILog log;

    public static void e(String tag, String message) {
        if (log != null)
            log.e(tag, message);
    }
}
