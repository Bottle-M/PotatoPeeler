package indi.somebottle.exceptions;

import java.io.IOException;

public class RegionFileNotFoundException extends IOException {
    /**
     * 自定义异常：区域文件未找到
     *
     * @param message 异常信息
     */
    public RegionFileNotFoundException(String message) {
        super(message);
    }
}
