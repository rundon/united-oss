/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.onefly.united.oss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.onefly.united.common.constant.Constant;
import com.onefly.united.common.page.PageData;
import com.onefly.united.common.redis.RedisUtils;
import com.onefly.united.common.service.impl.BaseServiceImpl;
import com.onefly.united.common.utils.ConvertUtils;
import com.onefly.united.oss.cloud.OSSFactory;
import com.onefly.united.oss.dao.SysOssDao;
import com.onefly.united.oss.dto.CheckFileDto;
import com.onefly.united.oss.dto.MultipartFileParamDto;
import com.onefly.united.oss.dto.ResultStatus;
import com.onefly.united.oss.dto.SysOssDto;
import com.onefly.united.oss.entity.SysOssEntity;
import com.onefly.united.oss.service.SysOssService;
import com.onefly.united.view.utils.Constants;
import com.onefly.united.view.utils.FileMD5Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SysOssServiceImpl extends BaseServiceImpl<SysOssDao, SysOssEntity> implements SysOssService {

    private long CHUNK_SIZE = 10485760;
    @Autowired
    private RedisUtils redisUtils;

    @Override
    public PageData<SysOssEntity> page(Map<String, Object> params) {
        IPage<SysOssEntity> page = baseDao.selectPage(
                getPage(params, Constant.CREATE_DATE, false),
                new QueryWrapper<>()
        );
        return getPageData(page, SysOssEntity.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysOssDto insertOssEntity(InputStream inputStream, String originalFilename, long size) {
        //上传文件
        String extension = FilenameUtils.getExtension(originalFilename);
        String url = OSSFactory.build().uploadSuffix(inputStream, extension);

        //保存文件信息
        SysOssEntity ossEntity = new SysOssEntity();
        ossEntity.setUrl(url);
        ossEntity.setFileName(originalFilename);
        ossEntity.setSize(size);
        ossEntity.setCreateDate(new Date());
        this.insert(ossEntity);
        return ConvertUtils.sourceToTarget(ossEntity, SysOssDto.class);
    }

    @Override
    public CheckFileDto
                                                                                                                                                                                checkFileMd5(String md5) throws IOException {
        Object processingObj = redisUtils.hGet(Constants.FILE_UPLOAD_STATUS, md5);
        if (processingObj == null) {
            return CheckFileDto.builder().resultStatus(ResultStatus.NO_HAVE).build();
        }
        String processingStr = processingObj.toString();
        boolean processing = Boolean.parseBoolean(processingStr);
        String value = (String) redisUtils.get(Constants.FILE_MD5_KEY + md5);
        if (processing) {
            return CheckFileDto.builder().resultStatus(ResultStatus.IS_HAVE).build();
        } else {
            File confFile = new File(value);
            byte[] completeList = FileUtils.readFileToByteArray(confFile);
            List<String> missChunkList = new LinkedList<>();
            for (int i = 0; i < completeList.length; i++) {
                if (completeList[i] != Byte.MAX_VALUE) {
                    missChunkList.add((i + 1) + "");
                }
            }
            return CheckFileDto.builder().resultStatus(ResultStatus.ING_HAVE).missChunkList(missChunkList).build();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysOssDto uploadFileByMappedByteBuffer(MultipartFileParamDto param) throws IOException {
        String temp = System.getProperty("java.io.tmpdir") + File.separatorChar + "data" + File.separatorChar;
        String fileName = param.getName();
        String uploadDirPath = temp + param.getMd5();
        String tempFileName = fileName + "_tmp";
        File tmpDir = new File(uploadDirPath);
        File tmpFile = new File(uploadDirPath, tempFileName);
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }

        RandomAccessFile tempRaf = new RandomAccessFile(tmpFile, "rw");
        FileChannel fileChannel = tempRaf.getChannel();

        //写入该分片数据
        long offset = CHUNK_SIZE * (param.getChunk() - 1);
        byte[] fileData = param.getFile().getBytes();
        MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, offset, fileData.length);
        mappedByteBuffer.put(fileData);
        // 释放
        FileMD5Util.freedMappedByteBuffer(mappedByteBuffer);
        fileChannel.close();

        boolean isOk = checkAndSetUploadProgress(param, uploadDirPath);
        if (isOk) {
            boolean flag = renameFile(tmpFile, fileName);
            if (flag) {
                File targe = new File(tmpFile.getParent() + File.separatorChar + fileName);
                SysOssDto sysOssDto = insertOssEntity(new FileInputStream(targe), param.getName(), targe.length());
                log.info("upload complete !!" + sysOssDto.getUrl() + " name=" + fileName + "清空临时文件：" + uploadDirPath);
                delDir(tmpDir);
                return sysOssDto;
            } else {
                log.error("重命名失败了");
            }
        }
        return null;
    }

    /**
     * 检查并修改文件上传进度
     *
     * @param param
     * @param uploadDirPath
     * @return
     * @throws IOException
     */
    private boolean checkAndSetUploadProgress(MultipartFileParamDto param, String uploadDirPath) throws IOException {
        String fileName = param.getName();
        File confFile = new File(uploadDirPath, fileName + ".conf");
        RandomAccessFile accessConfFile = new RandomAccessFile(confFile, "rw");
        //把该分段标记为 true 表示完成
        log.error("set part " + (param.getChunk() - 1) + " complete");
        accessConfFile.setLength(param.getChunks());
        accessConfFile.seek((param.getChunk() - 1));
        accessConfFile.write(Byte.MAX_VALUE);

        //completeList 检查是否全部完成,如果数组里是否全部都是(全部分片都成功上传)
        byte[] completeList = FileUtils.readFileToByteArray(confFile);
        byte isComplete = Byte.MAX_VALUE;
        for (int i = 0; i < completeList.length && isComplete == Byte.MAX_VALUE; i++) {
            //与运算, 如果有部分没有完成则 isComplete 不是 Byte.MAX_VALUE
            isComplete = (byte) (isComplete & completeList[i]);
            log.error("check part " + i + " complete?:" + completeList[i]);
        }

        accessConfFile.close();
        if (isComplete == Byte.MAX_VALUE) {
            redisUtils.hDel(Constants.FILE_UPLOAD_STATUS, param.getMd5(), "true");
            redisUtils.delete(Constants.FILE_MD5_KEY + param.getMd5());
            return true;
        } else {
            if (!redisUtils.hHasKey(Constants.FILE_UPLOAD_STATUS, param.getMd5())) {
                redisUtils.hSet(Constants.FILE_UPLOAD_STATUS, param.getMd5(), "false");
            }
            if (!redisUtils.hasKey(Constants.FILE_MD5_KEY + param.getMd5())) {
                redisUtils.set(Constants.FILE_MD5_KEY + param.getMd5(), uploadDirPath + "/" + fileName + ".conf");
            }
            return false;
        }
    }

    /**
     * 文件重命名
     *
     * @param toBeRenamed   将要修改名字的文件
     * @param toFileNewName 新的名字
     * @return
     */
    public boolean renameFile(File toBeRenamed, String toFileNewName) {
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
}
