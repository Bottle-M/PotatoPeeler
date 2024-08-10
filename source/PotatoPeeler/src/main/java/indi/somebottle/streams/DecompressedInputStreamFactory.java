package indi.somebottle.streams;

// 创建实例（静态工厂方法）

import indi.somebottle.exceptions.CompressionTypeUnsupportedException;
import net.jpountz.lz4.LZ4BlockInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * 解压缩流静态工厂
 */
public class DecompressedInputStreamFactory {
    /**
     * 根据压缩类型获取区块数据读取器实例
     *
     * @param compressionType 压缩类型
     * @param is              InputStream 输入流
     * @return InputStream 实例
     * @throws CompressionTypeUnsupportedException 压缩类型不支持
     * @throws IOException                         IO 异常
     * @apiNote 请记得关闭流
     */
    @SuppressWarnings("EnhancedSwitchMigration")
    public static InputStream getStream(int compressionType, InputStream is) throws IOException, CompressionTypeUnsupportedException {
        switch (compressionType) {
            case 1:
                // GZip
                return new GZIPInputStream(is);
            case 2:
                // Zlib
                return new InflaterInputStream(is);
            case 3:
                // Uncompressed
                return is;
            case 4:
                // LZ4
                return new LZ4BlockInputStream(is);
        }
        // 其余情况不支持
        throw new CompressionTypeUnsupportedException("Compression type: " + compressionType + " unsupported.");
    }
}
