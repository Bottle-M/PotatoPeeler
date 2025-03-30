package indi.somebottle.versioned;

import indi.somebottle.constants.DataVersionConstants;
import indi.somebottle.versioned.handlers.ChunksDatReadHandler;
import indi.somebottle.versioned.handlers.ChunksDatReadHandlerImpl1343;
import indi.somebottle.versioned.handlers.ChunksDatReadHandlerImpl4325;

import java.io.File;

public class ChunksDatReadHandlerFactory {

    private ChunksDatReadHandlerFactory() {
    }

    /**
     * 根据 DataVersion 获取一个读取 chunks.dat 的 handler
     *
     * @param dataVersion    数据版本
     * @param chunksDatBytes chunks.dat 文件的字节数据
     * @param chunksDatFile  chunks.dat 文件的 File 对象
     * @return 读取 chunks.dat 的 ChunksDatReadHandler
     */
    public static ChunksDatReadHandler getChunksDatReadHandler(int dataVersion, byte[] chunksDatBytes, File chunksDatFile) {
        if (dataVersion < DataVersionConstants.DATA_VERSION_1_21_5) {
            // 1.21.5 之前的版本
            return new ChunksDatReadHandlerImpl1343(chunksDatBytes, chunksDatFile);
        } else {
            // 1.21.5 及之后的版本
            return new ChunksDatReadHandlerImpl4325(chunksDatBytes, chunksDatFile);
        }
    }
}
