package indi.somebottle.exceptions;

import java.io.IOException;

public class NBTFormatException extends IOException {
    /**
     * 自定义异常：文件中的 NBT 格式不正确
     *
     * @param message 异常信息
     */
    public NBTFormatException(String message) {
        super(message);
    }
}
