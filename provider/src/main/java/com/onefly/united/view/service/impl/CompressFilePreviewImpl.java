package com.onefly.united.view.service.impl;

import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.model.ReturnResponse;
import com.onefly.united.view.model.StorageType;
import com.onefly.united.view.service.FilePreview;
import com.onefly.united.view.service.cache.caffeine.CaffeineCache;
import com.onefly.united.view.utils.DownloadUtils;
import com.onefly.united.view.utils.FileMD5Util;
import com.onefly.united.view.utils.FileUtils;
import com.onefly.united.view.utils.ZipReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;

/**
 * Created by kl on 2018/1/17.
 * Content :处理压缩包文件
 */
@Service
public class CompressFilePreviewImpl implements FilePreview {
    @Autowired
    private FileUtils fileUtils;
    @Autowired
    private DownloadUtils downloadUtils;
    @Autowired
    private ZipReader zipReader;
    @Autowired
    private KkViewProperties kkViewProperties;
    @Autowired
    private CaffeineCache caffeineCache;

    @Override
    public DeferredResult<Object> filePreviewHandle(String url, Model model, FileAttribute fileAttribute) throws IOException {
        String fileName = fileAttribute.getName();
        String suffix = fileAttribute.getSuffix();
        String fileTree = null;
        DeferredResult<Object> deferredResult = new DeferredResult<>();
        // 判断文件名是否存在(redis缓存读取)
        if (!caffeineCache.containConvertedFiles(fileName) || !kkViewProperties.getCache().isEnabled()) {
            String filePath;
            if (StorageType.URL == fileAttribute.getStorageType()) {
                ReturnResponse<String> response = downloadUtils.downLoad(fileAttribute, null);
                if (0 != response.getCode()) {
                    model.addAttribute("fileType", suffix);
                    model.addAttribute("msg", response.getMsg());
                    deferredResult.setResult("fileNotSupported");
                    return deferredResult;
                }
                filePath = response.getContent();
            } else {
                filePath = kkViewProperties.getFileSaveDir() + url;
            }
            if ("zip".equalsIgnoreCase(suffix) || "jar".equalsIgnoreCase(suffix) || "gzip".equalsIgnoreCase(suffix)) {
                fileTree = zipReader.readZipFile(filePath, fileName);
            } else if ("rar".equalsIgnoreCase(suffix)) {
                fileTree = zipReader.unRar(filePath, fileName);
            } else if ("7z".equalsIgnoreCase(suffix)) {
                fileTree = zipReader.read7zFile(filePath, fileName);
            }
            if (fileTree != null && !"null".equals(fileTree) && kkViewProperties.getCache().isEnabled()) {
                caffeineCache.addConvertedFile(fileName, fileTree,null);
            }
        } else {
            fileTree = caffeineCache.getConvertedFile(fileName);
        }
        if (fileTree != null && !"null".equals(fileTree)) {
            model.addAttribute("fileTree", fileTree);
            deferredResult.setResult("compress");
            return deferredResult;
        } else {
            model.addAttribute("fileType", suffix);
            model.addAttribute("msg", "压缩文件类型不受支持，尝试在压缩的时候选择RAR4格式");
            deferredResult.setResult("fileNotSupported");
            return deferredResult;
        }
    }
}
