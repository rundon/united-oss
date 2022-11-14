package com.onefly.united.view.service.cache.caffeine;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.dto.CacheMdPdf;
import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.model.StorageType;
import com.onefly.united.view.service.cache.CacheService;
import com.onefly.united.view.utils.FileMD5Util;
import com.onefly.united.view.utils.FileUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * caffeine第一级缓存
 */
@Slf4j
@Component
public class CaffeineCache {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private KkViewProperties kkViewProperties;

    @Autowired
    private FileUtils fileUtils;

    /**
     * 转换后pdf缓存路径
     */
    public static LoadingCache<String, String> pdfCache;
    /**
     * pdf 转图片的数量
     */
    public static LoadingCache<String, Integer> pdfImagesCache;
    /**
     * 只缓存http的文件(zip情况太复杂)的MD5，如果已经转换则返回历史
     */
    public static LoadingCache<String, String> pdfMD5Cache;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        if (kkViewProperties.getCache().isClean()) {
            pdfCache = Caffeine.newBuilder()
                    .expireAfterAccess(1, TimeUnit.DAYS)
                    .removalListener(new PdfCacheRemovalListener(cacheService, kkViewProperties, fileUtils))
                    .maximumSize(10_000)
                    .build(new CacheLoader<String, String>() {
                        @Nullable
                        @Override
                        public String load(@NonNull String key) {
                            return cacheService.getPDFCache(key);
                        }
                    });
        } else {
            pdfCache = Caffeine.newBuilder()
                    .build(new CacheLoader<String, String>() {
                        @Nullable
                        @Override
                        public String load(@NonNull String key) {
                            return cacheService.getPDFCache(key);
                        }
                    });
        }
        pdfCache.putAll(cacheService.getPDFCache());
        pdfImagesCache = Caffeine.newBuilder()
                .removalListener(new PdfImagesRemovalListener(cacheService, kkViewProperties))
                .build(new CacheLoader<String, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull String key) {
                        return cacheService.getPdfImageCache(key);
                    }
                });
        pdfImagesCache.putAll(cacheService.getPdfImageCache());
        //md5
        pdfMD5Cache = Caffeine.newBuilder()
                .build(new CacheLoader<String, String>() {
                    @Nullable
                    @Override
                    public String load(@NonNull String key) {
                        return cacheService.getMd5Cache(key);
                    }
                });
        pdfMD5Cache.putAll(cacheService.getMd5Cache());
        cacheService.cleanRunCache();
    }

    /**
     * @return 是否已转换过的文件(缓存)
     */
    public boolean containConvertedFiles(String key) {
        String value = this.getConvertedFile(key);
        return StringUtils.isNotBlank(value);
    }

    /**
     * @return 已转换过的文件，根据文件名获取
     */
    public String getConvertedFile(String key) {
        return pdfCache.get(key);
    }

    /**
     * @param key pdf本地路径
     * @return 已将pdf转换成图片的图片本地相对路径
     */
    public Integer getConvertedPdfImage(String key) {
        return pdfImagesCache.get(key);
    }

    /**
     * 添加转换后PDF缓存
     *
     * @param fileName pdf文件名
     * @param value    缓存相对路径
     */
    public synchronized void addConvertedFile(String fileName, String value, String md5) {
        cacheService.putPDFCache(fileName, value);
        pdfCache.put(fileName, value);
        if (StringUtils.isNotBlank(md5)) {
            pdfMD5Cache.put(md5, fileName);
            cacheService.putMd5Cache(md5, fileName);
        }
    }

    /**
     * 异常之后删除
     *
     * @param fileName
     * @param md5
     */
    public void deleteConvertedFile(String fileName, String md5) {
        cacheService.cleanPDFCache(fileName);
        pdfCache.invalidate(fileName);
        if (StringUtils.isNotBlank(md5)) {
            pdfMD5Cache.invalidate(md5);
            cacheService.cleanMd5Cache(md5);
        }
    }

    /**
     * 删除正在运行的
     * @param md5
     */
    public void deleteRunning(String md5) {
        cacheService.cleanRunCache(md5);
    }

    /**
     * 删除类似的值
     *
     * @param value
     */
    public void cleanAllLikeValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            List<String> allKey = cacheService.cleanAllLikeValue(value);
            allKey.stream().forEach(key -> {
                System.out.println("删除：" + key);
                pdfCache.invalidate(key);
                pdfImagesCache.invalidate(key);
            });
        }
    }

    /**
     * 清楚pdftup
     *
     * @param key
     */
    public void deleteConvertedPdfImage(String key) {
        cacheService.cleanPDFImageCache(key);
        pdfImagesCache.invalidate(key);
    }

    /**
     * 添加转换后图片组缓存
     *
     * @param pdfFilePath pdf文件绝对路径
     * @param num         图片张数
     */
    public void addConvertedPdfImage(String pdfFilePath, int num) {
        cacheService.putPdfImageCache(pdfFilePath, num);
        pdfImagesCache.put(pdfFilePath, num);
    }

    /**
     * 查询md5 里面的pdf名
     *
     * @param fileAttribute
     * @return
     * @throws Exception
     */
    public CacheMdPdf loadPdfName(FileAttribute fileAttribute) throws Exception {
        CacheMdPdf cacheMdPdf = new CacheMdPdf();
        long old = System.currentTimeMillis();
        String md5 = FileMD5Util.getFileMD5(fileAttribute.getUrl());
        long now = System.currentTimeMillis();
        String suffix = fileAttribute.getSuffix();
        boolean isHtml = suffix.equalsIgnoreCase("xls") || suffix.equalsIgnoreCase("xlsx");
        String pdfName = md5 + "." + (isHtml ? "html" : "pdf");
        log.info("文件类型" + fileAttribute.getStorageType());

        log.info("计算MD5成功，共耗时：" + ((now - old) / 1000.0) + "秒");
        if (StorageType.URL == fileAttribute.getStorageType()) {//只有url的才需要获取md5
            if (!containConvertedFiles(pdfName)) {
                if (!cacheService.checkRunCache(md5)) {
                    if (StringUtils.isNotBlank(pdfMD5Cache.get(md5))) {
                        String md5Cache = pdfMD5Cache.get(md5);
                        if (containConvertedFiles(md5Cache)) {
                            addConvertedFile(pdfName, getConvertedFile(md5Cache), md5);
                            pdfName = md5Cache;
                            log.info(md5 + "文件已经存在,返回:" + pdfName);
                        } else {
                            cacheService.putRunCache(md5, pdfName);
                            pdfMD5Cache.invalidate(md5);
                            cacheService.cleanMd5Cache(md5);
                            log.info(md5 + "文件虚假存在，删除!,重新转换");
                        }
                    } else {
                        if (!containConvertedFiles(pdfName)) {
                            cacheService.putRunCache(md5, pdfName);
                            log.info(md5 + "文件不存在,重新转换");
                        }
                    }
                } else {
                    cacheMdPdf.setDoing(true);
                    log.info(md5 + "文件正在转换");
                }
                cacheMdPdf.setMd5(md5);
            } else {
                if (cacheService.checkRunCache(md5)) {
                    cacheMdPdf.setDoing(true);
                    log.info(md5 + "文件正在转换");
                } else {
                    log.info("{} 文件已经转换.", pdfName);
                }
                log.info("{} 文件已经转换.", pdfName);
                cacheMdPdf.setMd5(md5);
            }
        }
        //log.info("正在转换的任务还有:{} 个.");
        cacheMdPdf.setPdfName(pdfName);
        return cacheMdPdf;
    }

}
