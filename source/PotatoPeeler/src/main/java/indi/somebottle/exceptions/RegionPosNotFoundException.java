package indi.somebottle.exceptions;

public class RegionPosNotFoundException extends Exception {
    /**
     * 自定义异常：区域文件 .mca 的坐标未找到
     *
     * @param message 异常信息
     */
    public RegionPosNotFoundException(String message) {
        super(message);
    }
}
