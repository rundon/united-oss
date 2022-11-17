package com.onefly.united.oss.cloud;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Part;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 自定义mini支持分块上传
 */
public class CustomMinioClient extends MinioClient {

    public CustomMinioClient(MinioClient client) {
        super(client);
    }

    /**
     * 初始化
     *
     * @param bucket
     * @param object
     * @return
     * @throws IOException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws InsufficientDataException
     * @throws ServerException
     * @throws InternalException
     * @throws XmlParserException
     * @throws InvalidResponseException
     * @throws ErrorResponseException
     */
    public String initMultiPartUpload(String bucket, String object) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException {
        CreateMultipartUploadResponse response = this.createMultipartUpload(bucket, null, object, null, null);
        return response.result().uploadId();
    }

    /**
     * 上传
     *
     * @param bucketName
     * @param objectName
     * @param data
     * @param length
     * @param uploadId
     * @param partNumber
     * @return
     * @throws ServerException
     * @throws InsufficientDataException
     * @throws ErrorResponseException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws InvalidKeyException
     * @throws XmlParserException
     * @throws InvalidResponseException
     * @throws InternalException
     */
    public UploadPartResponse uploadMultipart(String bucketName, String objectName, Object data, long length, String uploadId, int partNumber) throws ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, IOException, InvalidKeyException, XmlParserException, InvalidResponseException, InternalException {
        return this.uploadPart(bucketName, null, objectName, data, length, uploadId, partNumber, null, null);
    }

    /**
     * 合并
     *
     * @param bucketName
     * @param objectName
     * @param uploadId
     * @param parts
     * @return
     * @throws IOException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws InsufficientDataException
     * @throws ServerException
     * @throws InternalException
     * @throws XmlParserException
     * @throws InvalidResponseException
     * @throws ErrorResponseException
     */
    public ObjectWriteResponse mergeMultipartUpload(String bucketName, String objectName, String uploadId, Part[] parts) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException {
        return this.completeMultipartUpload(bucketName, null, objectName, uploadId, parts, null, null);
    }

    /**
     * 查询所有的发片
     *
     * @param bucketName
     * @param objectName
     * @param partNumberMarker
     * @param uploadId
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InsufficientDataException
     * @throws IOException
     * @throws InvalidKeyException
     * @throws ServerException
     * @throws XmlParserException
     * @throws ErrorResponseException
     * @throws InternalException
     * @throws InvalidResponseException
     */
    public ListPartsResponse listMultipart(String bucketName, String objectName, Integer partNumberMarker, String uploadId) throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException, ServerException, XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
        return this.listParts(bucketName, null, objectName, null, partNumberMarker, uploadId, null, null);
    }

}
