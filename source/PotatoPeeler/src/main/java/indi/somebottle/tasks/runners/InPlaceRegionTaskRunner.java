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
import java.util.Queue;

/**
 * 原地处理区域文件的线程 <br>
 */
public class InPlaceRegionTaskRunner implements RegionTaskRunner {
    private final TaskParams params; // 任务参数
    private final Queue<File> queue; // 此线程独有的任务队列
    private final PeelResult taskResult = new PeelResult(); // 存储本线程任务结果

    /**
     * 初始化区域文件队列处理线程(原地)
     *
     * @param queue  区域文件队列
     * @param params 任务参数
     */
    public InPlaceRegionTaskRunner(Queue<File> queue, TaskParams params) {
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
            // 队列有任务时就取出进行处理
            File mcaFile = queue.poll();
            long originalLength = mcaFile.length();
            // 原 MCA 文件路径
            Path originalMCAPath = Paths.get(mcaFile.toURI());
            // 备份 MCA 文件路径 (用于原地操作)
            Path backupMCAPath = originalMCAPath.resolveSibling(originalMCAPath.getFileName() + ".bak");
            // 备份 MCA 文件对象 (用于原地操作)
            File backupFile = backupMCAPath.toFile();
            // ##############################
            //        Region 文件读取
            // ##############################
            Region region;
            try {
                region = RegionUtils.readRegion(mcaFile);
            } catch (Exception e) {
                // 读取失败时检查有没有 .mca.bak 文件，如果有就尝试读取 .mca.bak
                GlobalLogger.warning("Exception occurred while reading region file: " + mcaFile.getAbsolutePath(), e);
                // 检查有没有 .mca.bak 文件
                if (backupFile.exists()) {
                    GlobalLogger.info("Backup file found. Trying to read backup file: " + backupFile.getAbsolutePath());
                    try {
                        // 如果有的话尝试读取 .mca.bak
                        region = RegionUtils.readRegion(backupFile);
                        // dryRun 模式下不执行这个 IO 操作
                        if (!params.dryRun) {
                            // 把无法读取的 .mca 移除，然后把备份文件重命名为 .mca，方便进行后面的流程
                            if (mcaFile.exists() && !mcaFile.delete()) {
                                GlobalLogger.warning("Failed to delete file: " + mcaFile.getAbsolutePath());
                                continue;
                            }
                            Files.move(backupMCAPath, originalMCAPath);
                        }
                    } catch (Exception ex) {
                        GlobalLogger.warning("Exception occurred while reading backup region file: " + backupFile.getAbsolutePath(), e);
                        continue;
                    }
                } else {
                    // 跳过此文件继续执行
                    continue;
                }
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
                // ------------- 实际运行(原地操作) -------------
                try {
                    // 先检查目标 backup 文件是否存在，若存在则移除
                    if (backupFile.exists() && !backupFile.delete()) {
                        GlobalLogger.warning("Failed to delete previous backup file: " + backupFile.getAbsolutePath());
                        continue;
                    }
                    // 把原文件临时重命名为 .mca.bak
                    Files.move(originalMCAPath, backupMCAPath);
                } catch (IOException e) {
                    // 文件重命名失败
                    GlobalLogger.warning("Failed to backup(rename) file:" + mcaFile.getAbsolutePath(), e);
                    continue;
                }
                // 把修改后的区域写回原 mcaFile
                try {
                    // 注意，到这里时原 .mca 文件已经被重命名了 .mca.bak
                    // 因此要从 .mca.bak 拷贝数据到新生成的 .mca
                    RegionUtils.writeRegion(region, backupFile, mcaFile, false);
                } catch (IOException e) {
                    // 写入失败，恢复备份
                    GlobalLogger.warning("Failed to write modified region to file: " + mcaFile.getAbsolutePath() + ", restoring backup...", e);
                    try {
                        // 如果 mcaFile 存在则尝试删除
                        if (mcaFile.exists() && !mcaFile.delete()) {
                            GlobalLogger.severe("Failed to delete file: " + mcaFile.getAbsolutePath(), e);
                        }
                        Files.move(backupMCAPath, originalMCAPath);
                    } catch (IOException ex) {
                        GlobalLogger.severe("Unexpected! Failed to restore backup file: " + backupMCAPath, ex);
                    }
                    continue;
                }
                // 若成功写入则删除备份
                if (!backupFile.delete()) {
                    GlobalLogger.warning("Failed to delete backup file: " + backupFile.getAbsolutePath());
                }
                // 统计减少的数据大小
                sizeReduced += (originalLength - mcaFile.length());
            }
            // 成功写入后才算正确移除区块和处理了区域
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
