package indi.somebottle.exceptions;

public class WorldNotExistException extends Exception {
    /**
     * 自定义异常：世界目录不存在
     *
     * @param message 异常信息
     */
    public WorldNotExistException(String message) {
        super(message);
    }
}
