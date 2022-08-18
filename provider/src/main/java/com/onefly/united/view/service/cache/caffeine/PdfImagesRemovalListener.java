package com.onefly.united.view.service.cache.caffeine;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.service.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;

/**
 * pdf图片过期移除监听
 */
@Slf4j
public class PdfImagesRemovalListener implements RemovalListener<String, Integer> {

    private CacheService cacheService;

    private KkViewProperties kkViewProperties;

    public PdfImagesRemovalListener(CacheService cacheService, KkViewProperties kkViewProperties) {
        this.cacheService = cacheService;
        this.kkViewProperties = kkViewProperties;
    }

    @Override
    public void onRemoval(@Nullable String key, @Nullable Integer value, @org.checkerframework.checker.nullness.qual.NonNull RemovalCause removalCause) {
        String path = kkViewProperties.getFileSaveDir() + key;
        String dir = path.substring(0, path.lastIndexOf("."));
        log.info("解析的目录：" + dir);
        File file = new File(dir);
        if (file.exists()) {
            delDir(file);
        }
        cacheService.cleanPDFImageCache(key);
        log.info("删除缓存key=" + key + ",value=" + value);
    }

    /**
     * 递归删除
     *
     * @param f
     */
    private void delDir(File f) {
        if (f.isDirectory()) {
            File[] subFiles = f.listFiles();
            for (File subFile : subFiles) {
                delDir(subFile);
            }
        }
        f.delete();
    }
}
