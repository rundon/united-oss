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
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 腾讯云存储
 *
 * @author Mark sunlightcs@gmail.com
 */
@Slf4j
public class QcloudCloudStorageService extends AbstractCloudStorageService {
    private COSCredentials credentials;
    private ClientConfig clientConfig;

    public QcloudCloudStorageService(CloudStorageConfig config) {
        this.config = config;
        //初始化
        init();
    }

    private void init() {
        //1、初始化用户身份信息(secretId, secretKey)
        credentials = new BasicCOSCredentials(config.getQcloudSecretId(), config.getQcloudSecretKey());

        //2、设置bucket的区域, COS地域的简称请参照 https://cloud.tencent.com/document/product/436/6224
        clientConfig = new ClientConfig(new Region(config.getQcloudRegion()));
    }

    @Override
    public String upload(byte[] data, String path) {
        return upload(new ByteArrayInputStream(data), path);
    }

    @Override
    public String upload(InputStream inputStream, String path) {
        COSClient client = new COSClient(credentials, clientConfig);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(inputStream.available());
            String bucketName = config.getQcloudBucketName() + "-" + config.getQcloudAppId();
            PutObjectRequest request = new PutObjectRequest(bucketName, path, inputStream, metadata);
            PutObjectResult result = client.putObject(request);
            if (result.getETag() == null) {
                throw new RenException(ErrorCode.OSS_UPLOAD_FILE_ERROR, "");
            }
        } catch (IOException e) {
            throw new RenException(ErrorCode.OSS_UPLOAD_FILE_ERROR, e, "");
        } finally {
            client.shutdown();
        }
        return config.getQcloudDomain() + "/" + path;
    }

    @Override
    public String uploadSuffix(byte[] data, String suffix) {
        return upload(data, getPath(config.getQcloudPrefix(), suffix));
    }

    @Override
    public String uploadSuffix(InputStream inputStream, String suffix) {
        return upload(inputStream, getPath(config.getQcloudPrefix(), suffix));
    }

    @Override
    public Object startBlock(MultipartFileParamDto param, String suffix) {
        QcloudStore store = new QcloudStore();
        COSClient client = new COSClient(credentials, clientConfig);
        String bucketName = config.getQcloudBucketName() + "-" + config.getQcloudAppId();
        String objectName = getPath(config.getQcloudPrefix(), suffix);
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectName);
        // 设置存储类型, 默认是标准(Standard), 低频(Standard_IA), 归档(Archive)
        request.setStorageClass(StorageClass.Standard);
        try {
            InitiateMultipartUploadResult initResponse = client.initiateMultipartUpload(request);
            store.setUploadId(initResponse.getUploadId());
            store.setObjectName(objectName);
        } catch (Exception e) {
            log.error("腾讯云存储 初始化异常" + e.getMessage());
            throw new RenException(e.getMessage());
        } finally {
            client.shutdown();
        }
        return store;
    }

    @Override
    public void processingBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj) {
        COSClient client = new COSClient(credentials, clientConfig);
        String bucketName = config.getQcloudBucketName() + "-" + config.getQcloudAppId();
        QcloudStore store = (QcloudStore) processingObj.getStore();
        try {
            UploadPartRequest uploadRequest = new UploadPartRequest().withBucketName(bucketName).
                    withUploadId(store.getUploadId()).withKey(store.getObjectName()).withPartNumber(param.getChunk()).
                    withInputStream(param.getFile().getInputStream()).withPartSize(param.getSize());
            UploadPartResult uploadPartResult = client.uploadPart(uploadRequest);
            log.warn(uploadPartResult.getETag());
        } catch (Exception e) {
            log.error("腾讯云存储 上传异常" + e.getMessage());
            throw new RenException(e.getMessage());
        } finally {
            client.shutdown();
        }
    }

    @Override
    public String endBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj) {
        String url = null;
        COSClient client = new COSClient(credentials, clientConfig);
        String bucketName = config.getQcloudBucketName() + "-" + config.getQcloudAppId();
        QcloudStore store = (QcloudStore) processingObj.getStore();
        processingObj.setStatus(true);
        try {
            List<PartETag> partETags = Lists.newArrayList();
            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, store.getObjectName(), store.getUploadId());
            PartListing partListing = null;
            do {
                partListing = client.listParts(listPartsRequest);
                for (PartSummary partSummary : partListing.getParts()) {
                    partETags.add(new PartETag(partSummary.getPartNumber(), partSummary.getETag()));
                }
                listPartsRequest.setPartNumberMarker(partListing.getNextPartNumberMarker());
            } while (partListing.isTruncated());
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, store.getObjectName(), store.getUploadId(), partETags);
            CompleteMultipartUploadResult result = client.completeMultipartUpload(compRequest);
            log.info("合并结果：" + result.getKey());
            url = config.getQcloudDomain() + "/" + store.getObjectName();
        } catch (Exception e) {
            log.error("腾讯云存储 合并异常" + e.getMessage());
            throw new RenException(e.getMessage());
        } finally {
            client.shutdown();
        }
        return url;
    }
}