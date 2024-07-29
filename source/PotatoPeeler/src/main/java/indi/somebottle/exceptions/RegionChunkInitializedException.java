package indi.somebottle.exceptions;

public class RegionChunkInitializedException extends Exception {
    /**
     * 自定义异常：Chunk 已经初始化
     *
     * @param message 异常信息
     */
    public RegionChunkInitializedException(String message) {
        super(message);
    }
}
