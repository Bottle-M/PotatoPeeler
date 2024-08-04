package indi.somebottle.tasks;

import indi.somebottle.entities.Chunk;
import indi.somebottle.entities.Region;
import indi.somebottle.exceptions.RegionChunkInitializedException;
import indi.somebottle.exceptions.RegionFormatException;
import indi.somebottle.exceptions.RegionPosNotFoundException;
import indi.somebottle.utils.ExceptionUtils;
import indi.somebottle.utils.RegionUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Queue;

// 实现 Runnable，在一个线程内从队列中取出 .mca 文件进行处理
public class RegionTaskRunner implements Runnable {
    private final long minInhabited;
    private final long mcaModifiableDelay;
    private final boolean verboseOutput;
    private final Queue<File> queue; // 此线程独有的任务队列

    public RegionTaskRunner(Queue<File> queue, long minInhabited, long mcaModifiableDelay, boolean verboseOutput) {
        this.queue = queue;
        this.minInhabited = minInhabited;
        this.mcaModifiableDelay = mcaModifiableDelay;
        this.verboseOutput = verboseOutput;
    }

    @Override
    public void run() {
        // 队列非空时不断取出进行处理
        while (!Thread.currentThread().isInterrupted() && !queue.isEmpty()) {
            // 队列有任务时就取出进行处理
            File mcaFile = queue.poll();
            // 先检查自 mca 文件创建以来过去了多久
            if (mcaFile.exists() && System.currentTimeMillis() - mcaFile.lastModified() < mcaModifiableDelay * 60 * 1000) {
                // 说明 mca 文件创建时间距离现在还不够久，不能修改
                continue;
            }
            // 先读取 Region 文件
            Region region;
            try {
                region = RegionUtils.readRegion(mcaFile, verboseOutput);
            } catch (Exception e) {
                ExceptionUtils.print(e, "Exception occurred while reading region.");
                // 停止继续执行
                return;
            }
            // 扫描区域所有现存区块，进行筛选
            List<Chunk> existingChunks = region.getExistingChunks();
            for (Chunk chunk : existingChunks) {
                if (chunk.getInhabitedTime() < minInhabited) {
                    // 如果区块的 inhabitedTime 小于阈值，就将其标记为待删除
                    chunk.setDeleteFlag(true);
                }
            }
        }
    }
}
