import indi.somebottle.indexing.ChunksSpatialIndex;
import indi.somebottle.indexing.ChunksSpatialIndexFactory;
import indi.somebottle.utils.ChunkUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ListReadTest {
    @Test
    public void readList() throws IOException {
        File listFile = new File("protected_chunks.list");
        ChunksSpatialIndex index = ChunksSpatialIndexFactory.createRStarTreeIndex();
        index = ChunkUtils.readProtectedChunks(index, listFile);
        System.out.println(index.contains(2, 1));
    }

    @Test
    public void readForceLoadedChunks() throws IOException {
        File chunksDatFile = new File("chunks.dat");
        ChunksSpatialIndex index = ChunksSpatialIndexFactory.createRStarTreeIndex();
        index = ChunkUtils.protectForceLoadedChunks(index, chunksDatFile);
        System.out.println(index.contains(1, 3));
    }
}
