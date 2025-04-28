package indi.somebottle.tasks.runners;

import indi.somebottle.entities.Chunk;
import indi.somebottle.entities.PeelResult;
import indi.somebottle.entities.Region;
import indi.somebottle.entities.TaskParams;
import indi.somebottle.logger.GlobalLogger;

import java.util.List;

/**
 * 区块任务执行器接口，Runnable <br>
 */
public interface RegionTaskRunner extends Runnable {
    /**
     * 取回当前线程的执行统计结果
     *
     * @return PeelResult 对象
     */
    PeelResult getTaskResult();

    /**
     * 标记区域文件中有待删除的区块 <br>
     *
     * @param region      区域文件对象
     * @param params      任务参数
     * @param mcaFileName 区域文件名
     * @return 标记待删除的区块数
     */
    default long markChunksForRemoval(Region region, TaskParams params, String mcaFileName) {
        long chunksToBeRemoved = 0; // 计算待移除的区块数
        List<Chunk> existingChunks = region.getExistingChunks();
        for (Chunk chunk : existingChunks) {
            if (chunk.isOverSized()) {
                // 如果区块数据较多，就不进行删除
                GlobalLogger.fine("Chunk at (" + chunk.getGlobalX() + "," + chunk.getGlobalZ() + ") in " + mcaFileName + " is oversize, ignored.");
                continue;
            }
            if (params.protectedChunksIndex.contains(chunk.getGlobalX(), chunk.getGlobalZ())) {
                // 如果区块在保护范围内，就不进行删除
                GlobalLogger.fine("Chunk at (" + chunk.getGlobalX() + "," + chunk.getGlobalZ() + ") in " + mcaFileName + " is protected, ignored.");
                continue;
            }
            if (chunk.getInhabitedTime() <= params.minInhabited) {
                // 如果区块的 inhabitedTime 小于等于阈值，就将其标记为待删除
                GlobalLogger.fine("Marked the chunk at (" + chunk.getGlobalX() + "," + chunk.getGlobalZ() + ") in " + mcaFileName + " for removal.");
                chunk.setDeleteFlag(true);
                chunksToBeRemoved++;
            }
        }
        return chunksToBeRemoved;
    }
}
