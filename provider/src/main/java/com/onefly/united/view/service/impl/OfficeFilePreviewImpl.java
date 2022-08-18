package com.onefly.united.view.service.impl;

import com.google.common.collect.Lists;
import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.dto.AsposeDto;
import com.onefly.united.view.dto.AsposeType;
import com.onefly.united.view.dto.CacheMdPdf;
import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.model.ReturnResponse;
import com.onefly.united.view.model.StorageType;
import com.onefly.united.view.service.FilePreview;
import com.onefly.united.view.service.cache.caffeine.CaffeineCache;
import com.onefly.united.view.utils.DownloadUtils;
import com.onefly.united.view.utils.FileUtils;
import com.onefly.united.view.utils.PdfUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

/**
 * Created by kl on 2018/1/17.
 * Content :处理office文件
 */
@Slf4j
@Service
public class OfficeFilePreviewImpl implements FilePreview {
    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private PdfUtils pdfUtils;

    @Autowired
    private DownloadUtils downloadUtils;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private KkViewProperties kkViewProperties;

    @Autowired
    private CaffeineCache caffeineCache;

    public static final String OFFICE_PREVIEW_TYPE_IMAGE = "image";
    public static final String OFFICE_PREVIEW_TYPE_ALL_IMAGES = "allImages";

    @Override
    public DeferredResult<Object> filePreviewHandle(String url, Model model, FileAttribute fileAttribute) throws Exception {
        // 预览Type，参数传了就取参数的，没传取系统默认
        String officePreviewType = model.asMap().get("officePreviewType") == null ? kkViewProperties.getPreviewType() : model.asMap().get("officePreviewType").toString();
        String suffix = fileAttribute.getSuffix();
        boolean isHtml = suffix.equalsIgnoreCase("xls") || suffix.equalsIgnoreCase("xlsx");
        CacheMdPdf cacheMdPdf = caffeineCache.loadPdfName(fileAttribute);
        String pdfName = cacheMdPdf.getPdfName();
        String outFilePath = kkViewProperties.getFileDir() + pdfName;
        DeferredResult<Object> deferredResult = new DeferredResult<>(600000L);//10分钟
        if (StorageType.LOCAL == fileAttribute.getStorageType()) {
            outFilePath = kkViewProperties.getFileSaveDir() + url.substring(0, url.lastIndexOf(".") + 1) + (isHtml ? "html" : "pdf");
        }
        log.info("是否正在做" + cacheMdPdf.isDoing());
        if (cacheMdPdf.isDoing()) {
            model.addAttribute("msg", "文件正在转换请耐心等待");
            deferredResult.setResult("fileDoing");
            return deferredResult;
        }
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
            model.addAttribute("pdfUrl", "file/" + pdfName);
            if (StringUtils.hasText(outFilePath)) {
                AsposeType asposeType = loadAsposeTypeBySuffix(suffix);
                AsposeDto asposeDto = AsposeDto.builder().inputFilePath(filePath).outputFilePath(outFilePath)
                        .asposeType(asposeType).fileAttribute(fileAttribute).isHtml(isHtml).md5(cacheMdPdf.getMd5())
                        .model(model).officePreviewType(officePreviewType).result(deferredResult).pdfName(pdfName).build();
                applicationEventPublisher.publishEvent(asposeDto);
                if (kkViewProperties.getCache().isEnabled()) {
                    // 加入缓存
                    caffeineCache.addConvertedFile(pdfName, fileUtils.getRelativePath(outFilePath), cacheMdPdf.getMd5());
                }
                model.addAttribute("pdfUrl", fileUtils.getRelativePath(outFilePath));
                return deferredResult;
            } else {
                deferredResult.setResult(isHtml ? "html" : "pdf");
                return deferredResult;
            }
        } else {
            if (!isHtml && (OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType) || OFFICE_PREVIEW_TYPE_ALL_IMAGES.equals(officePreviewType))) {
                outFilePath = caffeineCache.getConvertedFile(pdfName);
                if (!outFilePath.startsWith(kkViewProperties.getFileSaveDir())) {
                    outFilePath = kkViewProperties.getFileSaveDir() + outFilePath;
                }
                log.info("获取的路径={}", outFilePath);
                deferredResult.setResult(getPreviewType(model, fileAttribute, officePreviewType, outFilePath, pdfUtils, OFFICE_PREVIEW_TYPE_IMAGE));
                return deferredResult;
            }
            log.info("pdfname={},真实路径={}", pdfName, caffeineCache.getConvertedFile(pdfName));
            model.addAttribute("pdfUrl", caffeineCache.getConvertedFile(pdfName));
            deferredResult.setResult(isHtml ? "html" : "pdf");
            return deferredResult;
        }
    }

    /**
     * 查询类型
     *
     * @param suffix
     * @return
     */
    public AsposeType loadAsposeTypeBySuffix(String suffix) {
        AsposeType[] all = AsposeType.values();
        for (int i = 0; i < all.length; i++) {
            AsposeType node = all[i];
            if (Lists.newArrayList(node.getDesc().split(",")).stream().filter(type -> type.equals(suffix)).findAny().isPresent()) {
                return node;
            }
        }
        return null;
    }

    static String getPreviewType(Model model, FileAttribute fileAttribute, String officePreviewType, String outFilePath, PdfUtils pdfUtils, String officePreviewTypeImage) throws Exception {
        String pdfName = getLastFileName(outFilePath, "/");
        List<String> imageUrls = pdfUtils.pdf2jpg(outFilePath, pdfName);
        if (imageUrls == null || imageUrls.size() < 1) {
            model.addAttribute("msg", "office转图片异常，请联系管理员");
            model.addAttribute("fileType", fileAttribute.getSuffix());
            return "fileNotSupported";
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
        if (officePreviewTypeImage.equals(officePreviewType)) {
            return "officePicture";
        } else {
            return "picture";
        }
    }

    static String getLastFileName(String fullName, String seperator) {
        if (fullName.endsWith(seperator)) {
            fullName = fullName.substring(0, fullName.length() - 1);
        }
        String newName = fullName;
        if (fullName.contains(seperator)) {
            newName = fullName.substring(fullName.lastIndexOf(seperator) + 1);
        }
        return newName;
    }
}
