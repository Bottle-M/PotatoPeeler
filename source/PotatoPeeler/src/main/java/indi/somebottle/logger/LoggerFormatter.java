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
        return "[PotatoPeeler] " + dateFormatter.format(new Date(record.getMillis())) +
                " [" +
                record.getLevel().getLocalizedName() +
                "] " +
                record.getMessage() +
                System.lineSeparator();
    }
}
