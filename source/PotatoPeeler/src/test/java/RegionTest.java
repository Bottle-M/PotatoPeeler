import indi.somebottle.entities.Chunk;
import indi.somebottle.entities.Region;
import indi.somebottle.utils.RegionUtils;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class RegionTest {
    @Test
    public void regionReadTest() {
        File regionFile = new File("E:\\Projects\\TestArea\\minecraft-server\\world\\region\\r.0.0.mca");
        try {
            long startTime = System.currentTimeMillis();
            Region region = RegionUtils.readRegion(regionFile);
            long timeElapsed = System.currentTimeMillis() - startTime;
            System.out.println("读取耗时: " + timeElapsed + "ms");
            Chunk chunk00 = region.getChunkAt(6, 7);
            System.out.println(chunk00.getInhabitedTime());
            System.out.println(chunk00);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void regionModifyTest() {
        File regionFile = new File("E:\\Projects\\TestArea\\regionFiles\\r.-1.0.mca.zlib");
        File outputFile = new File("E:\\Projects\\TestArea\\regionFiles\\r.0.0.mca.modified");
        try {
            long startTime = System.currentTimeMillis();
            Region region = RegionUtils.readRegion(regionFile);
            // 扫描区域所有现存区块，进行筛选
            List<Chunk> existingChunks = region.getExistingChunks();
            for (Chunk chunk : existingChunks) {
                if (chunk.isOverSized()) {
                    // 如果区块数据较多，就不进行删除
                    continue;
                }
                if (chunk.getInhabitedTime() < 20) {
                    // 如果区块的 inhabitedTime 小于阈值，就将其标记为待删除
                    chunk.setDeleteFlag(true);
                }
            }
            RegionUtils.writeRegion(region, regionFile, outputFile);
            long timeElapsed = System.currentTimeMillis() - startTime;
            System.out.println("处理耗时: " + timeElapsed + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
