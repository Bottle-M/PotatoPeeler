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
        // 默认配置下 maxChildren=4, minChildren=round(4*0.4)=2
        RTree<Boolean, Geometry> tree = RTree.star().create();
        for (int i = 0; i < 100; i++) {
            // 随机生成 100 个矩形，模拟 100 个不同大小的受保护区块区域
            // 注意 x2 必须大于 x1, y2 必须大于 y1
            double x1 = rd.nextInt(), y1 = rd.nextInt(), x2 = x1 + (double) rd.nextInt(3000000), y2 = y1 + (double) rd.nextInt(3000000);
            tree = tree.add(true, Geometries.rectangle(x1, y1, x2, y2));
        }
        int hitNum = 0, missNum = 0;
        long startTime = System.currentTimeMillis();
        // 1024 * 1000 次查询
        for (int i = 0; i < 1024 * 1000; i++) {
            Iterable<Entry<Boolean, Geometry>> results = tree.search(Geometries.point((double) rd.nextInt(), rd.nextInt()));
            if (results.iterator().hasNext()) {
                hitNum++;
            } else {
                missNum++;
            }
        }
        long timeElapsed = System.currentTimeMillis() - startTime;
        System.out.println("Time elapsed: " + timeElapsed + " ms");
        System.out.println("Hit: " + hitNum + ", miss: " + missNum);
        // 测试结果：1024 * 1000 次查询，即模拟查询 1000 个 mca 文件中的所有区块，耗时 1000 ms 左右，效率很高
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
