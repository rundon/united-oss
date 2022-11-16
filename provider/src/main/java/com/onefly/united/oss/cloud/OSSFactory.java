/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.onefly.united.oss.cloud;

import com.onefly.united.common.constant.Constant;
import com.onefly.united.common.redis.RedisUtils;
import com.onefly.united.common.utils.SpringContextUtils;
import com.onefly.united.oauth2.service.SysParamsService;

/**
 * 文件上传Factory
 *
 * @author Mark sunlightcs@gmail.com
 */
public final class OSSFactory {
    private static SysParamsService sysParamsService;
    private static RedisUtils redisUtils;

    static {
        OSSFactory.sysParamsService = SpringContextUtils.getBean(SysParamsService.class);
        OSSFactory.redisUtils = SpringContextUtils.getBean(RedisUtils.class);
    }

    public static AbstractCloudStorageService build() {
        //获取云存储配置信息
        CloudStorageConfig config = sysParamsService.getValueObject(Constant.CLOUD_STORAGE_CONFIG_KEY, CloudStorageConfig.class);

        if (config.getType() == Constant.CloudService.QINIU.getValue()) {
            return new QiniuCloudStorageService(config, redisUtils);
        } else if (config.getType() == Constant.CloudService.ALIYUN.getValue()) {
            return new AliyunCloudStorageService(config, redisUtils);
        } else if (config.getType() == Constant.CloudService.QCLOUD.getValue()) {
            return new QcloudCloudStorageService(config, redisUtils);
        } else if (config.getType() == Constant.CloudService.FASTDFS.getValue()) {
            return new FastDFSCloudStorageService(config, redisUtils);
        } else if (config.getType() == Constant.CloudService.LOCAL.getValue()) {
            return new LocalCloudStorageService(config, redisUtils);
        } else if (config.getType() == Constant.CloudService.MINIO.getValue()) {
            return new MinioCloudStorageService(config, redisUtils);
        }

        return null;
    }

}