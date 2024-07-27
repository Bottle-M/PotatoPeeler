package indi.somebottle.exceptions;

public class RegionFileNotFoundException extends Exception {
    /**
     * 自定义异常：区域文件未找到
     *
     * @param message 异常信息
     */
    public RegionFileNotFoundException(String message) {
        super(message);
    }
}
