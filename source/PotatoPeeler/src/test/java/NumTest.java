import indi.somebottle.utils.NumUtils;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Covers the numeric helper methods used by the binary parsers.
 * 覆盖二进制解析器依赖的数值辅助方法。
 */
public class NumTest {
    /**
     * Verifies that reading a full-width big-endian value preserves signed long semantics.
     * 验证读取完整宽度的大端数值时会保留有符号 long 的语义。
     */
    @Test
    public void bigEndianToLongReadsSignedLongValues() {
        byte[] testBuf = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xF4, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE};

        long result = NumUtils.bigEndianToLong(testBuf, 8);

        assertEquals(-47244640258L, result);
        assertEquals(-2, (int) (result & 0xFFFFFFFFL));
        assertEquals(-12, (int) (result >> 32));
    }

    /**
     * Verifies that encoding a long to big-endian bytes produces the expected byte sequence.
     * 验证将 long 编码为大端字节序后得到的字节序列符合预期。
     */
    @Test
    public void longToBigEndianWritesExpectedBytes() {
        byte[] actual = new byte[8];

        NumUtils.longToBigEndian(1145141919810L, actual, 8);

        byte[] expected = {0x00, 0x00, 0x01, 0x0A, (byte) 0x9F, (byte) 0xC7, 0x00, 0x42};
        assertArrayEquals(expected, actual);
    }

    /**
     * Verifies that composite byte sizes are rendered with stable human-readable units.
     * 验证复合字节大小会被稳定地格式化为可读单位字符串。
     */
    @Test
    public void bytesToHumanReadableFormatsCompoundUnits() {
        assertEquals("1 MiB 1 KiB 4 Bytes", NumUtils.bytesToHumanReadable(1024L * 1024 + 1024L + 4));
    }

    /**
     * Verifies that zero bytes are still rendered as an explicit size string.
     * 验证零字节也会被格式化为明确的大小字符串。
     */
    @Test
    public void bytesToHumanReadableKeepsZeroReadable() {
        assertEquals("0 Bytes", NumUtils.bytesToHumanReadable(0));
    }
}
