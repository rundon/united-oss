package com.onefly.united.view.controller;

import com.onefly.united.view.service.SysPreviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;

/**
 * @author yudian-it
 */
@Slf4j
@Controller
public class OnlinePreviewController {

    @Autowired
    private SysPreviewService sysPreviewService;


    @RequestMapping(value = "/onlinePreviewById")
    public DeferredResult<Object> onlinePreviewById(@RequestParam("id") String id, Model model, HttpServletRequest req) throws Exception {
        return sysPreviewService.onlinePreviewById(id, model, req);
    }

    @RequestMapping(value = "/onlinePreview")
    public DeferredResult<Object> onlinePreview(@RequestParam("url") String url, @RequestParam(required = false) String name, Model model, HttpServletRequest req) throws Exception {
        if (StringUtils.isEmpty(name)) {
            model.addAttribute("fileTitle", "文件预览");
        } else {
            model.addAttribute("fileTitle", name);
        }
        return sysPreviewService.onlinePreviewByUrl(url, model, req);
    }

    @ResponseBody
    @RequestMapping(value = "cleanPreview")
    public String cleanPreview(@RequestParam("url") String url, HttpServletRequest req) throws Exception {
        return sysPreviewService.cleanPreview(url);
    }

    @ResponseBody
    @RequestMapping(value = "cleanPreviewById")
    public String cleanPreviewById(@RequestParam("id") String id, HttpServletRequest req) throws Exception {
        return sysPreviewService.cleanPreviewById(id, req);
    }
}
