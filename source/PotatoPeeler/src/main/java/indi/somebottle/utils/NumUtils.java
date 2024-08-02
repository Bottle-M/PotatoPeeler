package indi.somebottle.utils;

/**
 * 用于辅助数值转换的类
 */
public class NumUtils {
    /**
     * 大端 8 字节无符号数转换时各字节左移的位数
     */
    private static final int[] BIG_ENDIAN_SHIFTS = {56, 48, 40, 32, 24, 16, 8, 0};

    /**
     * 字节序列按大端转换为数值（最高支持 8 字节）
     *
     * @param buf   存储字节序列的 byte[]
     * @param width 数字占有几字节
     * @return 数值
     */
    public static long bigEndianToLong(byte[] buf, int width) {
        long res = 0;
        int bufInd = 0;
        for (int i = BIG_ENDIAN_SHIFTS.length - width; i < BIG_ENDIAN_SHIFTS.length; i++) {
            // 先把某个字节的数字与 0xFF 转换为 int，再左移指定位数
            // 因为 byte 在 Java 中也有符号。
            // 移位后再进行或运算，相当于把各个字节移动到对应位置上后拼起来
            res |= ((long) (buf[bufInd] & 0xFF) << BIG_ENDIAN_SHIFTS[i]);
            bufInd++;
        }
        return res;
    }
}
