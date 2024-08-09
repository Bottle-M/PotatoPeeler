package indi.somebottle.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 和文件 I/O 相关的工具方法
 */
public class IOUtils {
    /**
     * 在输入流中寻找某个字节序列，并跳过这个序列。<br>
     * 如果找到了这个序列，执行完方法后，InputStream 下一个读取的字节就是序列后的第一个字节。<br>
     * 如果没找到这个序列，InputStream 会读到流末尾，本方法返回 false。
     *
     * @param is       输入流
     * @param bytesSeq 要寻找的字节序列
     * @return 是否找到了这个字节序列
     * @throws IOException 读取出现异常时抛出
     * @apiNote 使用此方法时需要保证 bytesSeq 中<b>没有任何相等前后缀序列</b>（由 KMP 算法性质决定）
     */
    public static boolean findAndSkipBytes(InputStream is, byte[] bytesSeq) throws IOException {
        // 用于进行字节匹配的指针
        int searchPtr = 0;
        int byteRead;
        while ((byteRead = is.read()) != -1) {
            // 与上 0xFF 提升至 int，方便比较（Java 的 byte 是有符号的）。
            if (byteRead == (bytesSeq[searchPtr] & 0xFF)) {
                // 字节匹配则指针后移
                searchPtr++;
            } else if (byteRead == (bytesSeq[0] & 0xFF)) {
                // 如果不匹配，检查开头字节是否和当前字节一致
                // 如果是的话下一次从子串第二个字节开始匹配
                searchPtr = 1;
            } else {
                // 否则下一次从头开始
                searchPtr = 0;
            }
            // 如果整个 bytesSeq 串匹配上了
            if (searchPtr == bytesSeq.length) {
                return true;
            }
        }
        return false;
    }
}
