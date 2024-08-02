import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class DecompressTest {
    @Test
    public void zlibDecompressTest() {
        String inputFile = "C:\\Users\\58379\\Desktop\\chunk.0.0.bin";
        String outputFile = "C:\\Users\\58379\\Desktop\\export_decompressed.bin";
        byte[] buffer = new byte[1024];
        byte[] decompressedBuffer = new byte[1024];

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            Inflater inflater = new Inflater();
            int bytesRead;

            // 循环读取压缩数据块
            while ((bytesRead = fis.read(buffer)) != -1) {
                inflater.setInput(buffer, 0, bytesRead);

                // 循环解压数据块
                while (!inflater.finished() && !inflater.needsInput()) {
                    try {
                        int count = inflater.inflate(decompressedBuffer);
                        fos.write(decompressedBuffer, 0, count);
                    } catch (DataFormatException e) {
                        e.printStackTrace();
                    }
                }
            }

            inflater.end();
            System.out.println("解压完成，输出文件: " + outputFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
