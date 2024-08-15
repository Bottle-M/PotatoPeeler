package indi.somebottle.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * 将日志条目格式化为 [日期] [级别] 信息
 */
public class LoggerFormatter extends Formatter {
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]");

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder("[PotatoPeeler] ");
        sb.append(dateFormatter.format(new Date(record.getMillis())))
                .append(" [")
                .append(record.getLevel().getName())
                .append("] ")
                .append(record.getMessage());
        if (record.getThrown() != null) {
            // 本条日志附带有 Exception
            sb.append("\n");
            sb.append(record.getThrown().toString());
            StackTraceElement[] stackTraceElements = record.getThrown().getStackTrace();
            for (StackTraceElement element : stackTraceElements) {
                sb.append("\n");
                sb.append("\tat ");
                sb.append(element.toString());
            }
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
