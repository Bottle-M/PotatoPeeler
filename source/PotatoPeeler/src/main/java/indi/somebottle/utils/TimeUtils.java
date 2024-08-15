package indi.somebottle.utils;

import indi.somebottle.logger.GlobalLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 此类用于记录 Peeler 程序上次的运行时间，防止每次启动都执行一次
 */

public class TimeUtils {
    // 记录上次运行时间的文件的路径
    private static final Path timeRecordFilePath = Paths.get("./peeler.lrt");
    // 上次运行的时间
    private static long lastRunTime = 0;

    static {
        if (Files.exists(timeRecordFilePath)) {
            try {
                // 读取上次运行的时间
                // 支持 Java 8 API
                lastRunTime = Long.parseLong(new String(Files.readAllBytes(timeRecordFilePath)));
            } catch (IOException e) {
                GlobalLogger.severe("Failed to read last run time from file: " + timeRecordFilePath, e);
                System.exit(1);
            }
        }
    }

    public static long getLastRunTime() {
        return lastRunTime;
    }

    /**
     * 返回当前的秒级时间戳
     *
     * @return 当前的秒级时间戳
     */
    public static long timeNow() {
        return System.currentTimeMillis() / 1000;
    }

    public static void setLastRunTime(long lastRunTime) {
        TimeUtils.lastRunTime = lastRunTime;
        try {
            // 支持 Java 8 API
            Files.write(timeRecordFilePath, Long.toString(lastRunTime).getBytes());
        } catch (IOException e) {
            GlobalLogger.severe("Failed to write last run time to file: " + timeRecordFilePath, e);
        }
    }
}
