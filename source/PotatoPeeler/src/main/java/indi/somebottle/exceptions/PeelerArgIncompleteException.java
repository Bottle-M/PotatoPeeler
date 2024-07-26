package indi.somebottle.exceptions;

public class PeelerArgIncompleteException extends Exception {
    /**
     * 自定义异常：PotatoPeeler 相关的参数不完整，某个参数没有指定值，比如 --min-inhabited 后面没有指定
     *
     * @param message 异常信息
     */
    public PeelerArgIncompleteException(String message) {
        super(message);
    }
}
