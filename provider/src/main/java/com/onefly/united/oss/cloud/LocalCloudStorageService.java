/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.onefly.united.oss.cloud;

import com.onefly.united.common.exception.ErrorCode;
import com.onefly.united.common.exception.RenException;
import com.onefly.united.common.redis.RedisUtils;
import com.onefly.united.oss.dto.FileUploadResult;
import com.onefly.united.oss.dto.MultipartFileParamDto;
import com.onefly.united.view.utils.Constants;
import com.onefly.united.view.utils.FileMD5Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 本地上传
 *
 * @author Mark sunlightcs@gmail.com
 */
@Slf4j
public class LocalCloudStorageService extends AbstractCloudStorageService {

    public LocalCloudStorageService(CloudStorageConfig config, RedisUtils redisUtils) {
        this.config = config;
        this.redisUtils = redisUtils;
    }

    @Override
    public String upload(byte[] data, String path) {
        return upload(new ByteArrayInputStream(data), path);
    }

    @Override
    public String upload(InputStream inputStream, String path) {
        File file = new File(config.getLocalPath() + File.separator + path);
        try {
            FileUtils.copyToFile(inputStream, file);

        } catch (IOException e) {
            throw new RenException(ErrorCode.OSS_UPLOAD_FILE_ERROR, e, "");
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return config.getLocalDomain() + "/" + path;
    }

    @Override
    public String uploadSuffix(byte[] data, String suffix) {
        return upload(data, getPath(config.getLocalPrefix(), suffix));
    }

    @Override
    public String uploadSuffix(InputStream inputStream, String suffix) {
        return upload(inputStream, getPath(config.getLocalPrefix(), suffix));
    }

    @Override
    public String uploadBlock(MultipartFileParamDto param, String suffix) {
        log.info(param.getChunk() + ":开始上传");
        String url = null;
        String temp = config.getLocalPath() + File.separatorChar + "ChunkData" + File.separatorChar;
        String fileName = param.getName();
        String uploadDirPath = temp + param.getMd5();
        String tempFileName = fileName + "_tmp";
        File tmpDir = new File(uploadDirPath);
        File tmpFile = new File(uploadDirPath, tempFileName);
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        FileChannel fileChannel = null;
        try {
            RandomAccessFile tempRaf = new RandomAccessFile(tmpFile, "rw");
            fileChannel = tempRaf.getChannel();

            //写入该分片数据
            long offset = CHUNK_SIZE * (param.getChunk() - 1);
            byte[] fileData = param.getFile().getBytes();
            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, offset, fileData.length);
            mappedByteBuffer.put(fileData);
            // 释放
            FileMD5Util.freedMappedByteBuffer(mappedByteBuffer);
            fileChannel.close();
            boolean isOk = checkAndSetUploadProgress(param);
            if (isOk) {
                boolean flag = renameFile(tmpFile, fileName);
                if (flag) {
                    File targe = new File(tmpFile.getParent() + File.separatorChar + fileName);
                    FileInputStream fileIo = new FileInputStream(targe);
                    url = uploadSuffix(fileIo, suffix);
                    log.warn("upload complete !!" + url + " name=" + fileName + "清空临时文件：" + uploadDirPath);
                    delDir(tmpDir);
                } else {
                    log.error("重命名失败了");
                }
            }
        } catch (IOException e) {
            log.error("错误：" + e.getMessage());
        } finally {
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (IOException e) {
                    log.error("错误：" + e.getMessage());
                }
            }
        }
        return url;
    }

    /**
     * 检查并修改文件上传进度
     *
     * @param param
     * @return
     * @throws IOException
     */
    private boolean checkAndSetUploadProgress(MultipartFileParamDto param) throws IOException {
        boolean isOk = true;
        FileUploadResult processingObj = (FileUploadResult) redisUtils.hGet(Constants.ASYNC_UPLOADER, param.getMd5());
        if (processingObj == null) {
            processingObj = new FileUploadResult();
            processingObj.setFileId(param.getMd5());
            processingObj.setChunks(param.getChunks());
            redisUtils.hSet(Constants.ASYNC_UPLOADER, param.getMd5(), processingObj);
        }
        if (!redisUtils.hHasKey(Constants.ASYNC_UPLOADER_CHUNK, param.getMd5() + "@" + param.getChunk())) {
            redisUtils.hSet(Constants.ASYNC_UPLOADER_CHUNK, param.getMd5() + "@" + param.getChunk(), true);
        }
        for (int i = 1; i <= param.getChunks(); i++) {
            if (!redisUtils.hHasKey(Constants.ASYNC_UPLOADER_CHUNK, param.getMd5() + "@" + i)) {
                log.info("第" + i + "块未上传。");
                isOk = false;
                break;
            }
        }
        if (isOk) {
            processingObj.setStatus(true);
            redisUtils.hSet(Constants.ASYNC_UPLOADER, param.getMd5(), processingObj);
            for (int i = 1; i <= param.getChunks(); i++) {
                redisUtils.hDel(Constants.ASYNC_UPLOADER_CHUNK, param.getMd5() + "@" + i);
            }
        }
        return isOk;
    }


    /**
     * 删除临时文件
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

    /**
     * 文件重命名
     *
     * @param toBeRenamed   将要修改名字的文件
     * @param toFileNewName 新的名字
     * @return
     */
    private boolean renameFile(File toBeRenamed, String toFileNewName) {
        //检查要重命名的文件是否存在，是否是文件
        if (!toBeRenamed.exists() || toBeRenamed.isDirectory()) {
            log.info("File does not exist: " + toBeRenamed.getName());
            return false;
        }
        String p = toBeRenamed.getParent();
        File newFile = new File(p + File.separatorChar + toFileNewName);
        //修改文件名
        return toBeRenamed.renameTo(newFile);
    }
}
