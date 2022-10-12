/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.onefly.united.oss.controller;

import com.google.gson.Gson;
import com.onefly.united.common.annotation.LogOperation;
import com.onefly.united.common.constant.Constant;
import com.onefly.united.common.exception.ErrorCode;
import com.onefly.united.common.page.PageData;
import com.onefly.united.common.utils.Result;
import com.onefly.united.common.validator.ValidatorUtils;
import com.onefly.united.common.validator.group.DefaultGroup;
import com.onefly.united.oauth2.service.SysParamsService;
import com.onefly.united.oss.cloud.CloudStorageConfig;
import com.onefly.united.oss.dto.CheckFileDto;
import com.onefly.united.oss.dto.MultipartFileParamDto;
import com.onefly.united.oss.dto.SysOssDto;
import com.onefly.united.oss.entity.SysOssEntity;
import com.onefly.united.oss.group.AliyunGroup;
import com.onefly.united.oss.group.QcloudGroup;
import com.onefly.united.oss.group.QiniuGroup;
import com.onefly.united.oss.service.SysOssService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * 文件上传
 *
 * @author Mark sunlightcs@gmail.com
 */
@RestController
@RequestMapping("/api/oss")
@Api(tags = "文件上传")
public class SysOssController {
    @Autowired
    private SysOssService sysOssService;
    @Autowired
    private SysParamsService sysParamsService;

    private final static String KEY = Constant.CLOUD_STORAGE_CONFIG_KEY;

    @GetMapping("page")
    @ApiOperation(value = "分页")
    @PreAuthorize("hasAuthority('sys:oss:all')")
    public Result<PageData<SysOssEntity>> page(@ApiIgnore @RequestParam Map<String, Object> params) {
        PageData<SysOssEntity> page = sysOssService.page(params);

        return new Result<PageData<SysOssEntity>>().ok(page);
    }

    @GetMapping("info")
    @ApiOperation(value = "云存储配置信息")
    @PreAuthorize("hasAuthority('sys:oss:all')")
    public Result<CloudStorageConfig> info() {
        CloudStorageConfig config = sysParamsService.getValueObject(KEY, CloudStorageConfig.class);
        return new Result<CloudStorageConfig>().ok(config);
    }

    @PostMapping
    @ApiOperation(value = "保存云存储配置信息")
    @LogOperation("保存云存储配置信息")
    @PreAuthorize("hasAuthority('sys:oss:all')")
    public Result saveConfig(@RequestBody CloudStorageConfig config) {
        //校验类型
        ValidatorUtils.validateEntity(config);

        if (config.getType() == Constant.CloudService.QINIU.getValue()) {
            //校验七牛数据
            ValidatorUtils.validateEntity(config, QiniuGroup.class);
        } else if (config.getType() == Constant.CloudService.ALIYUN.getValue()) {
            //校验阿里云数据
            ValidatorUtils.validateEntity(config, AliyunGroup.class);
        } else if (config.getType() == Constant.CloudService.QCLOUD.getValue()) {
            //校验腾讯云数据
            ValidatorUtils.validateEntity(config, QcloudGroup.class);
        }

        sysParamsService.updateValueByCode(KEY, new Gson().toJson(config));

        return new Result();
    }

    @PostMapping("upload")
    @ApiOperation(value = "上传文件")
    @PreAuthorize("hasAuthority('sys:oss:all')")
    public Result<SysOssDto> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return new Result<SysOssDto>().error(ErrorCode.UPLOAD_FILE_EMPTY);
        }
        SysOssDto sysOssDto = sysOssService.insertOssEntity(file.getInputStream(), file.getOriginalFilename(), file.getSize());

        return new Result<SysOssDto>().ok(sysOssDto);
    }

    @DeleteMapping
    @ApiOperation(value = "删除")
    @LogOperation("删除")
    @PreAuthorize("hasAuthority('sys:oss:all')")
    public Result delete(@RequestBody Long[] ids) {
        sysOssService.deleteBatchIds(Arrays.asList(ids));

        return new Result();
    }

    @ApiOperation("分块上传大文件")
    @PostMapping("fragUpload")
    public Result<SysOssDto> fragUpload(MultipartFileParamDto param, HttpServletRequest request) throws IOException {
        ValidatorUtils.validateEntity(param, DefaultGroup.class);
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        SysOssDto sysOssDto = null;
        if (isMultipart) {
            sysOssDto = sysOssService.uploadFileByMappedByteBuffer(param);
        } else {
            sysOssDto = sysOssService.insertOssEntity(param.getFile().getInputStream(), param.getFile().getOriginalFilename()
                    , param.getFile().getSize());
        }
        return new Result<SysOssDto>().ok(sysOssDto);
    }

    /**
     * 秒传判断，断点判断
     *
     * @return
     */
    @ApiOperation("秒传判断，断点判断")
    @RequestMapping(value = "fragUpload", method = RequestMethod.GET)
    public Result<CheckFileDto> checkFileMd5(String md5) throws IOException {
        CheckFileDto checkFileDto = sysOssService.checkFileMd5(md5);
        return new Result<CheckFileDto>().ok(checkFileDto);
    }

}