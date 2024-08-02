package indi.somebottle.exceptions;

import java.io.IOException;

public class CompressionTypeUnsupportedException extends IOException {
    /**
     * 自定义异常：压缩类型不支持
     *
     * @param message 异常信息
     */
    public CompressionTypeUnsupportedException(String message) {
        super(message);
    }
}
