package indi.somebottle.tasks;

import indi.somebottle.entities.Chunk;
import indi.somebottle.entities.PeelResult;
import indi.somebottle.entities.Region;
import indi.somebottle.logger.GlobalLogger;
import indi.somebottle.utils.RegionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;

// 实现 Runnable，在一个线程内从队列中取出 .mca 文件进行处理
public class RegionTaskRunner implements Runnable {
    private final long minInhabited;
    private final long mcaModifiableDelay;
    private final Queue<File> queue; // 此线程独有的任务队列
    private final PeelResult taskResult = new PeelResult(); // 存储本线程任务结果

    public RegionTaskRunner(Queue<File> queue, long minInhabited, long mcaModifiableDelay) {
        this.queue = queue;
        this.minInhabited = minInhabited;
        this.mcaModifiableDelay = mcaModifiableDelay;
    }

    /**
     * 取回当前线程的执行统计结果
     *
     * @return PeelResult 对象
     */
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
            // MCA 文件路径
            Path originalMCAPath = Paths.get(mcaFile.toURI());
            // 备份 MCA 文件路径
            Path backupMCAPath = originalMCAPath.resolveSibling(originalMCAPath.getFileName() + ".bak");
            // 备份 MCA 文件对象
            File backupFile = new File(backupMCAPath.toString());
            // 先检查自 mca 文件创建以来过去了多久
            if (mcaFile.exists() && System.currentTimeMillis() - mcaFile.lastModified() < mcaModifiableDelay * 60 * 1000) {
                // 说明 mca 文件创建时间距离现在还不够久，不能修改
                continue;
            }
            // 先读取 Region 文件
            Region region;
            try {
                region = RegionUtils.readRegion(mcaFile);
            } catch (Exception e) {
                // 读取失败时检查有没有 .mca.bak 文件，如果有就尝试读取 .mca.bak
                GlobalLogger.severe("Exception occurred while reading region file: " + mcaFile.getAbsolutePath(), e);
                // 检查有没有 .mca.bak 文件
                if (backupFile.exists()) {
                    GlobalLogger.info("Backup file found. Trying to read backup file: " + backupFile.getAbsolutePath());
                    try {
                        // 如果有的话尝试读取 .mca.bak
                        region = RegionUtils.readRegion(backupFile);
                    } catch (Exception ex) {
                        GlobalLogger.severe("Exception occurred while reading backup region file: " + backupFile.getAbsolutePath(), e);
                        continue;
                    }
                } else {
                    // 跳过此文件继续执行
                    continue;
                }
            }
            // 扫描区域所有现存区块，进行筛选
            boolean hasModified = false; // 标记是否对区块进行了修改
            List<Chunk> existingChunks = region.getExistingChunks();
            for (Chunk chunk : existingChunks) {
                if (chunk.isOverSized()) {
                    // 如果区块数据较多，就不进行删除
                    continue;
                }
                if (chunk.getInhabitedTime() < minInhabited) {
                    // 如果区块的 inhabitedTime 小于阈值，就将其标记为待删除
                    GlobalLogger.fine("Removed chunk at (" + chunk.getX() + "," + chunk.getZ() + ") in " + mcaFile.getName());
                    chunk.setDeleteFlag(true);
                    chunksRemoved++;
                    hasModified = true;
                }
            }
            // 如果这个区域没有被修改过，就跳过
            if (!hasModified) {
                continue;
            }
            // 先把原文件重命名为 .mca.bak 后缀
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
                GlobalLogger.severe("Failed to backup(rename) file:" + mcaFile.getAbsolutePath(), e);
                continue;
            }
            // TODO： 待测试：有个问题，区块长度在游戏保存后可能是有增长的，这样紧凑写入真的没问题吗
            // 把修改后的区域写回原 mcaFile
            try {
                // 注意，到这里时原 .mca 文件已经被重命名了 .mca.bak
                // 因此要从 .mca.bak 拷贝数据到新生成的 .mca
                RegionUtils.writeRegion(region, backupFile, mcaFile);
            } catch (IOException e) {
                // 写入失败，恢复备份
                GlobalLogger.severe("Failed to write modified region to file: " + mcaFile.getAbsolutePath() + ", restoring backup...", e);
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
            // 统计
            sizeReduced += (originalLength - mcaFile.length());
            regionsAffected++;
        }
        // 更新任务结果
        taskResult.setSizeReduced(sizeReduced);
        taskResult.setChunksRemoved(chunksRemoved);
        taskResult.setRegionsAffected(regionsAffected);
        // 记录每个线程执行任务的总耗时
        taskResult.setTimeElapsed(System.currentTimeMillis() - startTime);
    }
}
