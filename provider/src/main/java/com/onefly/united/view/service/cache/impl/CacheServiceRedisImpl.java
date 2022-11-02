package com.onefly.united.view.service.cache.impl;


import com.google.common.collect.Lists;
import com.onefly.united.view.service.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.List;
import java.util.Map;

/**
 * @auther: chenjh
 * @time: 2019/4/2 18:02
 * @description
 */
@Slf4j
public class CacheServiceRedisImpl implements CacheService {

    private final RedissonClient redissonClient;

    public CacheServiceRedisImpl(Config config) {
        this.redissonClient = Redisson.create(config);
    }

    @Override
    public void putPDFCache(String key, String value) {
        RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY);
        convertedList.fastPut(key, value);
    }


    @Override
    public Map<String, String> getPDFCache() {
        return redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY);
    }

    @Override
    public String getPDFCache(String key) {
        RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY);
        return convertedList.get(key);
    }

    @Override
    public Integer getPdfImageCache(String key) {
        RMapCache<String, Integer> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_IMGS_KEY);
        return convertedList.get(key);
    }

    @Override
    public Map<String, Integer> getPdfImageCache() {
        return redissonClient.getMapCache(FILE_PREVIEW_PDF_IMGS_KEY);
    }

    @Override
    public void putPdfImageCache(String pdfFilePath, int num) {
        RMapCache<String, Integer> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_IMGS_KEY);
        convertedList.fastPut(pdfFilePath, num);
    }

    @Override
    public void putMd5Cache(String key, String value) {
        RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PDF_MD5_KEY);
        convertedList.fastPut(key, value);
    }

    @Override
    public String getMd5Cache(String key) {
        RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PDF_MD5_KEY);
        return convertedList.get(key);
    }

    @Override
    public Map<String, String> getMd5Cache() {
        return redissonClient.getMapCache(FILE_PDF_MD5_KEY);
    }


    @Override
    public void cleanPDFCache(String key) {
        RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY);
        convertedList.remove(key);
    }

    @Override
    public void cleanPDFImageCache(String key) {
        RMapCache<String, Integer> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_IMGS_KEY);
        convertedList.remove(key);
    }

    @Override
    public void cleanMd5Cache(String key) {
        RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PDF_MD5_KEY);
        convertedList.remove(key);
    }

    @Override
    public List<String> cleanAllLikeValue(String value) {
        List<String> allKey = Lists.newArrayList();
        RMapCache<String, String> pdf = redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY);
        List<String> pdfKey = Lists.newArrayList();
        pdf.entrySet().stream().forEach(entry -> {
            String likeValue = entry.getValue();
            if (likeValue.indexOf(value) >= 0) {
                pdfKey.add(entry.getKey());
                allKey.add(entry.getKey());
            }
        });

        RMapCache<String, Integer> img = redissonClient.getMapCache(FILE_PREVIEW_PDF_IMGS_KEY);
        List<String> imgKey = Lists.newArrayList();
        img.entrySet().stream().forEach(entry -> {
            String likeValue = entry.getKey();
            if (likeValue.indexOf(value) >= 0) {
                imgKey.add(entry.getKey());
                allKey.add(entry.getKey());
            }
        });
        if (pdfKey != null && pdfKey.size() > 0) {
            pdfKey.stream().forEach(key -> pdf.remove(key));
        }
        if (imgKey != null && imgKey.size() > 0) {
            imgKey.stream().forEach(key -> img.remove(key));
        }

        return allKey;
    }

    @Override
    public void addQueueTask(String url) {
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(TASK_QUEUE_NAME);
        queue.addAsync(url);
    }

    @Override
    public String takeQueueTask() throws InterruptedException {
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(TASK_QUEUE_NAME);
        return queue.take();
    }
}
