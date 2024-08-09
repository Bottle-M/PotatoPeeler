import indi.somebottle.utils.NumUtils;
import org.junit.Test;

public class NumTest {
    @Test
    public void bigEndianToNumTest() {
        byte[] testBuf = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xF4, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE};
        long res = NumUtils.bigEndianToLong(testBuf, 8);
        System.out.printf("%d\n", (int)(res & 0xFFFFFFFFL));
        System.out.printf("%d\n", res >> 32);
    }

    @Test
    public void numToBigEndianTest() {
        byte[] myBuf = new byte[8];
        NumUtils.longToBigEndian(1145141919810L, myBuf, 8);
        for (int i = 0; i < 8; i++) {
            System.out.printf("%02x ", myBuf[i]);
        }
    }

    @Test
    public void intToByteConversion() {
        int val = 255;
        System.out.printf("%02x", (byte) val);
    }

    @Test
    public void bytesToHumanReadable() {
        System.out.println(NumUtils.bytesToHumanReadable(1024L * 1024 + 1024L + 4));
    }
}
