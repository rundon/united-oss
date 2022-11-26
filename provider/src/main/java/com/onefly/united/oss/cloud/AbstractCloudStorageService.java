package com.onefly.united.oss.cloud;

import com.onefly.united.common.redis.RedisUtils;
import com.onefly.united.common.redis.lock.IDistributedLockService;
import com.onefly.united.common.utils.DateUtils;
import com.onefly.united.common.utils.SpringContextUtils;
import com.onefly.united.oss.dto.FileUploadResult;
import com.onefly.united.oss.dto.MultipartFileParamDto;
import com.onefly.united.view.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 云存储(支持七牛、阿里云、腾讯云、又拍云)
 *
 * @author rundon
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
     * 初始化
     *
     * @param param
     * @param suffix
     * @return
     */
    public abstract Object startBlock(MultipartFileParamDto param, String suffix);

    /**
     * 中间处理
     *
     * @param param
     * @param suffix
     */
    public abstract void processingBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj);

    /**
     * 结束扫尾
     *
     * @param param
     * @param suffix
     * @return
     */
    public abstract String endBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj);

    /**
     * 加锁版分块上传
     *
     * @param param
     * @param suffix
     * @return
     */
    public String syncUploadBlock(MultipartFileParamDto param, String suffix) {
        String url = null;
        boolean isOk = true;
        log.info("加锁" + param.getMd5() + ",chunk:" + param.getChunk());
        try {
            distributedLockService.lock(param.getMd5(), 10, TimeUnit.MINUTES);
            FileUploadResult processingObj = (FileUploadResult) redisUtils.hGet(Constants.ASYNC_UPLOADER, param.getMd5());
            if (processingObj == null) {
                processingObj = new FileUploadResult();
                processingObj.setFileId(param.getMd5());
                processingObj.setChunks(param.getChunks());
                processingObj.setStore(startBlock(param, suffix));//初始化
                redisUtils.hSet(Constants.ASYNC_UPLOADER, param.getMd5(), processingObj);
            }
            processingBlock(param, suffix, processingObj);///正在处理
            redisUtils.hSet(Constants.ASYNC_UPLOADER, param.getMd5(), processingObj);
            redisUtils.hSet(Constants.ASYNC_UPLOADER_CHUNK, param.getMd5() + "@" + param.getChunk(), true);
            for (int i = 1; i <= param.getChunks(); i++) {
                if (!redisUtils.hHasKey(Constants.ASYNC_UPLOADER_CHUNK, param.getMd5() + "@" + i)) {
                    log.info("第" + i + "块未上传。");
                    isOk = false;
                    break;
                }
            }
            if (isOk) {
                url = endBlock(param, suffix, processingObj);//结束擦屁股操作
                redisUtils.hSet(Constants.ASYNC_UPLOADER, param.getMd5(), processingObj);
                for (int i = 1; i <= param.getChunks(); i++) {
                    redisUtils.hDel(Constants.ASYNC_UPLOADER_CHUNK, param.getMd5() + "@" + i);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            distributedLockService.unlock(param.getMd5());
            log.info("释放锁" + param.getMd5() + ",chunk:" + param.getChunk());
        }
        return url;
    }
}
