package com.onefly.united.oss.cloud;

import com.google.common.collect.Lists;
import com.onefly.united.common.exception.ErrorCode;
import com.onefly.united.common.exception.RenException;
import com.onefly.united.oss.dto.CloudStore;
import com.onefly.united.oss.dto.FileUploadResult;
import com.onefly.united.oss.dto.MultipartFileParamDto;
import io.minio.*;
import io.minio.messages.Part;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * MinIO 存储
 *
 * @author rundon
 */
@Slf4j
public class MinioCloudStorageService extends AbstractCloudStorageService {

    private CustomMinioClient minioClient;

    public MinioCloudStorageService(CloudStorageConfig config) {
        this.config = config;
        //初始化
        init();
    }

    private void init() {
        MinioClient client =
                MinioClient.builder()
                        .endpoint(config.getMinioEndPoint())
                        .credentials(config.getMinioAccessKey(), config.getMinioSecretKey())
                        .build();
        minioClient = new CustomMinioClient(client);
    }

    @Override
    public String upload(byte[] data, String path) {
        return upload(new ByteArrayInputStream(data), path);
    }

    @Override
    public String upload(InputStream inputStream, String path) {
        try {
            //如果BucketName不存在，则创建
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(config.getMinioBucketName()).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(config.getMinioBucketName()).build());
            }
            PutObjectArgs arg = PutObjectArgs.builder()
                    .bucket(config.getMinioBucketName())
                    .object(path)
                    .stream(inputStream, Long.valueOf(inputStream.available()), -1)
                    .contentType("application/octet-stream").build();
            minioClient.putObject(arg);
        } catch (Exception e) {
            throw new RenException(ErrorCode.OSS_UPLOAD_FILE_ERROR, e, "");
        }

        return config.getMinioEndPoint() + "/" + config.getMinioBucketName() + "/" + path;
    }

    @Override
    public String uploadSuffix(byte[] data, String suffix) {
        return upload(data, getPath(config.getMinioPrefix(), suffix));
    }

    @Override
    public String uploadSuffix(InputStream inputStream, String suffix) {
        return upload(inputStream, getPath(config.getMinioPrefix(), suffix));
    }

    @Override
    public Object startBlock(MultipartFileParamDto param, String suffix) {
        CloudStore store = new CloudStore();
        String objectName = getPath(config.getAliyunPrefix(), suffix);
        try {
            String uploadId = minioClient.initMultiPartUpload(config.getMinioBucketName(), objectName);
            store.setUploadId(uploadId);
            store.setObjectName(objectName);
        } catch (Exception e) {
            log.error("minio 初始化异常" + e.getMessage());
            throw new RenException(e.getMessage());
        }
        return store;
    }

    @Override
    public void processingBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj) {
        CloudStore store = (CloudStore) processingObj.getStore();
        try {
            minioClient.uploadMultipart(config.getMinioBucketName(), store.getObjectName(), param.getFile().getInputStream()
                    , param.getFile().getSize(), store.getUploadId(), param.getChunk());
        } catch (Exception e) {
            log.error("minio 分块上传异常" + e.getMessage());
            throw new RenException(e.getMessage());
        }
    }

    @Override
    public String endBlock(MultipartFileParamDto param, String suffix, FileUploadResult processingObj) {
        String url = null;
        try {
            List<Part> parts = Lists.newArrayList();
            processingObj.setStatus(true);
            CloudStore store = (CloudStore) processingObj.getStore();
            ListPartsResponse partResult = minioClient.listMultipart(config.getMinioBucketName(), store.getObjectName()
                    , 0, store.getUploadId());
            int partNumber = 1;
            for (Part part : partResult.result().partList()) {
                parts.add(partNumber - 1, new Part(partNumber, part.etag()));
                partNumber++;
            }
            ObjectWriteResponse result = minioClient.mergeMultipartUpload(config.getMinioBucketName(), store.getObjectName(), store.getUploadId(), parts.toArray(new Part[parts.size()]));
            log.info("合并结果：" + result.object());
            url = config.getMinioEndPoint() + "/" + config.getMinioBucketName() + "/" + store.getObjectName();
        } catch (Exception e) {
            log.error("minio 合并异常" + e.getMessage());
            throw new RenException(e.getMessage());
        }
        return url;
    }
}
