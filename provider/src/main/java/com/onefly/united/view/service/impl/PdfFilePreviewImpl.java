package com.onefly.united.view.service.impl;

import com.google.common.collect.Lists;
import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.model.ReturnResponse;
import com.onefly.united.view.model.StorageType;
import com.onefly.united.view.service.FilePreview;
import com.onefly.united.view.service.cache.caffeine.CaffeineCache;
import com.onefly.united.view.utils.DownloadUtils;
import com.onefly.united.view.utils.FileMD5Util;
import com.onefly.united.view.utils.FileUtils;
import com.onefly.united.view.utils.PdfUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import static com.onefly.united.view.service.impl.OfficeFilePreviewImpl.getLastFileName;

/**
 * Created by kl on 2018/1/17.
 * Content :处理pdf文件
 */
@Service
public class PdfFilePreviewImpl implements FilePreview {
    @Autowired
    private FileUtils fileUtils;
    @Autowired
    private PdfUtils pdfUtils;
    @Autowired
    private DownloadUtils downloadUtils;
    @Autowired
    private KkViewProperties kkViewProperties;
    @Autowired
    private CaffeineCache caffeineCache;

    @Override
    public DeferredResult<Object> filePreviewHandle(String url, Model model, FileAttribute fileAttribute) throws Exception {
        String suffix = fileAttribute.getSuffix();
        String fileName = fileAttribute.getName();
        String officePreviewType = model.asMap().get("officePreviewType") == null ? kkViewProperties.getPreviewType() : model.asMap().get("officePreviewType").toString();
        String pdfName = fileName.substring(0, fileName.lastIndexOf(".") + 1) + "pdf";
        String outFilePath = kkViewProperties.getFileDir() + pdfName;
        DeferredResult<Object> deferredResult = new DeferredResult<>();
        if (StorageType.LOCAL == fileAttribute.getStorageType()) {
            outFilePath = kkViewProperties.getFileSaveDir() + url;
        }
        if (OfficeFilePreviewImpl.OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType) || OfficeFilePreviewImpl.OFFICE_PREVIEW_TYPE_ALL_IMAGES.equals(officePreviewType)) {
            //当文件不存在时，就去下载
            if (!caffeineCache.containConvertedFiles(pdfName) || !kkViewProperties.getCache().isEnabled()) {
                if (StorageType.URL == fileAttribute.getStorageType()) {
                    ReturnResponse<String> response = downloadUtils.downLoad(fileAttribute, fileName);
                    if (0 != response.getCode()) {
                        model.addAttribute("fileType", suffix);
                        model.addAttribute("msg", response.getMsg());
                        deferredResult.setResult("fileNotSupported");
                        return deferredResult;
                    }
                    outFilePath = response.getContent();
                }
                if (kkViewProperties.getCache().isEnabled()) {
                    // 加入缓存
                    caffeineCache.addConvertedFile(pdfName, fileUtils.getRelativePath(outFilePath),null);
                }
            }
            List<String> imageUrls = pdfUtils.pdf2jpg(outFilePath, getLastFileName(caffeineCache.getConvertedFile(pdfName), "/"));
            if (imageUrls == null || imageUrls.size() < 1) {
                model.addAttribute("msg", "pdf转图片异常，请联系管理员");
                model.addAttribute("fileType", fileAttribute.getSuffix());
                deferredResult.setResult("fileNotSupported");
                return deferredResult;
            }
            List<String> decodeimageUrls = Lists.newArrayList();
            imageUrls.stream().forEach(image -> {
                try {
                    decodeimageUrls.add(URLDecoder.decode(URLDecoder.decode(image, "UTF-8"), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            });
            model.addAttribute("imgurls", decodeimageUrls);
            model.addAttribute("currentUrl", decodeimageUrls.get(0));
            if (OfficeFilePreviewImpl.OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType)) {
                deferredResult.setResult("officePicture");
                return deferredResult;
            } else {
                deferredResult.setResult("picture");
                return deferredResult;
            }
        } else {
            // 不是http开头，浏览器不能直接访问，需下载到本地
            if (url != null && url.toLowerCase().startsWith("http")) {
                if (!caffeineCache.containConvertedFiles(pdfName) || !kkViewProperties.getCache().isEnabled()) {
                    ReturnResponse<String> response = downloadUtils.downLoad(fileAttribute, pdfName);
                    if (0 != response.getCode()) {
                        model.addAttribute("fileType", suffix);
                        model.addAttribute("msg", response.getMsg());
                        deferredResult.setResult("fileNotSupported");
                        return deferredResult;
                    }
                    model.addAttribute("pdfUrl", fileUtils.getRelativePath(response.getContent()));
                    if (kkViewProperties.getCache().isEnabled()) {
                        // 加入缓存
                        caffeineCache.addConvertedFile(pdfName, fileUtils.getRelativePath(outFilePath),null);
                    }
                } else {
                    model.addAttribute("pdfUrl", fileUtils.getRelativePath(outFilePath));
                }
            } else {
                model.addAttribute("pdfUrl", url);
            }
        }
        deferredResult.setResult("pdf");
        return deferredResult;
    }
}
