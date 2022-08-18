package com.onefly.united.view.service.impl;

import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.service.FilePreview;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Created by kl on 2018/1/17.
 * Content :其他文件
 */
@Service
public class OtherFilePreviewImpl implements FilePreview {
    @Override
    public DeferredResult<Object> filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        DeferredResult<Object> deferredResult = new DeferredResult<>();
        model.addAttribute("fileType", fileAttribute.getSuffix());
        model.addAttribute("msg", "系统还不支持该格式文件的在线预览");
        deferredResult.setResult("fileNotSupported");
        return deferredResult;
    }
}
