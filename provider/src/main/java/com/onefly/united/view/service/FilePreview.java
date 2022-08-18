package com.onefly.united.view.service;

import com.onefly.united.view.model.FileAttribute;
import org.springframework.ui.Model;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Created by kl on 2018/1/17.
 * Content :
 */
public interface FilePreview {
    DeferredResult<Object> filePreviewHandle(String url, Model model, FileAttribute fileAttribute) throws Exception;
}
