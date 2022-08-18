package com.onefly.united.view.service.impl;

import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.model.ReturnResponse;
import com.onefly.united.view.model.StorageType;
import com.onefly.united.view.service.FilePreview;
import com.onefly.united.view.utils.DownloadUtils;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by kl on 2018/1/17.
 * Content :处理文本文件
 */
@Service
public class SimTextFilePreviewImpl implements FilePreview {

    private final DownloadUtils downloadUtils;
    private final KkViewProperties kkViewProperties;
    public SimTextFilePreviewImpl(DownloadUtils downloadUtils,KkViewProperties kkViewProperties) {
        this.downloadUtils = downloadUtils;
        this.kkViewProperties=kkViewProperties;
    }

    @Override
    public DeferredResult<Object> filePreviewHandle(String url, Model model, FileAttribute fileAttribute) throws IOException {
        String fileName = fileAttribute.getName();
        String filePath;
        DeferredResult<Object> deferredResult = new DeferredResult<>();
        if (StorageType.URL == fileAttribute.getStorageType()) {
            ReturnResponse<String> response = downloadUtils.downLoad(fileAttribute, fileName);
            if (0 != response.getCode()) {
                model.addAttribute("fileType", fileAttribute.getSuffix());
                model.addAttribute("msg", response.getMsg());
                deferredResult.setResult("fileNotSupported");
                return deferredResult;
            }
            filePath = response.getContent();
        } else {
            filePath = kkViewProperties.getFileSaveDir() + url;
        }
        try {
            File originFile = new File(filePath);
            File previewFile = new File(filePath + ".txt");
            if (previewFile.exists()) {
                previewFile.delete();
            }
            Files.copy(originFile.toPath(), previewFile.toPath());
        } catch (IOException e) {
            model.addAttribute("msg", e.getMessage());
            model.addAttribute("fileType",fileAttribute.getSuffix());
            deferredResult.setResult("fileNotSupported");
            return deferredResult;
        }
        model.addAttribute("ordinaryUrl", (filePath+ ".txt").replace(kkViewProperties.getFileSaveDir(),""));
        deferredResult.setResult("txt");
        return deferredResult;
    }

}
