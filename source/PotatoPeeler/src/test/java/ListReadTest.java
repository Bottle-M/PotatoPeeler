import indi.somebottle.entities.ForcedChunksLoadResult;
import indi.somebottle.indexing.ChunksSpatialIndex;
import indi.somebottle.indexing.ChunksSpatialIndexFactory;
import indi.somebottle.utils.ChunkUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ListReadTest {
    @Test
    public void readList() throws IOException {
        File listFile = new File("chunks.protected");
        ChunksSpatialIndex index = ChunksSpatialIndexFactory.createRStarTreeIndex();
        index = ChunkUtils.readProtectedChunks(index, listFile);
        System.out.println(index.contains(1, -15));
    }

    @Test
    public void readForceLoadedChunks() throws IOException {
        File chunksDatFile = new File("chunks.dat");
        ChunksSpatialIndex index = ChunksSpatialIndexFactory.createRStarTreeIndex();
        ForcedChunksLoadResult loadResult = ChunkUtils.protectForceLoadedChunks(index, chunksDatFile);
        index = loadResult.getChunksSpatialIndex();
        System.out.println("Loaded " + loadResult.getChunksCount() + " forced chunks.");
        System.out.println(index.contains(71, 128));
    }
}
