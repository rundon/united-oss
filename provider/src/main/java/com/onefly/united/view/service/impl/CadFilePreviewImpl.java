package com.onefly.united.view.service.impl;


import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.model.ReturnResponse;
import com.onefly.united.view.model.StorageType;
import com.onefly.united.view.service.FilePreview;
import com.onefly.united.view.service.cache.caffeine.CaffeineCache;
import com.onefly.united.view.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.async.DeferredResult;

import static com.onefly.united.view.service.impl.OfficeFilePreviewImpl.getPreviewType;

/**
 * @author chenjh
 * @since 2019/11/21 14:28
 */
@Service
public class CadFilePreviewImpl implements FilePreview {
    @Autowired
    private FileUtils fileUtils;
    @Autowired
    private DownloadUtils downloadUtils;
    @Autowired
    private CadUtils cadUtils;
    @Autowired
    private PdfUtils pdfUtils;
    @Autowired
    private KkViewProperties kkViewProperties;
    @Autowired
    private CaffeineCache caffeineCache;
    private static final String OFFICE_PREVIEW_TYPE_IMAGE = "image";
    private static final String OFFICE_PREVIEW_TYPE_ALL_IMAGES = "allImages";


    @Override
    public DeferredResult<Object> filePreviewHandle(String url, Model model, FileAttribute fileAttribute) throws Exception {
        // 预览Type，参数传了就取参数的，没传取系统默认
        String officePreviewType = model.asMap().get("officePreviewType") == null ? kkViewProperties.getPreviewType() : model.asMap().get("officePreviewType").toString();
        String suffix = fileAttribute.getSuffix();
        String fileName = fileAttribute.getName();
        String pdfName = fileName.substring(0, fileName.lastIndexOf(".") + 1) + "pdf";
        String outFilePath = kkViewProperties.getFileDir() + pdfName;
        DeferredResult<Object> deferredResult = new DeferredResult<>();
        // 判断之前是否已转换过，如果转换过，直接返回，否则执行转换
        if (!caffeineCache.containConvertedFiles(pdfName) || !kkViewProperties.getCache().isEnabled()) {
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
            if (StringUtils.hasText(outFilePath)) {
                boolean convertResult = cadUtils.cadToPdf(filePath, outFilePath);
                if (!convertResult) {
                    model.addAttribute("fileType", suffix);
                    model.addAttribute("msg", "cad文件转换异常，请联系管理员");
                    deferredResult.setResult("fileNotSupported");
                    return deferredResult;
                }
                if (kkViewProperties.getCache().isEnabled()) {
                    // 加入缓存
                    caffeineCache.addConvertedFile(pdfName, fileUtils.getRelativePath(outFilePath),null);
                }
            }
        }
        if (OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType) || OFFICE_PREVIEW_TYPE_ALL_IMAGES.equals(officePreviewType)) {
            deferredResult.setResult(getPreviewType(model, fileAttribute, officePreviewType, outFilePath, pdfUtils, OFFICE_PREVIEW_TYPE_IMAGE));
            return deferredResult;
        }
        model.addAttribute("pdfUrl", fileUtils.getRelativePath(outFilePath));
        deferredResult.setResult("pdf");
        return deferredResult;
    }


}
