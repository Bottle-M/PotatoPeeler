package indi.somebottle.exceptions;

public class RegionTaskNotAcceptedException extends Exception {
    /**
     * 自定义异常：当处理 Region 的任务不被接收时抛出
     *
     * @param message 异常信息
     */
    public RegionTaskNotAcceptedException(String message) {
        super(message);
    }
}
