package indi.somebottle.logger;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalLogger {
    /**
     * Logger 对象，记录日志格式： [日期] [级别] 信息
     */
    private static final Logger logger = Logger.getLogger("PotatoPeeler");
    /**
     * 日志文件处理器
     */
    private static FileHandler logFileHandler = null;
    /**
     * 日志存放目录
     */
    public static String LOGGER_DIR = "./peeler_logs";

    static {
        // 输出到控制台
        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(new LoggerFormatter());
        // 只要是日志记录器记录到的，都打印出来
        ch.setLevel(Level.ALL);
        logger.addHandler(ch);
        // 记录到文件中
        File logDir = new File(LOGGER_DIR);
        if (!logDir.exists() && !logDir.mkdirs()) {
            logger.severe("Can not create log directory: " + LOGGER_DIR);
            System.exit(1);
        }
        // 默认设置日志文件大小限制为 2MB，最多保留 10 个日志文件
        resetLogFileHandler(1024 * 1024 * 2, 10);
        // 防止日志在控制台重复输出，不使用继承的 handlers
        logger.setUseParentHandlers(false);
    }

    public static void resetLogFileHandler(int sizeLimit, int maxCount) {
        if (logFileHandler != null) {
            // 移除旧的日志文件处理器(如果有的话)
            logger.removeHandler(logFileHandler);
            logFileHandler.close();
        }
        String pathPattern = LOGGER_DIR + "/peeler_%g.log";
        try {
            logFileHandler = new FileHandler(pathPattern, sizeLimit, maxCount, true);
        } catch (IOException e) {
            logger.severe("Can not create log file handler: " + e.getMessage());
            System.exit(1);
        }
        logFileHandler.setFormatter(new LoggerFormatter());
        logFileHandler.setLevel(Level.ALL);
        logger.addHandler(logFileHandler);
        logger.fine("Log file handler was reset, size limit per file: " + sizeLimit + ", max file count: " + maxCount);
    }

    /**
     * 是否启用详细输出
     *
     * @param verbose 是否启用详细输出
     * @apiNote 详细输出时相当于 Level.ALL，否则相当于 Level.INFO
     */
    public static void setVerbose(boolean verbose) {
        logger.setLevel(verbose ? Level.ALL : Level.INFO);
    }

    /**
     * 记录一条详细日志
     *
     * @param msg 日志信息
     */
    public static void fine(String msg) {
        logger.fine(msg);
    }


    /**
     * 记录一条普通消息日志
     *
     * @param msg 日志信息
     */
    public static void info(String msg) {
        logger.info(msg);
    }

    /**
     * 记录一条警告日志
     *
     * @param msg 日志信息
     */
    public static void warning(String msg) {
        logger.warning(msg);
    }

    /**
     * 记录一条警告日志，并附带异常信息
     *
     * @param msg 日志信息
     * @param e   异常对象
     */
    public static void warning(String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
    }

    /**
     * 记录一条严重错误日志
     *
     * @param msg 日志信息
     */
    public static void severe(String msg) {
        logger.severe(msg);
    }

    /**
     * 记录一条严重错误日志，并附带异常信息
     *
     * @param msg 日志信息
     * @param e   异常对象
     */
    public static void severe(String msg, Throwable e) {
        logger.log(Level.SEVERE, msg, e);
    }
}
