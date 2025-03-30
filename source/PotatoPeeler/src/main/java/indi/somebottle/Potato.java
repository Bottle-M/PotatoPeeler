package indi.somebottle;

import indi.somebottle.entities.ForcedChunksLoadResult;
import indi.somebottle.entities.TaskParams;
import indi.somebottle.indexing.ChunksSpatialIndex;
import indi.somebottle.indexing.ChunksSpatialIndexFactory;
import indi.somebottle.logger.GlobalLogger;
import indi.somebottle.tasks.RegionTaskDispatcher;
import indi.somebottle.entities.PeelResult;
import indi.somebottle.exceptions.RegionFileNotFoundException;
import indi.somebottle.exceptions.RegionTaskAlreadyStartedException;
import indi.somebottle.exceptions.RegionTaskInterruptedException;
import indi.somebottle.exceptions.RegionTaskNotAcceptedException;
import indi.somebottle.utils.ChunkUtils;
import indi.somebottle.utils.RegionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Potato {
    /**
     * 受保护区块清单的文件名 <br>
     * 每个世界维度根目录下都可以配置这个文件。
     */
    public static final String PROTECTED_CHUNKS_LIST_FILENAME = "chunks.protected";

    /**
     * 对某个世界的 .mca 区域文件进行处理
     *
     * @param worldPath    世界目录路径
     * @param threadsNum   线程数
     * @param minInhabited InhabitedTime 阈值 (tick)
     * @return 处理后的结果 PeelResult
     * @throws RegionFileNotFoundException       找不到区域文件时抛出
     * @throws RegionTaskInterruptedException    任务被中断时抛出
     * @throws RegionTaskNotAcceptedException    任务不被接受时抛出
     * @throws RegionTaskAlreadyStartedException 任务重复启动时抛出
     * @throws IOException                       读取文件时可能抛出
     */
    public static PeelResult peel(String worldPath, int threadsNum, long minInhabited) throws RegionFileNotFoundException, RegionTaskInterruptedException, RegionTaskNotAcceptedException, RegionTaskAlreadyStartedException, IOException {
        // 先检查世界目录下的区域文件目录是否存在
        Path regionDirPath = RegionUtils.findRegionDirPath(worldPath);
        if (regionDirPath == null) {
            // 没有找到区域文件所在目录
            throw new RegionFileNotFoundException("Can not find region directory in " + worldPath);
        }
        // 扫描目录下的 .mca 文件
        File[] mcaFiles = regionDirPath.toFile().listFiles(file -> file.getName().endsWith(".mca"));
        if (mcaFiles == null || mcaFiles.length == 0) {
            // 没有找到 .mca 文件
            throw new RegionFileNotFoundException("Can not find .mca files in " + regionDirPath);
        }
        // 找到世界维度根目录下的受保护区块清单
        Path protectedChunksListPath = regionDirPath.resolveSibling(PROTECTED_CHUNKS_LIST_FILENAME);
        // 建立区块空间索引（R* 树实现）
        ChunksSpatialIndex protectedChunksIndex = ChunksSpatialIndexFactory.createRStarTreeIndex();
        if (Files.exists(protectedChunksListPath)) {
            // 若存在则从清单中读取受保护的区块
            protectedChunksIndex = ChunkUtils.readProtectedChunks(protectedChunksIndex, protectedChunksListPath.toFile());
            GlobalLogger.info("Protected chunks from " + protectedChunksListPath + " have been read.");
        }
        // 找到世界目录下的数据目录中的 chunks.dat
        Path chunksDatPath = regionDirPath.resolveSibling("data").resolve("chunks.dat");
        if (Files.exists(chunksDatPath)) {
            GlobalLogger.info("File chunks.dat found, reading force-loaded chunks.");
            // 如果 chunks.dat 存在，则读取本世界维度强制加载的区块，加入索引
            ForcedChunksLoadResult loadResult = ChunkUtils.protectForceLoadedChunks(protectedChunksIndex, chunksDatPath.toFile());
            protectedChunksIndex = loadResult.getChunksSpatialIndex();
            long forceLoadedChunksCount = loadResult.getChunksCount();
            GlobalLogger.info("Loaded " + forceLoadedChunksCount + " forced chunks.");
        }
        // 构建任务参数
        TaskParams params = new TaskParams(minInhabited, protectedChunksIndex);
        // 创建任务调度器
        RegionTaskDispatcher dispatcher = new RegionTaskDispatcher(threadsNum, params);
        // 把文件提交给任务调度器
        for (File mcaFile : mcaFiles) {
            dispatcher.addTask(mcaFile);
        }
        // 启动任务调度器
        dispatcher.start();
        // 等待任务完成
        if (!dispatcher.waitForCompletion()) {
            // 如果被打断了，抛出异常
            throw new RegionTaskInterruptedException("Interrupted while waiting for .mca files to be processed.");
        }
        return dispatcher.getResult();
    }
}
