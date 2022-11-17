/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.onefly.united.oss.cloud;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import com.onefly.united.common.exception.ErrorCode;
import com.onefly.united.common.exception.RenException;
import com.onefly.united.oss.dto.AliyunStore;
import com.onefly.united.oss.dto.FileUploadResult;
import com.onefly.united.oss.dto.MultipartFileParamDto;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云存储
 *
 * @author Mark sunlightcs@gmail.com
 */
@Slf4j
public class AliyunCloudStorageService extends AbstractCloudStorageService {

    public AliyunCloudStorageService(CloudStorageConfig config) {
        this.config = config;
    }

    @Override
    public String upload(byte[] data, String path) {
        return upload(new ByteArrayInputStream(data), path);
    }

    @Override
    public String upload(InputStream inputStream, String path) {
        OSS client = new OSSClientBuilder().build(config.getAliyunEndPoint(), config.getAliyunAccessKeyId(),
                config.getAliyunAccessKeySecret());
        try {
            client.putObject(config.getAliyunBucketName(), path, inputStream);
        } catch (Exception e) {
            throw new RenException(ErrorCode.OSS_UPLOAD_FILE_ERROR, e, "");
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
        return config.getAliyunDomain() + "/" + path;
    }

    @Override
    public String uploadSuffix(byte[] data, String suffix) {
        return upload(data, getPath(config.getAliyunPrefix(), suffix));
    }

    @Override
    public String uploadSuffix(InputStream inputStream, String suffix) {
        return upload(inputStream, getPath(config.getAliyunPrefix(), suffix));
    }

    @Override
    public Object startBlock(MultipartFileParamDto param, String suffix) {
        OSS ossClient = new OSSClientBuilder().build(config.getAliyunEndPoint(), config.getAliyunAccessKeyId(),
                config.getAliyunAccessKeySecret());
        AliyunStore store = new AliyunStore();
        try {
            String objectName = getPath(config.getAliyunPrefix(), suffix);
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(config.getAliyunBucketName(), objectName);
            // 初始化分片。
            InitiateMultipartUploadResult upresult = ossClient.initiateMultipartUpload(request);
            String uploadId = upresult.getUploadId();
            store.setUploadId(uploadId);
            store.setObjectName(objectName);
        } catch (OSSException oe) {
            log.error("初始化错误:Error Message:{},Error Code:{},Request ID:{},Host ID:{}", oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
            throw new RenException(oe.getErrorMessage());
        } catch (ClientException ce) {
            log.error("初始化错误:Error Message:{}" + ce.getMessage());
            throw new RenException(ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return store;
    }

    @Override
    public void processingBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj) {
        long offset = CHUNK_SIZE * (param.getChunk() - 1);
        OSS ossClient = new OSSClientBuilder().build(config.getAliyunEndPoint(), config.getAliyunAccessKeyId(),
                config.getAliyunAccessKeySecret());
        try {
            AliyunStore store = (AliyunStore) processingObj.getStore();
            log.info(param.getChunk() + ":开始上传,start:" + offset);
            // 跳过已经上传的分片。
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(config.getAliyunBucketName());
            uploadPartRequest.setKey(store.getObjectName());
            uploadPartRequest.setUploadId(store.getUploadId());
            uploadPartRequest.setInputStream(param.getFile().getInputStream());
            // 设置分片大小。除了最后一个分片没有大小限制，其他的分片最小为100 KB。
            uploadPartRequest.setPartSize(param.getFile().getSize());
            // 设置分片号。每一个上传的分片都有一个分片号，取值范围是1~10000，如果超出此范围，OSS将返回InvalidArgument错误码。
            uploadPartRequest.setPartNumber(param.getChunk());
            // 每个分片不需要按顺序上传，甚至可以在不同客户端上传，OSS会按照分片号排序组成完整的文件。
            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
            // 每次上传分片之后，OSS的返回结果包含PartETag。PartETag将被保存在partETags中。
        } catch (OSSException oe) {
            log.error("处理中错误:Error Message:{},Error Code:{},Request ID:{},Host ID:{}", oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
            throw new RenException(oe.getErrorMessage());
        } catch (ClientException ce) {
            log.error("处理中错误:Error Message:{}" + ce.getMessage());
            throw new RenException(ce.getMessage());
        } catch (IOException e) {
            log.error("处理中错误:Error Message:{}" + e.getMessage());
            throw new RenException(e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public String endBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj) {
        String url = null;
        OSS ossClient = new OSSClientBuilder().build(config.getAliyunEndPoint(), config.getAliyunAccessKeyId(),
                config.getAliyunAccessKeySecret());
        try {
            processingObj.setStatus(true);
            List<PartETag> partETags = new ArrayList<PartETag>();
            AliyunStore store = (AliyunStore) processingObj.getStore();
            ListPartsRequest listPartsRequest = new ListPartsRequest(config.getAliyunBucketName(), store.getObjectName(), store.getUploadId());
            PartListing partListing = ossClient.listParts(listPartsRequest);
            int partCount = partListing.getParts().size();
            for (int i = 0; i < partCount; i++) {
                PartSummary partSummary = partListing.getParts().get(i);
                partETags.add(new PartETag(partSummary.getPartNumber(), partSummary.getETag()));
            }

            //合并
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(config.getAliyunBucketName(), store.getObjectName(), store.getUploadId(), partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult = ossClient.completeMultipartUpload(completeMultipartUploadRequest);
            log.info(completeMultipartUploadResult.getETag());
            url = config.getAliyunDomain() + "/" + store.getObjectName();
        } catch (OSSException oe) {
            log.error("结束错误:Error Message:{},Error Code:{},Request ID:{},Host ID:{}", oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
            throw new RenException(oe.getErrorMessage());
        } catch (ClientException ce) {
            log.error("结束错误:Error Message:{}" + ce.getMessage());
            throw new RenException(ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return url;
    }
}