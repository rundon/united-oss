/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.onefly.united.oss.cloud;

import com.onefly.united.common.redis.RedisUtils;
import com.onefly.united.common.redis.lock.IDistributedLockService;
import com.onefly.united.common.utils.DateUtils;
import com.onefly.united.common.utils.SpringContextUtils;
import com.onefly.united.oss.dto.MultipartFileParamDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 云存储(支持七牛、阿里云、腾讯云、又拍云)
 *
 * @author Mark sunlightcs@gmail.com
 */
@Slf4j
public abstract class AbstractCloudStorageService {

    public static long CHUNK_SIZE = 10485760;

    /**
     * 云存储配置信息
     */
    CloudStorageConfig config;

    /**
     * 保存上传信息
     */
    public RedisUtils redisUtils = SpringContextUtils.getBean(RedisUtils.class);
    /**
     * 分布式锁
     */
    private IDistributedLockService distributedLockService = SpringContextUtils.getBean(IDistributedLockService.class);

    /**
     * 文件路径
     *
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 返回上传路径
     */
    public String getPath(String prefix, String suffix) {
        //生成uuid
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        //文件路径
        String path = DateUtils.format(new Date(), "yyyyMMdd") + "/" + uuid;

        if (StringUtils.isNotBlank(prefix)) {
            path = prefix + "/" + path;
        }

        return path + "." + suffix;
    }

    /**
     * 文件上传
     *
     * @param data 文件字节数组
     * @param path 文件路径，包含文件名
     * @return 返回http地址
     */
    public abstract String upload(byte[] data, String path);

    /**
     * 文件上传
     *
     * @param data   文件字节数组
     * @param suffix 后缀
     * @return 返回http地址
     */
    public abstract String uploadSuffix(byte[] data, String suffix);

    /**
     * 文件上传
     *
     * @param inputStream 字节流
     * @param path        文件路径，包含文件名
     * @return 返回http地址
     */
    public abstract String upload(InputStream inputStream, String path);

    /**
     * 文件上传
     *
     * @param inputStream 字节流
     * @param suffix      后缀
     * @return 返回http地址
     */
    public abstract String uploadSuffix(InputStream inputStream, String suffix);

    /**
     * 分块上传
     *
     * @param param
     * @param suffix
     * @return
     */
    public abstract String uploadBlock(MultipartFileParamDto param, String suffix);

    /**
     * 加锁版分块上传
     *
     * @param param
     * @param suffix
     * @return
     */
    public String syncUploadBlock(MultipartFileParamDto param, String suffix) {
        String url = null;
        log.warn("加锁" + param.getMd5() + ",chunk:" + param.getChunk());
        try {
            distributedLockService.lock("sync:" + param.getMd5(), 10, TimeUnit.MINUTES);
            url = uploadBlock(param, suffix);
        } finally {
            distributedLockService.unlock("sync:" + param.getMd5());
            log.warn("释放锁" + param.getMd5() + ",chunk:" + param.getChunk());
        }
        return url;
    }
}
