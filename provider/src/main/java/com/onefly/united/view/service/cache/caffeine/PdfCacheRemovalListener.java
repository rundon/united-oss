package com.onefly.united.view.service.cache.caffeine;

import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.common.collect.Lists;
import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.model.FileType;
import com.onefly.united.view.service.cache.CacheService;
import com.onefly.united.view.utils.FileUtils;
import com.onefly.united.view.utils.ZipReader;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * pdf过期监听
 */
@Slf4j
public class PdfCacheRemovalListener implements RemovalListener<String, String> {

    private CacheService cacheService;

    private KkViewProperties kkViewProperties;

    private FileUtils fileUtils;

    public PdfCacheRemovalListener(CacheService cacheService, KkViewProperties kkViewProperties, FileUtils fileUtils) {
        this.cacheService = cacheService;
        this.kkViewProperties = kkViewProperties;
        this.fileUtils = fileUtils;
    }

    @Override
    public void onRemoval(@Nullable String key, @Nullable String value, @NonNull RemovalCause removalCause) {
        FileType fileType = fileUtils.typeFromFileName(key);
        int length = key.split("\\.").length;
        if (CaffeineCache.pdfCache.getIfPresent(key) == null) {
            //压缩包
            if (FileType.compress == fileType && length == 2) {
                ZipReader.FileNode fileNode = JSON.parseObject(value, ZipReader.FileNode.class);
                String fileName = fileNode.getFileName().split("_")[1].split("/")[0];
                log.info("拆出来的:" + fileName);
                String path = kkViewProperties.getFileDir() + fileName;
                List<String> deleteKey = Lists.newArrayList(key);
                deleteKey.addAll(CaffeineCache.pdfCache.asMap().keySet().stream().filter(pdf -> pdf.indexOf(fileName) >= 0).collect(Collectors.toList()));
                if (deleteKey.size() == 1) {
                    deleteKey.addAll(cacheService.getPDFCache().keySet().stream().filter(pdf -> pdf.indexOf(fileName) >= 0).collect(Collectors.toList()));
                    //清除pdf转图片的缓存
                    deleteKey.stream().forEach(delete -> {
                        cacheService.cleanPDFCache(delete);
                        log.info("删除缓存key=" + delete);
                    });
                    CaffeineCache.pdfImagesCache.invalidateAll(CaffeineCache.pdfImagesCache.asMap().keySet().stream().filter(keys -> keys.indexOf(fileName) >= 0).collect(Collectors.toList()));
                    File file = new File(path);
                    if (file.exists()) {
                        delDir(file);
                    } else {
                        log.error("文件不存在？？？" + path);
                    }
                } else {
                    System.out.println("重新添加key：=" + key);
                    CaffeineCache.pdfCache.put(key, value);
                }
            } else if (length == 2) {
                String nameSplit = value.split("/")[1];
                String name = nameSplit.substring(0, nameSplit.lastIndexOf("."));
                File pdf = new File(kkViewProperties.getFileDir());
                if (pdf.exists()) {
                    Lists.newArrayList(pdf.listFiles(new FileNameFilter(name))).stream().forEach(file -> file.delete());
                }
                cacheService.cleanPDFCache(key);
                CaffeineCache.pdfImagesCache.invalidateAll(CaffeineCache.pdfImagesCache.asMap().keySet().stream().filter(keys -> keys.indexOf(name) >= 0).collect(Collectors.toList()));
                log.info("删除缓存key=" + key + ",value=" + value);
            } else {
                log.info("其他情况:key=" + key + ",value=" + value);
            }
        } else {
            log.info("意外情况" + key);
        }


    }

    public class FileNameFilter implements FileFilter {
        private String fileName;

        public FileNameFilter(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public boolean accept(File pathname) {
            if (pathname.isDirectory()) {
                return false;
            }
            String nameFile = pathname.getName().split("\\.")[0];
            String name = fileName.split("\\.")[0];
            return nameFile.equals(name);
        }
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
