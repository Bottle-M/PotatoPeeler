package indi.somebottle.exceptions;

public class RegionTaskInterruptedException extends Exception {
    /**
     * 自定义异常：任务被打断
     *
     * @param message 异常信息
     */
    public RegionTaskInterruptedException(String message) {
        super(message);
    }
}
