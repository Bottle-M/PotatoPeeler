package indi.somebottle.exceptions;

import java.io.IOException;

public class RegionFormatException extends IOException {
    /**
     * 自定义异常：区域文件格式不正确
     *
     * @param message 异常信息
     */
    public RegionFormatException(String message) {
        super(message);
    }
}
