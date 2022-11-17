/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.onefly.united.oss.cloud;

import com.google.common.collect.Lists;
import com.onefly.united.common.exception.ErrorCode;
import com.onefly.united.common.exception.RenException;
import com.onefly.united.oss.dto.FileUploadResult;
import com.onefly.united.oss.dto.MultipartFileParamDto;
import com.onefly.united.oss.dto.QcloudStore;
import com.onefly.united.oss.dto.QiniuStore;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Client;
import com.qiniu.http.Response;
import com.qiniu.storage.*;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 七牛云存储
 *
 * @author Mark sunlightcs@gmail.com
 */
@Slf4j
public class QiniuCloudStorageService extends AbstractCloudStorageService {
    private UploadManager uploadManager;
    private String token;
    private Client client;
    private Configuration configuration;

    public QiniuCloudStorageService(CloudStorageConfig config) {
        this.config = config;
        //初始化
        init();
    }

    private void init() {
        this.configuration = new Configuration(Region.autoRegion());
        this.client = new Client(this.configuration);
        uploadManager = new UploadManager(client, null);
        token = Auth.create(config.getQiniuAccessKey(), config.getQiniuSecretKey()).
                uploadToken(config.getQiniuBucketName());
    }

    @Override
    public String upload(byte[] data, String path) {
        try {
            Response res = uploadManager.put(data, path, token);
            if (!res.isOK()) {
                throw new RenException(ErrorCode.OSS_UPLOAD_FILE_ERROR, res.toString());
            }
        } catch (Exception e) {
            throw new RenException(ErrorCode.OSS_UPLOAD_FILE_ERROR, e, "");
        }

        return config.getQiniuDomain() + "/" + path;
    }

    @Override
    public String upload(InputStream inputStream, String path) {
        try {
            byte[] data = IOUtils.toByteArray(inputStream);
            return this.upload(data, path);
        } catch (IOException e) {
            throw new RenException(ErrorCode.OSS_UPLOAD_FILE_ERROR, e, "");
        }
    }

    @Override
    public String uploadSuffix(byte[] data, String suffix) {
        return upload(data, getPath(config.getQiniuPrefix(), suffix));
    }

    @Override
    public String uploadSuffix(InputStream inputStream, String suffix) {
        return upload(inputStream, getPath(config.getQiniuPrefix(), suffix));
    }

    @Override
    public Object startBlock(MultipartFileParamDto param, String suffix) {
        QiniuStore store = new QiniuStore();
        String objectName = getPath(config.getQcloudPrefix(), suffix);
        ApiUploadV2InitUpload initUploadApi = new ApiUploadV2InitUpload(client);
        ApiUploadV2InitUpload.Request initUploadRequest = new ApiUploadV2InitUpload.Request(config.getQiniuPrefix(), token)
                .setKey(objectName);
        try {
            ApiUploadV2InitUpload.Response initUploadResponse = initUploadApi.request(initUploadRequest);
            store.setUploadId(initUploadResponse.getUploadId());
            store.setObjectName(objectName);
        } catch (Exception e) {
            log.error("七牛存储 初始化异常" + e.getMessage());
            throw new RenException(e.getMessage());
        }
        return store;
    }

    @Override
    public void processingBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj) {
        QcloudStore store = (QcloudStore) processingObj.getStore();
        ApiUploadV2UploadPart uploadPartApi = new ApiUploadV2UploadPart(client);
        try {
            ApiUploadV2UploadPart.Request uploadPartRequest = new ApiUploadV2UploadPart.Request(config.getQiniuPrefix(), token, store.getUploadId(), param.getChunk())
                    .setKey(store.getObjectName())
                    .setUploadData(param.getFile().getInputStream(), null, param.getSize());
            ApiUploadV2UploadPart.Response uploadPartResponse = uploadPartApi.request(uploadPartRequest);
            log.warn("upload part:" + uploadPartResponse.getResponse());
        } catch (Exception e) {
            log.error("七牛存储 上传异常" + e.getMessage());
            throw new RenException(e.getMessage());
        }
    }

    @Override
    public String endBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj) {
        String url = null;
        // 获取上传的 part 信息
        Integer partNumberMarker = null;
        QcloudStore store = (QcloudStore) processingObj.getStore();
        List<Map<String, Object>> listPartInfo = Lists.newArrayList();
        while (true) {
            ApiUploadV2ListParts listPartsApi = new ApiUploadV2ListParts(client);
            ApiUploadV2ListParts.Request listPartsRequest = new ApiUploadV2ListParts.Request(config.getQiniuPrefix(), token, store.getUploadId())
                    .setKey(store.getObjectName())
                    .setPartNumberMarker(partNumberMarker);
            try {
                ApiUploadV2ListParts.Response listPartsResponse = listPartsApi.request(listPartsRequest);
                partNumberMarker = listPartsResponse.getPartNumberMarker();
                listPartInfo.addAll(listPartsResponse.getParts());
                log.warn("list part:" + listPartsResponse.getResponse());
                // 列举结束
                if (partNumberMarker == 0) {
                    break;
                }
            } catch (QiniuException e) {
                log.error(e.getMessage());
                break;
            }
        }

        ApiUploadV2CompleteUpload completeUploadApi = new ApiUploadV2CompleteUpload(client);
        ApiUploadV2CompleteUpload.Request completeUploadRequest = new ApiUploadV2CompleteUpload.Request(config.getQiniuPrefix(), token, store.getUploadId(), listPartInfo)
                .setKey(store.getObjectName());
        try {
            ApiUploadV2CompleteUpload.Response completeUploadResponse = completeUploadApi.request(completeUploadRequest);
            log.warn("complete upload:" + completeUploadResponse.getResponse());
            url = config.getQiniuDomain() + "/" + store.getObjectName();
        } catch (QiniuException e) {
            e.printStackTrace();
        }
        return url;
    }
}
