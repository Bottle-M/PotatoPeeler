import indi.somebottle.entities.Chunk;
import indi.somebottle.entities.Region;
import indi.somebottle.utils.RegionUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Covers region-file level read/write behavior against generated minimal Anvil fixtures.
 * 基于最小化 Anvil 测试夹具覆盖 Region 文件读写行为。
 */
public class RegionTest {
    /**
     * Isolated temporary workspace for generated region files.
     * 为生成的 Region 测试文件提供隔离的临时目录。
     */
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    /**
     * Verifies that chunk payloads compressed with the supported Anvil compression types can all be
     * parsed back into the expected chunk metadata.
     * 验证使用受支持的 Anvil 压缩类型写入的 Chunk 负载都能被正确解析回预期的元数据。
     *
     * @throws Exception if fixture generation or region parsing fails
     *                   当夹具生成或 Region 解析失败时抛出
     */
    @Test
    public void readRegionSupportsAllChunkCompressionTypes() throws Exception {
        Path regionPath = temp.getRoot().toPath().resolve("region").resolve("r.-1.2.mca");
        TestDataFactory.writeRegionFile(
                regionPath,
                new TestDataFactory.RegionChunkSpec(0, 0, 1, 11L, 101L),
                new TestDataFactory.RegionChunkSpec(1, 0, 2, 22L, 202L),
                new TestDataFactory.RegionChunkSpec(2, 0, 3, 33L, 303L),
                new TestDataFactory.RegionChunkSpec(3, 0, 4, 44L, 404L)
        );

        Region region = RegionUtils.readRegion(regionPath.toFile());

        assertEquals(4, region.getExistingChunks().size());
        assertChunk(region.getChunkAt(0, 0), -32, 64, 11L);
        assertChunk(region.getChunkAt(1, 0), -31, 64, 22L);
        assertChunk(region.getChunkAt(2, 0), -30, 64, 33L);
        assertChunk(region.getChunkAt(3, 0), -29, 64, 44L);
        assertEquals(404L, region.getChunkModifiedTimeAt(3, 0));
    }

    /**
     * Verifies that deleted chunks are removed from the rewritten region file while untouched chunks
     * and their timestamps are preserved.
     * 验证重写 Region 文件后，被标记删除的 Chunk 会被移除，而未修改的 Chunk 及其时间戳会被保留。
     *
     * @throws Exception if fixture generation, rewrite, or verification fails
     *                   当夹具生成、重写或结果校验失败时抛出
     */
    @Test
    public void writeRegionRemovesDeletedChunksAndPreservesRemainingChunks() throws Exception {
        Path sourcePath = temp.getRoot().toPath().resolve("region").resolve("r.0.0.mca");
        Path outputPath = temp.getRoot().toPath().resolve("out").resolve("r.0.0.mca");
        TestDataFactory.writeRegionFile(
                sourcePath,
                new TestDataFactory.RegionChunkSpec(0, 0, 2, 5L, 111L),
                new TestDataFactory.RegionChunkSpec(1, 0, 1, 50L, 222L)
        );
        Files.createDirectories(outputPath.getParent());

        Region region = RegionUtils.readRegion(sourcePath.toFile());
        region.getChunkAt(0, 0).setDeleteFlag(true);

        long bytesWritten = RegionUtils.writeRegion(region, sourcePath.toFile(), outputPath.toFile(), false);
        Region rewrittenRegion = RegionUtils.readRegion(outputPath.toFile());

        assertTrue(bytesWritten < Files.size(sourcePath));
        assertNull(rewrittenRegion.getChunkAt(0, 0));
        assertEquals(0L, rewrittenRegion.getChunkModifiedTimeAt(0, 0));
        assertChunk(rewrittenRegion.getChunkAt(1, 0), 1, 0, 50L);
        assertEquals(222L, rewrittenRegion.getChunkModifiedTimeAt(1, 0));
    }

    /**
     * Asserts the core metadata extracted from a parsed chunk.
     * 断言解析后 Chunk 的核心元数据是否符合预期。
     *
     * @param chunk the parsed chunk to verify
     *              需要校验的 Chunk 对象
     * @param expectedGlobalX expected global x coordinate
     *                        期望的全局 X 坐标
     * @param expectedGlobalZ expected global z coordinate
     *                        期望的全局 Z 坐标
     * @param expectedInhabitedTime expected inhabited time value
     *                              期望的 InhabitedTime 数值
     */
    private static void assertChunk(Chunk chunk, int expectedGlobalX, int expectedGlobalZ, long expectedInhabitedTime) {
        assertNotNull(chunk);
        assertEquals(expectedGlobalX, chunk.getGlobalX());
        assertEquals(expectedGlobalZ, chunk.getGlobalZ());
        assertEquals(expectedInhabitedTime, chunk.getInhabitedTime());
    }
}
