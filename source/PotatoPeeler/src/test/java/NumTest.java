import indi.somebottle.utils.NumUtils;
import org.junit.Test;

public class NumTest {
    @Test
    public void bigEndianToNumTest() {
        byte[] testBuf = {0x01, 0x0a, (byte) 0x9f, (byte) 0xc7, 0x00, 0x42};
        System.out.println(NumUtils.bigEndianToLong(testBuf, 6));
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
}
