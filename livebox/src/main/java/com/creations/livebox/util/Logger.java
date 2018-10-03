package com.creations.livebox.util;

/**
 * @author Sérgio Serra on 25/08/2018.
 * sergioserra99@gmail.com
 * <p>
 * Wrapper class for android.util.Log intended to seamlessly replace imports of
 * android.util.Log in the application for this class.
 * <p>
 * Uses an internal mechanism for log level check.
 * <p>
 * Change the LOG_LEVEL field for controlling the log output. Possible values are:
 * <p>
 * VERBOSE  = 2
 * DEBUG    = 3
 * INFO     = 4
 * WARN     = 5
 * ERROR    = 6
 * ASSERT   = 7
 * SUPPRESS = 8
 */
public class Logger {

    /**
     * Application available log levels.
     */
    public static final int VERBOSE = android.util.Log.VERBOSE;
    public static final int DEBUG = android.util.Log.DEBUG;
    public static final int INFO = android.util.Log.INFO;
    public static final int WARN = android.util.Log.WARN;
    public static final int ERROR = android.util.Log.ERROR;
    public static final int ASSERT = android.util.Log.ASSERT;
    public static final int SUPPRESS = 8;
    /**
     * android.util.Log category for this class. Used for internal logging only.
     */
    protected static final String TAG = Logger.class.getSimpleName();
    /**
     * VERBOSE starts at 2 so we have to pad the array so that LEVEL_STRINGS[i] = levelName(i).
     */
    protected static final String[] LEVEL_STRINGS = {"verbose", "verbose", "verbose", "debug", "info", "warn", "error", "assert", "suppress"};
    /**
     * Application log level. Use values is LEVEL_STRINGS to define the application log level.
     */
    protected static int logLevel = DEBUG;
    /**
     * The default log category prefix is empty.
     */
    private static String mainTagPrefix = "";

    private Logger() {
    }

    /**
     * Query if a specific logging level is active.
     */
    public static boolean isVerbose() {
        return logLevel == VERBOSE;
    }

    public static boolean isDebug() {
        return logLevel <= DEBUG;
    }

    public static boolean isInfo() {
        return logLevel <= INFO;
    }

    public static boolean isWarn() {
        return logLevel <= WARN;
    }

    public static boolean isError() {
        return logLevel <= ERROR;
    }

    public static boolean isAssert() {
        return logLevel <= ASSERT;
    }

    public static boolean isSuppress() {
        return logLevel == SUPPRESS;
    }

    /**
     * Provides a log category prefix for any category.
     * E.g.:
     * <p>
     * Setting just once (e.g. in the Application):
     * setTagPrefix("Cars_");
     * <p>
     * Any log messages for now on will have a prefix:
     * android.util.Log.d("MainActivity", "This is a debug message");
     * <p>
     * The log output will be something like:
     * D/Cars_MainActivity(  501): This is a debug message
     * <p>
     * This way we can grep the log prefix instead of the process PID (which is always changing)
     * for a clear view of the app (or component) log messages.
     *
     * @param tagPrefix The log category prefix.
     */
    public static void setTagPrefix(String tagPrefix) {
        mainTagPrefix = tagPrefix;
    }

    public static void disable() {
        logLevel = SUPPRESS;
    }

    /**
     * Set logging level for your application.
     *
     * @param level The log level. You can choose from VERBOSE, DEBUG, INFO, WARN, ERROR or ASSERT
     */
    public static void setLevel(int level) {
        if (level >= VERBOSE && level <= SUPPRESS) {
            logLevel = level;
            android.util.Log.v(TAG, "setLevel() - android.util.Log level set to: " + LEVEL_STRINGS[level]);
        } else {
            android.util.Log.e(TAG, "setLevel() - Incorrect level: " + level);
        }
    }

    /**
     * Mirror method for android.util.Log.isLoggable. Doesn't call the native method.
     * Instead, calls the internal method isInternalLoggable.
     *
     * @param tag   The log category.
     * @param level The log level.
     * @return The boolean result.
     */
    public static boolean isLoggable(String tag, int level) {
        return isInternalLoggable(mainTagPrefix + tag, level);
    }

    /**
     * Used internally, only for this class. Calls the overloaded method without the
     * log category.
     *
     * @param tag   The log level.
     * @param level The log level.
     * @return The boolean result.
     */
    private static boolean isInternalLoggable(String tag, int level) {
        return isInternalLoggable(level);
    }

    /**
     * Used internally, only for this class. Determines if the log level is allowed.
     *
     * @param level The log level.
     * @return The boolean result.
     */
    private static boolean isInternalLoggable(int level) {
        return level >= logLevel;
    }

    /**
     * Mirrors the android.util.Log.d(String category, String msg) with internal log level check.
     *
     * @param tag The log category.
     * @param msg The message for logging.
     */
    public static void d(String tag, String msg) {
        if (isInternalLoggable(android.util.Log.DEBUG)) {
            android.util.Log.d(mainTagPrefix + tag, msg);
            return;
        }

        // Log with system out
        System.out.println(msg);
    }


    /**
     * Mirrors the android.util.Log.d(String category, String msg) with internal log level check.
     *
     * @param tag The log category.
     * @param msg The message for logging.
     */
    public static void d(String tag, String msg, Object... args) {
        if (isInternalLoggable(android.util.Log.DEBUG)) {
            android.util.Log.d(mainTagPrefix + tag, String.format(msg, args));
            return;
        }

        // Log with system out
        System.out.println(msg);
    }

    /**
     * Same behavior of android.util.Log.d(String category, String msg) with internal log level
     * check. But instead of requiring a TAG, uses the object passed as parameter for
     * getting the class name for the category.
     *
     * @param object The object to get the category from.
     * @param msg    The message for logging.
     */
    public static void d(Object object, String msg) {
        if (isInternalLoggable(android.util.Log.DEBUG)) {
            android.util.Log.d(mainTagPrefix + object.getClass().getSimpleName(), msg);
        }
    }

    /**
     * Mirrors the android.util.Log.d(String category, String msg, Throwable tr)
     * with internal log level check.
     *
     * @param tag The log category.
     * @param msg The message for logging.
     * @param tr  The throwable for displaying the stack trace in the log.
     */
    public static void d(String tag, String msg, Throwable tr) {
        if (isInternalLoggable(android.util.Log.DEBUG)) {
            android.util.Log.d(mainTagPrefix + tag, msg, tr);
        }
    }

    /**
     * Same behavior of android.util.Log.d(String category, String msg, Throwable tr) with
     * internal log level check. But instead of requiring a TAG, uses the object passed
     * as parameter for getting the class name for the category.
     *
     * @param object The object to get the category from.
     * @param msg    The message for logging.
     * @param tr     The throwable for displaying the stack trace in the log.
     */
    public static void d(Object object, String msg, Throwable tr) {
        if (isInternalLoggable(android.util.Log.DEBUG)) {
            android.util.Log.d(mainTagPrefix + object.getClass().getSimpleName(), msg, tr);
        }
    }

    /**
     * Mirrors the android.util.Log.e(String category, String msg) with internal log level check.
     *
     * @param tag The log category.
     * @param msg The message for logging.
     */
    public static void e(String tag, String msg) {
        if (isInternalLoggable(android.util.Log.ERROR)) {
            android.util.Log.e(mainTagPrefix + tag, msg);
        }
    }

    /**
     * Same behavior of android.util.Log.e(String category, String msg) with internal log level
     * check. But instead of requiring a TAG, uses the object passed as parameter for
     * getting the class name for the category.
     *
     * @param object The object to get the category from.
     * @param msg    The message for logging.
     */
    public static void e(Object object, String msg) {
        if (isInternalLoggable(android.util.Log.ERROR)) {
            android.util.Log.e(mainTagPrefix + object.getClass().getSimpleName(), msg);
        }
    }

    /**
     * Mirrors the android.util.Log.e(String category, String msg, Throwable tr)
     * with internal log level check.
     *
     * @param tag The log category.
     * @param msg The message for logging.
     * @param tr  The throwable for displaying the stack trace in the log.
     */
    public static void e(String tag, String msg, Throwable tr) {
        if (isInternalLoggable(android.util.Log.ERROR)) {
            android.util.Log.e(mainTagPrefix + tag, msg, tr);
        }
    }

    /**
     * Same behavior of android.util.Log.e(String category, String msg, Throwable tr) with
     * internal log level check. But instead of requiring a TAG, uses the object passed
     * as parameter for getting the class name for the category.
     *
     * @param object The object to get the category from.
     * @param msg    The message for logging.
     * @param tr     The throwable for displaying the stack trace in the log.
     */
    public static void e(Object object, String msg, Throwable tr) {
        if (isInternalLoggable(android.util.Log.ERROR)) {
            android.util.Log.e(mainTagPrefix + object.getClass().getSimpleName(), msg, tr);
        }
    }

    /**
     * Mirrors the android.util.Log.i(String category, String msg) with internal log level check.
     *
     * @param tag The log category.
     * @param msg The message for logging.
     */
    public static void i(String tag, String msg) {
        if (isInternalLoggable(android.util.Log.INFO)) {
            android.util.Log.i(mainTagPrefix + tag, msg);
        }
    }

    /**
     * Same behavior of android.util.Log.i(String category, String msg) with internal log level
     * check. But instead of requiring a TAG, uses the object passed as parameter for
     * getting the class name for the category.
     *
     * @param object The object to get the category from.
     * @param msg    The message for logging.
     */
    public static void i(Object object, String msg) {
        if (isInternalLoggable(android.util.Log.INFO)) {
            android.util.Log.i(mainTagPrefix + object.getClass().getSimpleName(), msg);
        }
    }

    /**
     * Mirrors the android.util.Log.i(String category, String msg, Throwable tr)
     * with internal log level check.
     *
     * @param tag The log category.
     * @param msg The message for logging.
     * @param tr  The throwable for displaying the stack trace in the log.
     */
    public static void i(String tag, String msg, Throwable tr) {
        if (isInternalLoggable(android.util.Log.INFO)) {
            android.util.Log.i(mainTagPrefix + tag, msg, tr);
        }
    }

    /**
     * Same behavior of android.util.Log.i(String category, String msg, Throwable tr) with
     * internal log level check. But instead of requiring a TAG, uses the object passed
     * as parameter for getting the class name for the category.
     *
     * @param object The object to get the category from.
     * @param msg    The message for logging.
     * @param tr     The throwable for displaying the stack trace in the log.
     */
    public static void i(Object object, String msg, Throwable tr) {
        if (isInternalLoggable(android.util.Log.INFO)) {
            android.util.Log.i(mainTagPrefix + object.getClass().getSimpleName(), msg, tr);
        }
    }

    /**
     * Mirrors the android.util.Log.v(String category, String msg) with internal log level check.
     *
     * @param tag The log category.
     * @param msg The message for logging.
     */
    public static void v(String tag, String msg) {
        if (isInternalLoggable(android.util.Log.VERBOSE)) {
            android.util.Log.v(mainTagPrefix + tag, msg);
        }
    }

    /**
     * Same behavior of android.util.Log.v(String category, String msg) with internal log level
     * check. But instead of requiring a TAG, uses the object passed as parameter for
     * getting the class name for the category.
     *
     * @param object The object to get the category from.
     * @param msg    The message for logging.
     */
    public static void v(Object object, String msg) {
        if (isInternalLoggable(android.util.Log.VERBOSE)) {
            android.util.Log.v(mainTagPrefix + object.getClass().getSimpleName(), msg);
        }
    }

    /**
     * Mirrors the android.util.Log.v(String category, String msg, Throwable tr)
     * with internal log level check.
     *
     * @param tag The log category.
     * @param msg The message for logging.
     * @param tr  The throwable for displaying the stack trace in the log.
     */
    public static void v(String tag, String msg, Throwable tr) {
        if (isInternalLoggable(android.util.Log.VERBOSE)) {
            android.util.Log.v(mainTagPrefix + tag, msg, tr);
        }
    }

    /**
     * Same behavior of android.util.Log.v(String category, String msg, Throwable tr) with
     * internal log level check. But instead of requiring a TAG, uses the object passed
     * as parameter for getting the class name for the category.
     *
     * @param object The object to get the category from.
     * @param msg    The message for logging.
     * @param tr     The throwable for displaying the stack trace in the log.
     */
    public static void v(Object object, String msg, Throwable tr) {
        if (isInternalLoggable(android.util.Log.VERBOSE)) {
            android.util.Log.v(mainTagPrefix + object.getClass().getSimpleName(), msg, tr);
        }
    }

    /**
     * Mirrors the android.util.Log.w(String category, String msg) with internal log level check.
     *
     * @param tag The log category.
     * @param msg The message for logging.
     */
    public static void w(String tag, String msg) {
        if (isInternalLoggable(android.util.Log.WARN)) {
            android.util.Log.w(mainTagPrefix + tag, msg);
        }
    }

    /**
     * Same behavior of android.util.Log.w(String category, String msg) with internal log level
     * check. But instead of requiring a TAG, uses the object passed as parameter for
     * getting the class name for the category.
     *
     * @param object The object to get the category from.
     * @param msg    The message for logging.
     */
    public static void w(Object object, String msg) {
        if (isInternalLoggable(android.util.Log.WARN)) {
            android.util.Log.w(mainTagPrefix + object.getClass().getSimpleName(), msg);
        }
    }

    /**
     * Mirrors the android.util.Log.w(String category, Throwable tr) with internal log level check.
     *
     * @param tag The log category.
     * @param tr  The throwable for displaying the stack trace in the log.
     */
    public static void w(String tag, Throwable tr) {
        if (isInternalLoggable(android.util.Log.WARN)) {
            android.util.Log.w(mainTagPrefix + tag, tr);
        }
    }

    /**
     * Same behavior of android.util.Log.w(String category, Throwable tr) with internal log level
     * check. But instead of requiring a TAG, uses the object passed as parameter for
     * getting the class name for the category.
     *
     * @param object The object to get the category from.
     * @param tr     The throwable for displaying the stack trace in the log.
     */
    public static void w(Object object, Throwable tr) {
        if (isInternalLoggable(android.util.Log.WARN)) {
            android.util.Log.w(mainTagPrefix + object.getClass().getSimpleName(), tr);
        }
    }

    /**
     * Mirrors the android.util.Log.w(String category, String msg, Throwable tr)
     * with internal log level check.
     *
     * @param tag The log category.
     * @param msg The message for logging.
     * @param tr  The throwable for displaying the stack trace in the log.
     */
    public static void w(String tag, String msg, Throwable tr) {
        if (isInternalLoggable(android.util.Log.WARN)) {
            android.util.Log.w(mainTagPrefix + tag, msg, tr);
        }
    }

    /**
     * Same behavior of android.util.Log.w(String category, String msg, Throwable tr) with
     * internal log level check. But instead of requiring a TAG, uses the object passed
     * as parameter for getting the class name for the category.
     *
     * @param object The object to get the category from.
     * @param msg    The message for logging.
     * @param tr     The throwable for displaying the stack trace in the log.
     */
    public static void w(Object object, String msg, Throwable tr) {
        if (isInternalLoggable(android.util.Log.WARN)) {
            android.util.Log.w(mainTagPrefix + object.getClass().getSimpleName(), msg, tr);
        }
    }

    /**
     * Low-level logging call. Wrapper class for the android.util.println(int priority, String category, String msg).
     *
     * @param priority The priority/type of this log message.
     * @param tag      Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
     * @param msg      The message you would like logged.
     * @return The number of bytes written.
     */
    public static int println(int priority, String tag, String msg) {
        return android.util.Log.println(priority, mainTagPrefix + tag, msg);
    }

}

