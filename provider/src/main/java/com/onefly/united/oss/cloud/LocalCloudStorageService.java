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
import com.onefly.united.oss.dto.FileUploadResult;
import com.onefly.united.oss.dto.MultipartFileParamDto;
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
    /**
     * 临时的文件存储luj
     */
    private String temp;

    public LocalCloudStorageService(CloudStorageConfig config) {
        temp = config.getLocalPath() + File.separatorChar + "ChunkData" + File.separatorChar;
        this.config = config;
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
    public Object startBlock(MultipartFileParamDto param, String suffix) {

        log.info("初始化 本地无需存储信息");
        return null;
    }

    @Override
    public void processingBlock(MultipartFileParamDto param, String suffix,FileUploadResult processingObj) {
        log.info(param.getChunk() + ":开始上传");
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
        } catch (IOException e) {
            log.error("错误：" + e.getMessage());
            throw new RenException("本地上传失败" + e.getMessage());
        } finally {
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (IOException e) {
                    log.error("错误：" + e.getMessage());
                }
            }
        }
        log.info(param.getChunk() + ":结束上传");
    }

    @Override
    public String endBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj) {
        String url = null;
        String fileName = param.getName();
        String uploadDirPath = temp + param.getMd5();
        String tempFileName = fileName + "_tmp";
        File tmpDir = new File(uploadDirPath);
        File tmpFile = new File(uploadDirPath, tempFileName);
        try {
            boolean flag = renameFile(tmpFile, fileName);
            if (flag) {
                processingObj.setStatus(true);
                File targe = new File(tmpFile.getParent() + File.separatorChar + fileName);
                FileInputStream fileIo = new FileInputStream(targe);
                url = uploadSuffix(fileIo, suffix);
                log.info("upload complete !!" + url + " name=" + fileName + "清空临时文件：" + uploadDirPath);
                delDir(tmpDir);
            } else {
                log.error("重命名失败了");
            }
        } catch (Exception e) {
            log.error("本地改名失败：" + e.getMessage());
            throw new RenException("本地改名失败" + e.getMessage());
        }
        return url;
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
