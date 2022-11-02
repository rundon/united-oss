package com.onefly.united.view.service.cache;

import java.util.List;
import java.util.Map;

/**
 * 属于二级缓存
 */
public interface CacheService {

    String FILE_PREVIEW_PDF_KEY = "converted-preview-pdf-file";
    String FILE_PREVIEW_PDF_IMGS_KEY = "converted-preview-pdfimgs-file";
    String TASK_QUEUE_NAME = "convert-task";
    String FILE_PDF_MD5_KEY = "converted-pdf-md5";

    void putPDFCache(String key, String value);

    Map<String, String> getPDFCache();

    String getPDFCache(String key);

    Integer getPdfImageCache(String key);

    Map<String, Integer> getPdfImageCache();

    void putPdfImageCache(String pdfFilePath, int num);

    ///文件MD5 pdf名
    void putMd5Cache(String key, String value);

    String getMd5Cache(String key);

    Map<String, String> getMd5Cache();

    /**
     * 清除pdf
     *
     * @param key
     */
    void cleanPDFCache(String key);

    /**
     * 清除pdf 图片
     *
     * @param key
     */
    void cleanPDFImageCache(String key);

    /**
     * 清除md5
     *
     * @param key
     */
    void cleanMd5Cache(String key);

    /**
     * 删除值
     * @param value
     */
    List<String> cleanAllLikeValue(String value);

    void addQueueTask(String url);

    String takeQueueTask() throws InterruptedException;

}
