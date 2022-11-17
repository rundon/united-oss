/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.onefly.united.oss.cloud;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.DefaultAppendFileStorageClient;
import com.google.common.io.ByteSource;
import com.onefly.united.common.exception.ErrorCode;
import com.onefly.united.common.exception.RenException;
import com.onefly.united.common.utils.SpringContextUtils;
import com.onefly.united.oss.dto.FastDFSPath;
import com.onefly.united.oss.dto.FileUploadResult;
import com.onefly.united.oss.dto.MultipartFileParamDto;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * FastDFS
 *
 * @author Mark sunlightcs@gmail.com
 */
@Slf4j
public class FastDFSCloudStorageService extends AbstractCloudStorageService {
    private static DefaultAppendFileStorageClient defaultAppendFileStorageClient;
    public static final String DEFAULT_GROUP = "group1";

    static {
        defaultAppendFileStorageClient = (DefaultAppendFileStorageClient) SpringContextUtils.getBean("defaultAppendFileStorageClient");
    }

    public FastDFSCloudStorageService(CloudStorageConfig config) {
        this.config = config;
    }

    @Override
    public String upload(byte[] data, String path) {
        return upload(new ByteArrayInputStream(data), path);
    }

    @Override
    public String upload(InputStream inputStream, String suffix) {
        StorePath storePath;
        try {
            storePath = defaultAppendFileStorageClient.uploadFile(DEFAULT_GROUP, inputStream, inputStream.available(), suffix);
        } catch (Exception ex) {
            throw new RenException(ErrorCode.OSS_UPLOAD_FILE_ERROR, ex, ex.getMessage());
        }

        return config.getFastdfsDomain() + "/" + storePath.getPath();
    }

    @Override
    public String uploadSuffix(byte[] data, String suffix) {
        return upload(data, suffix);
    }

    @Override
    public String uploadSuffix(InputStream inputStream, String suffix) {
        return upload(inputStream, suffix);
    }

    @Override
    public Object startBlock(MultipartFileParamDto param, String suffix) {
        FastDFSPath store;
        try {
            byte[] initialArray = new byte[param.getTotal().intValue()];
            InputStream targetStream = ByteSource.wrap(initialArray).openStream();
            StorePath path = defaultAppendFileStorageClient.uploadAppenderFile(DEFAULT_GROUP, targetStream, param.getTotal(), suffix);//添加空文件
            store = new FastDFSPath(path.getPath(), path.getGroup());
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RenException(e.getMessage());
        }
        return store;
    }

    @Override
    public void processingBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj) {
        long offset = CHUNK_SIZE * (param.getChunk() - 1);
        try {
            FastDFSPath path = (FastDFSPath) processingObj.getStore();
            //一个个修改文件
            log.info(param.getChunk() + ":开始上传,start:" + offset);
            defaultAppendFileStorageClient.modifyFile(path.getGroup(), path.getPath(), param.getFile().getInputStream(), param.getFile().getSize(), offset);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RenException(e.getMessage());
        }
    }

    @Override
    public String endBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj) {
        processingObj.setStatus(true);
        FastDFSPath path = (FastDFSPath) processingObj.getStore();
        return config.getFastdfsDomain() + "/" + path.getPath();
    }
}