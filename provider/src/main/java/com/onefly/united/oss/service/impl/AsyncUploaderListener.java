package com.onefly.united.oss.service.impl;

import com.onefly.united.common.exception.RenException;
import com.onefly.united.common.utils.Result;
import com.onefly.united.oss.cloud.OSSFactory;
import com.onefly.united.oss.dto.AsyncDeferred;
import com.onefly.united.oss.dto.MultipartFileParamDto;
import com.onefly.united.oss.dto.SysOssDto;
import com.onefly.united.oss.service.SysOssService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * 异步上传
 */
@Slf4j
@Component
public class AsyncUploaderListener {

    @Autowired
    private SysOssService sysOssService;

    @EventListener
    @Async("myExecutor")
    public void AsyncUploader(AsyncDeferred result) {
        MultipartFileParamDto param = result.getParam();
        String extension = FilenameUtils.getExtension(param.getName());
        String url = OSSFactory.build().syncUploadBlock(param, extension);
        DeferredResult<Result<SysOssDto>> deferredResult = result.getResult();
        if (StringUtils.isNotBlank(url)) {
            try {//为了防止异常导致死等待 必须有返回值
                deferredResult.setResult(new Result().ok(sysOssService.insertOssEntity(url, param.getName(), param.getTotal())));
            } catch (Exception e) {
                log.error("上传错误：" + e.getMessage());
                throw new RenException(e.getMessage());
            } finally {
                deferredResult.setResult(null);
            }
        } else {
            deferredResult.setResult(null);
        }
    }
}
