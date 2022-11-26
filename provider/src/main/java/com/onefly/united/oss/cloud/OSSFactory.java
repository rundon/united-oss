package com.onefly.united.oss.cloud;

import com.onefly.united.common.constant.Constant;
import com.onefly.united.common.utils.SpringContextUtils;
import com.onefly.united.oauth2.service.SysParamsService;

/**
 * 文件上传Factory
 *
 * @author rundon
 */
public final class OSSFactory {
    private static SysParamsService sysParamsService;

    static {
        OSSFactory.sysParamsService = SpringContextUtils.getBean(SysParamsService.class);
    }

    public static AbstractCloudStorageService build() {
        //获取云存储配置信息
        CloudStorageConfig config = sysParamsService.getValueObject(Constant.CLOUD_STORAGE_CONFIG_KEY, CloudStorageConfig.class);

        if (config.getType() == Constant.CloudService.QINIU.getValue()) {
            return new QiniuCloudStorageService(config);
        } else if (config.getType() == Constant.CloudService.ALIYUN.getValue()) {
            return new AliyunCloudStorageService(config);
        } else if (config.getType() == Constant.CloudService.QCLOUD.getValue()) {
            return new QcloudCloudStorageService(config);
        } else if (config.getType() == Constant.CloudService.FASTDFS.getValue()) {
            return new FastDFSCloudStorageService(config);
        } else if (config.getType() == Constant.CloudService.LOCAL.getValue()) {
            return new LocalCloudStorageService(config);
        } else if (config.getType() == Constant.CloudService.MINIO.getValue()) {
            return new MinioCloudStorageService(config);
        }

        return null;
    }

}