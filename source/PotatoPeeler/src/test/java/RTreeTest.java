import indi.somebottle.indexing.ChunksSpatialIndex;
import indi.somebottle.indexing.ChunksSpatialIndexFactory;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers the spatial-index behavior used to protect chunks and chunk ranges from removal.
 * 覆盖用于保护区块和区块范围不被移除的空间索引行为。
 */
public class RTreeTest {
    /**
     * Verifies that point entries are indexed and queried precisely.
     * 验证点状条目会被精确索引并正确命中查询。
     */
    @Test
    public void pointEntriesCanBeQueried() {
        ChunksSpatialIndex index = ChunksSpatialIndexFactory.createRStarTreeIndex()
                .add(1, 2)
                .add(-5, 7);

        assertTrue(index.contains(1, 2));
        assertTrue(index.contains(-5, 7));
        assertFalse(index.contains(0, 0));
    }

    /**
     * Verifies that rectangular protected areas include their covered points while leaving outside
     * coordinates unmatched.
     * 验证矩形保护区域会覆盖其内部坐标，同时不会错误命中外部坐标。
     */
    @Test
    public void rectangularEntriesCoverTheirWholeArea() {
        ChunksSpatialIndex index = ChunksSpatialIndexFactory.createRStarTreeIndex()
                .add(0, 0, 3, 1)
                .add(2, 0, 3, 4)
                .add(1, 2);

        assertTrue(index.contains(2, 4));
        assertTrue(index.contains(1, 2));
        assertFalse(index.contains(4, 4));
    }
}
