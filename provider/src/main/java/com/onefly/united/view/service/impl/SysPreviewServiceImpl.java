package com.onefly.united.view.service.impl;

import com.onefly.united.common.exception.RenException;
import com.onefly.united.oss.entity.SysOssEntity;
import com.onefly.united.oss.service.SysOssService;
import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.service.FilePreview;
import com.onefly.united.view.service.FilePreviewFactory;
import com.onefly.united.view.service.SysPreviewService;
import com.onefly.united.view.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;

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
}
