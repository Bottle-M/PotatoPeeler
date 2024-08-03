package indi.somebottle.streams;

// 创建实例（静态工厂方法）

import indi.somebottle.exceptions.CompressionTypeUnsupportedException;
import net.jpountz.lz4.LZ4BlockInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class InputStreamFactory {
    /**
     * 根据压缩类型获取区块数据读取器实例
     *
     * @param compressionType 压缩类型
     * @param rais            RandomAccessInputStream 输入流
     * @return InputStream 实例
     * @throws CompressionTypeUnsupportedException 压缩类型不支持
     * @throws IOException                         IO 异常
     * @apiNote 请记得关闭流
     */
    public static InputStream getStream(int compressionType, RandomAccessInputStream rais) throws IOException, CompressionTypeUnsupportedException {
        switch (compressionType) {
            case 1:
                // GZip
                return new GZIPInputStream(rais);
            case 2:
                // Zlib
                return new InflaterInputStream(rais);
            case 3:
                // Uncompressed
                return rais;
            case 4:
                // LZ4
                return new LZ4BlockInputStream(rais);
        }
        // 其余情况不支持
        throw new CompressionTypeUnsupportedException("Compression type: " + compressionType + " unsupported.");
    }
}
