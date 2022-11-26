package com.onefly.united.oss.service;

import com.onefly.united.common.page.PageData;
import com.onefly.united.common.service.BaseService;
import com.onefly.united.common.utils.Result;
import com.onefly.united.oss.dto.CheckFileDto;
import com.onefly.united.oss.dto.MultipartFileParamDto;
import com.onefly.united.oss.dto.SysOssDto;
import com.onefly.united.oss.entity.SysOssEntity;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.util.Map;

/**
 * 文件上传
 *
 * @author rundon
 */
public interface SysOssService extends BaseService<SysOssEntity> {

    PageData<SysOssEntity> page(Map<String, Object> params);

    SysOssDto insertOssEntity(String url, String originalFilename, long size);

    CheckFileDto checkFileMd5(String md5) throws IOException;

    /**
     * 异步分块上传
     * @param param
     * @return
     */
    DeferredResult<Result<SysOssDto>> asyncUploader(MultipartFileParamDto param);
}
