package com.onefly.united.oss.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.onefly.united.common.constant.Constant;
import com.onefly.united.common.page.PageData;
import com.onefly.united.common.redis.RedisUtils;
import com.onefly.united.common.service.impl.BaseServiceImpl;
import com.onefly.united.common.utils.ConvertUtils;
import com.onefly.united.common.utils.Result;
import com.onefly.united.oss.dao.SysOssDao;
import com.onefly.united.oss.dto.*;
import com.onefly.united.oss.entity.SysOssEntity;
import com.onefly.united.oss.service.SysOssService;
import com.onefly.united.view.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
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

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

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
    public SysOssDto insertOssEntity(String url, String originalFilename, long size) {
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
    public CheckFileDto checkFileMd5(String md5) throws IOException {
        FileUploadResult processing = (FileUploadResult) redisUtils.hGet(Constants.ASYNC_UPLOADER, md5);//true 已经上传，false 上传一部分
        if (processing == null) {
            return CheckFileDto.builder().resultStatus(ResultStatus.NO_HAVE).build();
        }
        if (processing.getStatus()) {
            return CheckFileDto.builder().resultStatus(ResultStatus.IS_HAVE).build();
        } else {
            List<String> missChunkList = new LinkedList<>();
            for (int i = 1; i <= processing.getChunks(); i++) {
                if (!redisUtils.hHasKey(Constants.ASYNC_UPLOADER_CHUNK, md5 + "@" + i)) {
                    missChunkList.add(String.valueOf(i));
                }
            }
            return CheckFileDto.builder().resultStatus(ResultStatus.ING_HAVE).missChunkList(missChunkList).build();
        }
    }

    @Override
    public DeferredResult<Result<SysOssDto>> asyncUploader(MultipartFileParamDto param) {
        DeferredResult<Result<SysOssDto>> deferredResult = new DeferredResult<>(600000L);//10分钟
        AsyncDeferred deferred = AsyncDeferred.builder().param(param).result(deferredResult).build();
        applicationEventPublisher.publishEvent(deferred);
        return deferredResult;
    }
}
