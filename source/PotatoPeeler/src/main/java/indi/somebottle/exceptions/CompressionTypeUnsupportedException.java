package indi.somebottle.exceptions;

public class CompressionTypeUnsupportedException extends UnsupportedOperationException {
    /**
     * 自定义异常：压缩类型不支持
     *
     * @param message 异常信息
     */
    public CompressionTypeUnsupportedException(String message) {
        super(message);
    }
}
