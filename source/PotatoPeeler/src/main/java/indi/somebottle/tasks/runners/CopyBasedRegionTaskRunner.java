package indi.somebottle.tasks.runners;

import indi.somebottle.entities.PeelResult;
import indi.somebottle.entities.Region;
import indi.somebottle.entities.TaskParams;
import indi.somebottle.logger.GlobalLogger;
import indi.somebottle.utils.RegionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Queue;

/**
 * 非原地（复制）处理区域文件的线程 <br>
 */
public class CopyBasedRegionTaskRunner implements RegionTaskRunner {
    private final TaskParams params; // 任务参数
    private final Queue<File> queue; // 此线程独有的任务队列
    private final PeelResult taskResult = new PeelResult(); // 存储本线程任务结果

    /**
     * 初始化区域文件队列处理线程(非原地)
     *
     * @param queue  区域文件队列
     * @param params 任务参数
     * @throws IllegalArgumentException 任务参数中输出路径未指定
     */
    public CopyBasedRegionTaskRunner(Queue<File> queue, TaskParams params) {
        if (params.absOutputDirPath == null) {
            // 如果输出路径未指定，阻止构造
            throw new IllegalArgumentException("Output directory path must be specified for copy-based task runner.");
        }
        this.queue = queue;
        this.params = params;
    }

    @Override
    public PeelResult getTaskResult() {
        return taskResult;
    }

    @Override
    public void run() {
        // 统计
        long sizeReduced = 0;
        long chunksRemoved = 0;
        long regionsAffected = 0;
        long startTime = System.currentTimeMillis();
        // 队列非空时不断取出进行处理
        while (!Thread.currentThread().isInterrupted() && !queue.isEmpty()) {
            File mcaFile = queue.poll();
            // 原 MCA 文件大小
            long originalLength = mcaFile.length();
            // 原 MCA 文件路径
            Path originalMCAPath = Paths.get(mcaFile.toURI());
            Path relativeMCAPath = params.absWorldDirPath.relativize(originalMCAPath.toAbsolutePath());
            // 输出 MCA 文件路径
            Path outputMCAPath = params.absOutputDirPath.resolve(relativeMCAPath);
            File outputMCAFile = outputMCAPath.toFile();
            if (outputMCAFile.exists()) {
                // 输出文件已经存在则跳过
                GlobalLogger.warning("Unable to write modified region because output file already exists: " + outputMCAFile.getAbsolutePath());
                continue;
            }
            Path outputMCAParentPath = outputMCAPath.getParent();
            try {
                // 创建必要目录
                Files.createDirectories(outputMCAParentPath);
                // 试运行时不需要复制文件
                if (!params.dryRun) {
                    // 正常流程则复制原文件
                    // 这里复制是因为如果没有区块被修改，则会跳过下面的写入，但是既然是输出到指定目录，文件还是得复制的
                    Files.copy(originalMCAPath, outputMCAPath, StandardCopyOption.COPY_ATTRIBUTES);
                }
            } catch (IOException e) {
                GlobalLogger.warning("Failed to create output directory and copy file: " + outputMCAParentPath, e);
                continue;
            }
            // ##############################
            //        Region 文件读取
            // ##############################
            Region region;
            try {
                region = RegionUtils.readRegion(mcaFile);
            } catch (Exception e) {
                // 读取失败
                GlobalLogger.warning("Exception occurred while reading region file: " + mcaFile.getAbsolutePath(), e);
                continue;
            }
            // ##############################
            //           区块筛选
            // ##############################
            long chunksMarked = markChunksForRemoval(region, params, mcaFile.getName());
            // ##############################
            //        写入 Region 文件
            // ##############################
            if (chunksMarked == 0) {
                // 如果这个区域没有被修改过，就跳过
                GlobalLogger.fine("No chunks marked for removal in region file: " + mcaFile.getAbsolutePath() + ", skipped.");
                continue;
            }
            if (params.dryRun) {
                // ------------- Dry-run -------------
                try {
                    // 模拟写入区域文件
                    long bytesWrite = RegionUtils.writeRegion(region, mcaFile, null, true);
                    sizeReduced += (originalLength - bytesWrite);
                } catch (IOException e) {
                    GlobalLogger.warning("Failed to write modified region to file(dry-run): ", e);
                    continue;
                }
            } else {
                // ------------- 实际运行(输出到指定目录) -------------
                try {
                    // 写入文件
                    RegionUtils.writeRegion(region, mcaFile, outputMCAFile, false);
                    sizeReduced += (originalLength - outputMCAFile.length());
                } catch (IOException e) {
                    GlobalLogger.warning("Failed to write modified region to file: " + outputMCAFile.getAbsolutePath(), e);
                    // 因为写入失败，outputMCAFile 可能不完整，需要重新复制一份
                    try {
                        Files.copy(originalMCAPath, outputMCAPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    } catch (IOException ex) {
                        GlobalLogger.warning("Unexpected! Failed to copy original region file to: " + outputMCAFile.getAbsolutePath(), ex);
                    }
                    continue;
                }
            }
            // 成功写入后才算正确移除区块，处理了这个区域
            regionsAffected++;
            chunksRemoved += chunksMarked;
        }
        // 更新任务结果
        taskResult.setSizeReduced(sizeReduced);
        taskResult.setChunksRemoved(chunksRemoved);
        taskResult.setRegionsAffected(regionsAffected);
        // 记录每个线程执行任务的总耗时
        taskResult.setTimeElapsed(System.currentTimeMillis() - startTime);
    }
}
