package indi.somebottle.exceptions;

public class RegionTaskAlreadyStartedException extends IllegalStateException {
    /**
     * 自定义异常：任务已经在运行
     *
     * @param message 异常信息
     */
    public RegionTaskAlreadyStartedException(String message) {
        super(message);
    }

}
