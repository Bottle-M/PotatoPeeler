import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathTest {
    @Test
    public void mcaRelativePathTest() {
        String mcaPathStr = "world/region/r.0.0.mca";
        String worldPathStr = "world";
        String outputPathStr = "newWorld";
        Path mcaPath = Paths.get(mcaPathStr).toAbsolutePath();
        Path worldPath = Paths.get(worldPathStr).toAbsolutePath();
        Path outputPath = Paths.get(outputPathStr).toAbsolutePath();
        // 计算相对路径
        Path relativePath = worldPath.relativize(mcaPath);
        Path resolvedPath = outputPath.resolve(relativePath);
        System.out.println("MCA 绝对路径: " + mcaPath);
        System.out.println("World 绝对路径: " + worldPath);
        System.out.println("相对路径: " + relativePath);
        System.out.println("输出路径: " + resolvedPath);
    }
}
