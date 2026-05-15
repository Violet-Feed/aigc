package violet.aigc.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FFmpegUtil {

    // 如果你希望可配置，可以改成从配置文件或 env 读取
    private static final String FFMPEG_PATH = "ffmpeg";

    /**
     * 从远程视频 URL 抓取首帧，返回 PNG 图像字节数组
     */
    public static byte[] fetchFirstFrame(String videoUrl) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                FFMPEG_PATH,
                "-ss", "00:00:01",   // 从第 1 秒附近取一帧，时间点可根据需要调整
                "-i", videoUrl,
                "-frames:v", "1",    // 只解码一帧
                "-f", "image2",
                "-vcodec", "png",    // 输出 PNG
                "pipe:1"             // 输出到 stdout
        );

        // 不要合并 stderr 到 stdout，否则可能污染 PNG 字节流
        pb.redirectErrorStream(false);

        Process process = pb.start();

        try (InputStream is = process.getInputStream();
             InputStream es = process.getErrorStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ByteArrayOutputStream errBaos = new ByteArrayOutputStream()) {

            Thread errThread = new Thread(() -> {
                try {
                    byte[] errBuffer = new byte[8192];
                    int errLen;
                    while ((errLen = es.read(errBuffer)) != -1) {
                        errBaos.write(errBuffer, 0, errLen);
                    }
                } catch (IOException ignored) {
                }
            });

            errThread.start();

            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }

            int exitCode = process.waitFor();
            errThread.join();

            String errMsg = errBaos.toString("UTF-8");

            if (exitCode != 0) {
                throw new IOException("ffmpeg exit code = " + exitCode + ", stderr = " + errMsg);
            }

            return baos.toByteArray();
        } finally {
            // 防止进程泄露
            process.destroy();
        }
    }
}