import indi.somebottle.entities.Chunk;
import indi.somebottle.entities.Region;
import indi.somebottle.utils.RegionUtils;
import org.junit.Test;

import java.io.File;

public class RegionTest {
    @Test
    public void regionReadTest() {
        File regionFile = new File("C:\\Users\\58379\\Desktop\\r.-1.-1.mca");
        try {
            long startTime = System.currentTimeMillis();
            Region region = RegionUtils.readRegion(regionFile, false);
            long timeElapsed = System.currentTimeMillis() - startTime;
            System.out.println("读取耗时: " + timeElapsed + "ms");
            Chunk chunk00 = region.getChunkAt(31, 31);
            System.out.println(chunk00.getInhabitedTime());
            System.out.println(chunk00);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
