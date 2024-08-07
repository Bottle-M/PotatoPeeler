import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Geometry;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ExecutionException;

public class RTreeTest {
    @Test
    public void intensiveTest() throws ExecutionException, InterruptedException {
        Random rd = new Random();
/*        try {
            // 20 秒后启动
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        // R* 树
        RTree<Boolean, Geometry> tree = RTree.star().maxChildren(4).create();
        for (int i = 0; i < 100; i++) {
            // 随机生成 100 个矩形
            // 注意 x2 必须大于 x1, y2 必须大于 y1
            float x1 = (float) rd.nextInt(), y1 = (float) rd.nextInt(), x2 = x1 + (float) rd.nextInt(3000000), y2 = y1 + (float) rd.nextInt(3000000);
            tree = tree.add(true, Geometries.rectangle(x1, y1, x2, y2));
        }
        int hitNum = 0, missNum = 0;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1024 * 1000; i++) {
            Iterable<Entry<Boolean, Geometry>> results = tree.search(Geometries.point((float) rd.nextInt(), (float) rd.nextInt()));
            if (results.iterator().hasNext()) {
                hitNum++;
            } else {
                missNum++;
            }
        }
        long timeElapsed = System.currentTimeMillis() - startTime;
        System.out.println("Time elapsed: " + timeElapsed + " ms");
        System.out.println("Hit: " + hitNum + ", miss: " + missNum);
        /*System.gc();
        try {
            // 触发 GC 后等待 20 秒
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }

    @Test
    public void hitTest() {
        // R* 树
        RTree<Boolean, Geometry> tree = RTree.star().maxChildren(6).create();
        // 形成 L 形，测试边界是否准确
        tree = tree.add(true, Geometries.rectangle(0f, 0f, 3f, 1f))
                .add(true, Geometries.rectangle(2f, 0f, 3f, 4f))
                .add(true, Geometries.point(1f, 2f));
        Iterable<Entry<Boolean, Geometry>> results = tree.search(Geometries.point(2f, 4f));
        if (results.iterator().hasNext()) {
            System.out.println("Hit!");
        } else {
            System.out.println("Miss!");
        }
    }
}
