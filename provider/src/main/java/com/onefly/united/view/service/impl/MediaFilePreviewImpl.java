package com.onefly.united.view.service.impl;

import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.service.FilePreview;
import com.onefly.united.view.utils.DownloadUtils;
import com.onefly.united.view.utils.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;

/**
 * @author : kl
 * @authorboke : kailing.pub
 * @create : 2018-03-25 上午11:58
 * @description:
 **/
@Service
public class MediaFilePreviewImpl implements FilePreview {

    private final DownloadUtils downloadUtils;

    private final FileUtils fileUtils;
    private final KkViewProperties kkViewProperties;

    public MediaFilePreviewImpl(DownloadUtils downloadUtils,
                                FileUtils fileUtils,
                                KkViewProperties kkViewProperties) {
        this.downloadUtils = downloadUtils;
        this.fileUtils = fileUtils;
        this.kkViewProperties = kkViewProperties;
    }

    @Override
    public DeferredResult<Object> filePreviewHandle(String url, Model model, FileAttribute fileAttribute) throws IOException {
        model.addAttribute("mediaUrl", url);
        DeferredResult<Object> deferredResult = new DeferredResult<>();
        String suffix = fileAttribute.getSuffix();
        if ("flv".equalsIgnoreCase(suffix)) {
            deferredResult.setResult("flv");
            return deferredResult;
        }
        deferredResult.setResult("media");
        return deferredResult;
    }


}
