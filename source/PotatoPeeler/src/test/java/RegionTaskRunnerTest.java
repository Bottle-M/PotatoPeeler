import indi.somebottle.entities.PeelResult;
import indi.somebottle.entities.Region;
import indi.somebottle.entities.TaskParams;
import indi.somebottle.indexing.ChunksSpatialIndexFactory;
import indi.somebottle.tasks.runners.CopyBasedRegionTaskRunner;
import indi.somebottle.tasks.runners.InPlaceRegionTaskRunner;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers failure-handling paths of region task runners, especially backup and restore behavior.
 * 覆盖 Region 任务执行器的失败处理路径，重点验证备份与恢复行为。
 */
public class RegionTaskRunnerTest {
    /**
     * Suppresses expected warning logs emitted by the simulated write-failure scenarios in this
     * test class.
     * 关闭本测试类中模拟写入失败场景所产生的预期 warning 日志。
     */
    @BeforeClass
    public static void suppressExpectedRunnerWarnings() {
        Logger.getLogger("PotatoPeeler").setLevel(Level.OFF);
    }

    /**
     * Isolated temporary workspace for generated world and output directories.
     * 为生成的世界目录和输出目录提供隔离的临时工作区。
     */
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    /**
     * Verifies that the in-place runner restores the original region file from the backup file when
     * writing the modified region fails after a partial output has already been produced.
     * 验证原地处理模式在部分输出已经产生后写入失败时，会从备份文件恢复原始 Region 文件。
     *
     * @throws Exception if fixture generation or verification fails
     *                   当夹具生成或结果校验失败时抛出
     */
    @Test
    public void inPlaceRunnerRestoresBakFileWhenWriteFails() throws Exception {
        Path worldDir = temp.getRoot().toPath().resolve("world");
        Path regionPath = worldDir.resolve("region").resolve("r.0.0.mca");
        TestDataFactory.writeRegionFile(
                regionPath,
                new TestDataFactory.RegionChunkSpec(0, 0, 2, 0L, 100L)
        );
        byte[] originalBytes = Files.readAllBytes(regionPath);

        Queue<File> queue = new ArrayDeque<>();
        queue.add(regionPath.toFile());
        TaskParams params = new TaskParams(0L, ChunksSpatialIndexFactory.createRStarTreeIndex(), false, worldDir, null);
        InPlaceRegionTaskRunner runner = new FailingInPlaceRegionTaskRunner(queue, params);

        runner.run();

        PeelResult result = runner.getTaskResult();
        assertArrayEquals(originalBytes, Files.readAllBytes(regionPath));
        assertFalse(Files.exists(regionPath.resolveSibling("r.0.0.mca.bak")));
        assertEquals(0L, result.getRegionsAffected());
        assertEquals(0L, result.getChunksRemoved());
    }

    /**
     * Verifies that the copy-based runner replaces a partially written output file with a fresh copy
     * of the original source region when write-back fails.
     * 验证复制输出模式在写回失败时，会用源 Region 文件重新覆盖部分写入的输出文件。
     *
     * @throws Exception if fixture generation or verification fails
     *                   当夹具生成或结果校验失败时抛出
     */
    @Test
    public void copyBasedRunnerReCopiesOriginalFileWhenWriteFails() throws Exception {
        Path worldDir = temp.getRoot().toPath().resolve("world");
        Path outputDir = temp.getRoot().toPath().resolve("output");
        Path regionPath = worldDir.resolve("region").resolve("r.0.0.mca");
        TestDataFactory.writeRegionFile(
                regionPath,
                new TestDataFactory.RegionChunkSpec(0, 0, 1, 0L, 100L)
        );
        byte[] originalBytes = Files.readAllBytes(regionPath);

        Queue<File> queue = new ArrayDeque<>();
        queue.add(regionPath.toFile());
        TaskParams params = new TaskParams(0L, ChunksSpatialIndexFactory.createRStarTreeIndex(), false, worldDir, outputDir);
        CopyBasedRegionTaskRunner runner = new FailingCopyBasedRegionTaskRunner(queue, params);

        runner.run();

        Path copiedPath = outputDir.resolve("region").resolve("r.0.0.mca");
        PeelResult result = runner.getTaskResult();
        assertTrue(Files.exists(copiedPath));
        assertArrayEquals(originalBytes, Files.readAllBytes(copiedPath));
        assertEquals(0L, result.getRegionsAffected());
        assertEquals(0L, result.getChunksRemoved());
    }

    /**
     * Test double that forces in-place writes to fail after leaving behind a corrupt partial file.
     * 用于模拟原地写入时先产生损坏文件、随后再失败的测试替身。
     */
    private static final class FailingInPlaceRegionTaskRunner extends InPlaceRegionTaskRunner {
        /**
         * Creates a runner with deterministic failure behavior for backup-restore testing.
         * 创建一个用于备份恢复测试的、失败行为可预测的 Runner。
         *
         * @param queue region work queue
         *              Region 文件任务队列
         * @param params task parameters
         *               任务参数
         */
        FailingInPlaceRegionTaskRunner(Queue<File> queue, TaskParams params) {
            super(queue, params);
        }

        /**
         * Writes corrupt bytes first and then fails to simulate a mid-write crash.
         * 先写入损坏字节，再抛出异常，用于模拟写到一半崩溃的场景。
         *
         * @param region ignored test region
         *               测试中未使用的 Region 对象
         * @param sourceFile source region file
         *                   源 Region 文件
         * @param outputFile destination region file
         *                   目标 Region 文件
         * @param dryRun whether the caller requested dry-run mode
         *               调用方是否请求 dry-run 模式
         * @return never returns normally
         *         不会正常返回
         * @throws IOException always thrown to trigger restore logic
         *                     始终抛出，用于触发恢复逻辑
         */
        @Override
        protected long writeRegion(Region region, File sourceFile, File outputFile, boolean dryRun) throws IOException {
            Files.write(outputFile.toPath(), new byte[]{1, 2, 3});
            throw new IOException("simulated in-place write failure");
        }
    }

    /**
     * Test double that forces copy-based writes to fail after leaving behind a corrupt partial file.
     * 用于模拟复制输出模式下先产生损坏文件、随后再失败的测试替身。
     */
    private static final class FailingCopyBasedRegionTaskRunner extends CopyBasedRegionTaskRunner {
        /**
         * Creates a runner with deterministic failure behavior for output rollback testing.
         * 创建一个用于输出回滚测试的、失败行为可预测的 Runner。
         *
         * @param queue region work queue
         *              Region 文件任务队列
         * @param params task parameters
         *               任务参数
         */
        FailingCopyBasedRegionTaskRunner(Queue<File> queue, TaskParams params) {
            super(queue, params);
        }

        /**
         * Writes corrupt bytes first and then fails to simulate a mid-write crash.
         * 先写入损坏字节，再抛出异常，用于模拟写到一半崩溃的场景。
         *
         * @param region ignored test region
         *               测试中未使用的 Region 对象
         * @param sourceFile source region file
         *                   源 Region 文件
         * @param outputFile destination region file
         *                   目标 Region 文件
         * @param dryRun whether the caller requested dry-run mode
         *               调用方是否请求 dry-run 模式
         * @return never returns normally
         *         不会正常返回
         * @throws IOException always thrown to trigger rollback logic
         *                     始终抛出，用于触发回滚逻辑
         */
        @Override
        protected long writeRegion(Region region, File sourceFile, File outputFile, boolean dryRun) throws IOException {
            Files.write(outputFile.toPath(), new byte[]{9, 8, 7});
            throw new IOException("simulated copy-based write failure");
        }
    }
}
