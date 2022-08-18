package com.onefly.united.view.service.impl;

import com.google.common.collect.Lists;
import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.service.FilePreview;
import com.onefly.united.view.utils.DownloadUtils;
import com.onefly.united.view.utils.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.util.List;

/**
 * Created by kl on 2018/1/17.
 * Content :图片文件处理
 */
@Service
public class PictureFilePreviewImpl implements FilePreview {

    private final FileUtils fileUtils;

    private final DownloadUtils downloadUtils;

    public PictureFilePreviewImpl(FileUtils fileUtils,
                                  DownloadUtils downloadUtils) {
        this.fileUtils = fileUtils;
        this.downloadUtils = downloadUtils;
    }

    @Override
    public DeferredResult<Object> filePreviewHandle(String url, Model model, FileAttribute fileAttribute) throws IOException {
        String fileKey = (String) RequestContextHolder.currentRequestAttributes().getAttribute("fileKey", 0);
        DeferredResult<Object> deferredResult = new DeferredResult<>();
        List<String> imgUrls = Lists.newArrayList(url);
        model.addAttribute("imgurls", imgUrls);
        model.addAttribute("currentUrl", url);
        deferredResult.setResult("picture");
        return deferredResult;
    }
}
