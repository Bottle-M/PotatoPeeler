import indi.somebottle.utils.NumUtils;
import org.junit.Test;

public class NumTest {
    @Test
    public void bigEndianToNumTest() {
        byte[] testBuf = {0x01,0x0a,(byte)0x9f,(byte)0xc7, 0x00, 0x42};
        System.out.println(NumUtils.bigEndianToLong(testBuf, 6));
    }
}
