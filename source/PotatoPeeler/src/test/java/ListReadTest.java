import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Geometry;
import indi.somebottle.utils.ChunkUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ListReadTest {
    @Test
    public void readList() throws IOException {
        File listFile = new File("protected_chunks.list");
        RTree<Boolean, Geometry> tree = ChunkUtils.readProtectedChunks(listFile);
        System.out.println(tree.size());
        System.out.println(ChunkUtils.isProtectedChunk(tree, 1, 3));
    }
}
