package indi.somebottle.streams;

// 创建实例（静态工厂方法）

import indi.somebottle.exceptions.CompressionTypeUnsupportedException;
import net.jpountz.lz4.LZ4BlockInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class InputStreamFactory {
    /**
     * 根据压缩类型获取区块数据读取器实例
     *
     * @param compressionType 压缩类型
     * @param bis             字节输入流
     * @return InputStream 实例
     * @apiNote 请记得关闭流
     */
    public static InputStream getStream(int compressionType, ByteArrayInputStream bis) throws IOException, CompressionTypeUnsupportedException {
        switch (compressionType) {
            case 1:
                // GZip
                return new GZIPInputStream(bis);
            case 2:
                // Zlib
                return new InflaterInputStream(bis);
            case 3:
                // Uncompressed
                return bis;
            case 4:
                // LZ4
                return new LZ4BlockInputStream(bis);
        }
        // 其余情况不支持
        throw new CompressionTypeUnsupportedException("Compression type: " + compressionType + " unsupported.");
    }
}
