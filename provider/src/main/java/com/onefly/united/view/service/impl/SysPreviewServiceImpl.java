package com.onefly.united.view.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.onefly.united.common.exception.RenException;
import com.onefly.united.oss.entity.SysOssEntity;
import com.onefly.united.oss.service.SysOssService;
import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.dto.CacheMdPdf;
import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.model.FileType;
import com.onefly.united.view.service.FilePreview;
import com.onefly.united.view.service.FilePreviewFactory;
import com.onefly.united.view.service.SysPreviewService;
import com.onefly.united.view.service.cache.caffeine.CaffeineCache;
import com.onefly.united.view.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

@Slf4j
@Service
public class SysPreviewServiceImpl implements SysPreviewService {

    @Autowired
    private KkViewProperties kkViewProperties;

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private FilePreviewFactory previewFactory;

    @Autowired
    private CaffeineCache caffeineCache;
    @Autowired
    private SysOssService sysOssService;

    @Override
    public DeferredResult<Object> onlinePreviewById(String id, Model model, HttpServletRequest req) throws Exception {
        SysOssEntity sysOssEntity = sysOssService.selectById(Long.valueOf(id));
        String url = sysOssEntity.getUrl();
        model.addAttribute("fileTitle", sysOssEntity.getFileName());
        if (StringUtils.isBlank(url)) {
            throw new RenException("无法查询到url请检查。");
        }
        return onlinePreviewByUrl(url, model, req);
    }

    @Override
    public DeferredResult<Object> onlinePreviewByUrl(String url, Model model, HttpServletRequest req) throws Exception {
        FileAttribute fileAttribute = fileUtils.getFileAttribute(url);
        req.setAttribute("fileKey", req.getParameter("fileKey"));
        model.addAttribute("pdfDownloadDisable", String.valueOf(kkViewProperties.isDisableDown()));
        model.addAttribute("officePreviewType", req.getParameter("officePreviewType"));
        FilePreview filePreview = previewFactory.get(fileAttribute);
        log.info("预览文件url：{}，previewType：{}", url, fileAttribute.getType());
        return filePreview.filePreviewHandle(url, model, fileAttribute);
    }

    @Override
    public String cleanPreview(String url) throws Exception {
        FileAttribute fileAttribute = fileUtils.getFileAttribute(url);
        if (fileAttribute.getType() == FileType.pdf || fileAttribute.getType() == FileType.office) {
            String pdfName;
            String md5 = null;
            if (fileAttribute.getType() == FileType.office) {
                CacheMdPdf cacheMdPdf = caffeineCache.loadPdfName(fileAttribute);
                pdfName = cacheMdPdf.getPdfName();
                md5 = cacheMdPdf.getMd5();
            } else {
                String fileName = fileAttribute.getName();
                pdfName = fileName.substring(0, fileName.lastIndexOf(".") + 1) + "pdf";
            }
            if (caffeineCache.containConvertedFiles(pdfName)) {
                String outFilePath = caffeineCache.getConvertedFile(pdfName);
                if (!outFilePath.startsWith(kkViewProperties.getFileSaveDir())) {
                    outFilePath = kkViewProperties.getFileSaveDir() + outFilePath;
                }
                File file = new File(outFilePath);
                if (file.exists()) {
                    file.delete();
                }
                Integer imageCount = caffeineCache.getConvertedPdfImage(outFilePath.replaceFirst(kkViewProperties.getFileSaveDir(), ""));
                if (imageCount != null && imageCount > 0) {
                    int index = outFilePath.lastIndexOf(".");
                    String folder = outFilePath.substring(0, index);

                    File image = new File(folder);
                    if (image.exists()) {
                        image.delete();
                    }
                    caffeineCache.deleteConvertedPdfImage(outFilePath.replaceFirst(kkViewProperties.getFileSaveDir(), ""));
                }
                caffeineCache.deleteConvertedFile(pdfName, md5);
                return "success";
            }
        } else if (fileAttribute.getType() == FileType.compress) {//压缩包
            String fileName = fileAttribute.getName();
            String key = null;
            if (caffeineCache.containConvertedFiles(fileName)) {
                String fileTree = caffeineCache.getConvertedFile(fileName);
                JSONObject json = JSON.parseObject(fileTree);
                String parentPaths = (String) json.get("fileName");
                String[] splitPaths = parentPaths.split("_");
                if (splitPaths.length > 1) {
                    String splitPath = splitPaths[1];
                    String[] files = splitPath.split("/");
                    if (files.length > 0) {
                        key = files[0];
                        String file = kkViewProperties.getFileDir() + files[0];
                        File dir = new File(file);
                        if (dir.exists()) {
                            System.out.println("递归删文件");
                            FileUtils.delDir(dir);
                        }
                    }
                }
                System.out.println("清除缓存");
                caffeineCache.deleteConvertedFile(fileName, null);
                caffeineCache.cleanAllLikeValue(key);
            }
        }
        return "success";
    }

    @Override
    public String cleanPreviewById(String id, HttpServletRequest req) throws Exception {
        SysOssEntity sysOssEntity = sysOssService.selectById(Long.valueOf(id));
        String url = sysOssEntity.getUrl();
        if (StringUtils.isBlank(url)) {
            throw new RenException("无法查询到url请检查。");
        }
        return cleanPreview(url);
    }
}
