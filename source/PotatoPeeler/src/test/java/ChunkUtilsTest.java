import indi.somebottle.constants.DataVersionConstants;
import indi.somebottle.entities.ForcedChunksLoadResult;
import indi.somebottle.exceptions.NBTFormatException;
import indi.somebottle.indexing.ChunksSpatialIndex;
import indi.somebottle.indexing.ChunksSpatialIndexFactory;
import indi.somebottle.utils.ChunkUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Covers forced-chunk loading from compressed NBT storage files across supported Minecraft formats.
 * 覆盖受支持 Minecraft 格式下，从压缩 NBT 存储文件读取强制加载区块的行为。
 */
public class ChunkUtilsTest {
    /**
     * Isolated temporary workspace for generated chunks.dat fixtures.
     * 为生成的 chunks.dat 测试夹具提供隔离的临时目录。
     */
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    /**
     * Verifies that legacy {@code chunks.dat} files store forced chunks as a long-array payload and
     * that every encoded coordinate is loaded into the spatial index.
     * 验证旧版 {@code chunks.dat} 使用 long 数组存储强制加载区块，并确保所有坐标都被加载进空间索引。
     *
     * @throws Exception if fixture generation or parsing fails
     *                   当夹具生成或解析失败时抛出
     */
    @Test
    public void protectForceLoadedChunksReadsLegacyChunksDat() throws Exception {
        Path chunksDatPath = temp.getRoot().toPath().resolve("data").resolve("chunks.dat");
        TestDataFactory.writeLegacyChunksDat(
                chunksDatPath,
                DataVersionConstants.DATA_VERSION_1_12_2,
                new long[][]{{12, -34}, {56, 78}}
        );

        ChunksSpatialIndex index = ChunksSpatialIndexFactory.createRStarTreeIndex();
        ForcedChunksLoadResult loadResult = ChunkUtils.protectForceLoadedChunks(index, chunksDatPath.toFile());
        ChunksSpatialIndex updatedIndex = loadResult.getChunksSpatialIndex();

        assertEquals(2L, loadResult.getChunksCount());
        assertTrue(updatedIndex.contains(12, -34));
        assertTrue(updatedIndex.contains(56, 78));
        assertFalse(updatedIndex.contains(0, 0));
    }

    /**
     * Verifies that modern {@code chunk_tickets.dat} files only load ticket compounds whose type
     * marks the chunk as forced.
     * 验证新版 {@code chunk_tickets.dat} 只会加载类型标记为 forced 的 ticket compound。
     *
     * @throws Exception if fixture generation or parsing fails
     *                   当夹具生成或解析失败时抛出
     */
    @Test
    public void protectForceLoadedChunksReadsModernChunkTickets() throws Exception {
        Path chunkTicketsPath = temp.getRoot().toPath().resolve("data").resolve("namespace").resolve("chunk_tickets.dat");
        TestDataFactory.writeModernChunkTicketsDat(
                chunkTicketsPath,
                DataVersionConstants.DATA_VERSION_1_21_5,
                new TestDataFactory.ChunkTicketSpec(71, 128, "minecraft:forced"),
                new TestDataFactory.ChunkTicketSpec(8, 9, "minecraft:portal"),
                new TestDataFactory.ChunkTicketSpec(-5, 7, "forced")
        );

        ChunksSpatialIndex index = ChunksSpatialIndexFactory.createRStarTreeIndex();
        ForcedChunksLoadResult loadResult = ChunkUtils.protectForceLoadedChunks(index, chunkTicketsPath.toFile());
        ChunksSpatialIndex updatedIndex = loadResult.getChunksSpatialIndex();

        assertEquals(2L, loadResult.getChunksCount());
        assertTrue(updatedIndex.contains(71, 128));
        assertTrue(updatedIndex.contains(-5, 7));
        assertFalse(updatedIndex.contains(8, 9));
    }

    /**
     * Verifies that truncated legacy NBT data is rejected instead of silently producing a partial
     * index.
     * 验证被截断的旧版 NBT 数据会被拒绝，而不是静默生成不完整的索引。
     *
     * @throws Exception if fixture generation fails
     *                   当夹具生成失败时抛出
     */
    @Test
    public void protectForceLoadedChunksRejectsTruncatedLegacyNbt() throws Exception {
        Path chunksDatPath = temp.getRoot().toPath().resolve("data").resolve("chunks.dat");
        TestDataFactory.writeTruncatedLegacyChunksDat(
                chunksDatPath,
                DataVersionConstants.DATA_VERSION_1_12_2,
                1
        );

        ChunksSpatialIndex index = ChunksSpatialIndexFactory.createRStarTreeIndex();

        assertThrows(
                NBTFormatException.class,
                () -> ChunkUtils.protectForceLoadedChunks(index, chunksDatPath.toFile())
        );
    }
}
