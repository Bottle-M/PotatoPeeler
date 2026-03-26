import indi.somebottle.utils.ChunkUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Covers path-resolution helpers used to locate world output and ticket files.
 * 覆盖用于定位世界输出目录和 ticket 文件的路径解析辅助逻辑。
 */
public class PathTest {
    /**
     * Isolated temporary workspace for generated directory structures.
     * 为测试生成的目录结构提供隔离的临时工作区。
     */
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    /**
     * Verifies the relative path calculation used when copying region files into a new world
     * directory.
     * 验证将 Region 文件复制到新世界目录时使用的相对路径计算逻辑。
     */
    @Test
    public void resolvesRegionPathRelativeToWorldDirectory() {
        Path worldPath = temp.getRoot().toPath().resolve("world");
        Path mcaPath = worldPath.resolve("region").resolve("r.0.0.mca");
        Path outputPath = temp.getRoot().toPath().resolve("newWorld");

        Path relativePath = worldPath.relativize(mcaPath);
        Path resolvedPath = outputPath.resolve(relativePath);

        assertEquals(outputPath.resolve("region").resolve("r.0.0.mca"), resolvedPath);
    }

    /**
     * Verifies that the legacy {@code data/chunks.dat} path takes precedence over nested modern
     * ticket files when both exist.
     * 验证当旧版 {@code data/chunks.dat} 与新版嵌套 ticket 文件同时存在时，优先返回旧版路径。
     *
     * @throws Exception if directory setup fails
     *                   当目录结构创建失败时抛出
     */
    @Test
    public void findChunkTicketsFilePathPrefersLegacyChunksDat() throws Exception {
        Path worldPath = temp.getRoot().toPath().resolve("world");
        Path dataPath = worldPath.resolve("data");
        Files.createDirectories(dataPath.resolve("namespace"));
        Files.createFile(dataPath.resolve("chunks.dat"));
        Files.createFile(dataPath.resolve("namespace").resolve("chunk_tickets.dat"));

        Path foundPath = ChunkUtils.findChunkTicketsFilePath(worldPath);

        assertEquals(dataPath.resolve("chunks.dat"), foundPath);
    }

    /**
     * Verifies that no ticket file is reported when the world data directory exists but contains no
     * supported ticket file.
     * 验证当世界数据目录存在但不包含任何受支持的 ticket 文件时，会返回空结果。
     *
     * @throws Exception if directory setup fails
     *                   当目录结构创建失败时抛出
     */
    @Test
    public void findChunkTicketsFilePathReturnsNullWhenNoFileExists() throws Exception {
        Path worldPath = temp.getRoot().toPath().resolve("world");
        Files.createDirectories(worldPath.resolve("data"));

        assertNull(ChunkUtils.findChunkTicketsFilePath(worldPath));
    }
}
