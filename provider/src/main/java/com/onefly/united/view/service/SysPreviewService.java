package com.onefly.united.view.service;

import org.springframework.ui.Model;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;

public interface SysPreviewService {
    DeferredResult<Object> onlinePreviewById(String id, Model model, HttpServletRequest req) throws Exception;

    DeferredResult<Object> onlinePreviewByUrl(String url, Model model, HttpServletRequest req) throws Exception;
}
