package com.onefly.united.oss.down;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.CRC32;

/**
 * <p>
 * 多线程下载
 * 断点续传下载 demo
 *
 * @author niujinpeng
 * @Date 2020/7/15 15:14
 */
@Slf4j
public class DownloadMain {

    // 下载线程数量
    public static int DOWNLOAD_THREAD_NUM = 3;
    // 下载线程池
    private static ExecutorService executor = Executors.newFixedThreadPool(DOWNLOAD_THREAD_NUM + 1);
    // 临时文件后缀
    public static String FILE_TEMP_SUFFIX = ".temp";

    // 支持的 URL 协议
    private static HashSet<String> PROTOCAL_SET = new HashSet();

    static {
        PROTOCAL_SET.add("thunder://");
        PROTOCAL_SET.add("http://");
        PROTOCAL_SET.add("https://");
    }

    public static void main(String[] args) throws Exception {
        //log.DEBUG = true;
//        if (args == null || args.length == 0 || args[0].trim().length() == 0) {
//            log.info("没有传入任何下载链接");
//            log.info("支持 http/https/thunder 链接");
//            return;
//        }
        final String url = "http://192.168.16.212:8282/resources/20210730/318d17e102fe47deb66cbf10f1ea3cfa.doc";
        long count = PROTOCAL_SET.stream().filter(prefix -> url.startsWith(prefix)).count();
        if (count == 0) {
            log.info("不支持的协议类型");
            return;
        }
        log.info("要下载的链接是:{}", url);
        new DownloadMain().download(url);
    }

    public void download(String url) throws Exception {
        String fileName = HttpUtls.getHttpFileName(url);
        long localFileSize = FileUtils.getFileContentLength(fileName);
        // 获取网络文件具体大小
        long httpFileContentLength = HttpUtls.getHttpFileContentLength(url);
        if (localFileSize >= httpFileContentLength) {
            log.info("{}已经下载完毕，无需重新下载", fileName);
            return;
        }
        List<Future<Boolean>> futureList = new ArrayList<>();
        if (localFileSize > 0) {
            log.info("开始断点续传 {}", fileName);
        } else {
            log.info("开始下载文件 {}", fileName);
        }
        log.info("开始下载时间 {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));
        long startTime = System.currentTimeMillis();
        // 任务切分
        splitDownload(url, futureList);
        LogThread logThread = new LogThread(httpFileContentLength);
        Future<Boolean> future = executor.submit(logThread);
        futureList.add(future);
        // 开始下载
        for (Future<Boolean> booleanFuture : futureList) {
            booleanFuture.get();
        }
        log.info("文件下载完毕 {}，本次下载耗时：{}", fileName, (System.currentTimeMillis() - startTime) / 1000 + "s");
        log.info("结束下载时间 {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));
        // 文件合并
        boolean merge = merge(fileName);
        if (merge) {
            // 清理分段文件
            clearTemp(fileName);
        }
        log.info("本次文件下载结束");
        //System.exit(0);
    }

    /**
     * 切分下载任务到多个线程
     *
     * @param url
     * @param futureList
     * @throws IOException
     */
    public void splitDownload(String url, List<Future<Boolean>> futureList) throws IOException {
        long httpFileContentLength = HttpUtls.getHttpFileContentLength(url);
        // 任务切分
        long size = httpFileContentLength / DOWNLOAD_THREAD_NUM;
        long lastSize = httpFileContentLength - (httpFileContentLength / DOWNLOAD_THREAD_NUM * (DOWNLOAD_THREAD_NUM - 1));
        for (int i = 0; i < DOWNLOAD_THREAD_NUM; i++) {
            long start = i * size;
            Long downloadWindow = (i == DOWNLOAD_THREAD_NUM - 1) ? lastSize : size;
            Long end = start + downloadWindow;
            if (start != 0) {
                start++;
            }
            DownloadThread downloadThread = new DownloadThread(url, start, end, i, httpFileContentLength);
            Future<Boolean> future = executor.submit(downloadThread);
            futureList.add(future);
        }
    }

    public boolean merge(String fileName) throws IOException {
        log.info("开始合并文件 {}", fileName);
        byte[] buffer = new byte[1024 * 10];
        int len = -1;
        try (RandomAccessFile oSavedFile = new RandomAccessFile(fileName, "rw")) {
            for (int i = 0; i < DOWNLOAD_THREAD_NUM; i++) {
                try (BufferedInputStream bis = new BufferedInputStream(
                        new FileInputStream(fileName + FILE_TEMP_SUFFIX + i))) {
                    while ((len = bis.read(buffer)) != -1) { // 读到文件末尾则返回-1
                        oSavedFile.write(buffer, 0, len);
                    }
                }
            }
            log.info("文件合并完毕 {}", fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean clearTemp(String fileName) {
        log.info("开始清理临时文件 {}{}0-{}", fileName, FILE_TEMP_SUFFIX, (DOWNLOAD_THREAD_NUM - 1));
        for (int i = 0; i < DOWNLOAD_THREAD_NUM; i++) {
            File file = new File(fileName + FILE_TEMP_SUFFIX + i);
            file.delete();
        }
        log.info("临时文件清理完毕 {}{}0-{}", fileName, FILE_TEMP_SUFFIX, (DOWNLOAD_THREAD_NUM - 1));
        return true;
    }

    /**
     * 使用CheckedInputStream计算CRC
     */
    public static Long getCRC32(String filepath) throws IOException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(filepath));
        CRC32 crc = new CRC32();
        byte[] bytes = new byte[1024];
        int cnt;
        while ((cnt = inputStream.read(bytes)) != -1) {
            crc.update(bytes, 0, cnt);
        }
        inputStream.close();
        return crc.getValue();
    }

}
