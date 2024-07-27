package indi.somebottle.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// 此类用于记录 Peeler 程序上次的运行时间，防止每次启动都执行一次
public class TimeUtils {
    // 记录上次运行时间的文件
    private static File timeRecordFile = new File("./peeler.lrt");
    // 上次运行的时间
    private static long lastRunTime = 0;

    static {
        if (timeRecordFile.exists()) {
            try {
                // 读取上次运行的时间
                lastRunTime = Long.parseLong(Files.readString(timeRecordFile.toPath()));
            } catch (IOException e) {
                ExceptionUtils.print(e, "Failed to read last run time from file: " + timeRecordFile.getAbsolutePath());
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
            Files.writeString(timeRecordFile.toPath(), String.valueOf(lastRunTime));
        } catch (IOException e) {
            ExceptionUtils.print(e, "Failed to write last run time to file: " + timeRecordFile.getAbsolutePath());
        }
    }
}
