/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 *
 * https://www.renren.io
 *
 * 版权所有，侵权必究！
 */

package com.onefly.united.oss.service;

import com.onefly.united.common.page.PageData;
import com.onefly.united.common.service.BaseService;
import com.onefly.united.oss.dto.CheckFileDto;
import com.onefly.united.oss.dto.MultipartFileParamDto;
import com.onefly.united.oss.dto.SysOssDto;
import com.onefly.united.oss.entity.SysOssEntity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 文件上传
 * 
 * @author Mark sunlightcs@gmail.com
 */
public interface SysOssService extends BaseService<SysOssEntity> {

	PageData<SysOssEntity> page(Map<String, Object> params);

    SysOssDto insertOssEntity(InputStream inputStream, String originalFilename, long size);

    CheckFileDto checkFileMd5(String md5) throws IOException;

    SysOssDto uploadFileByMappedByteBuffer(MultipartFileParamDto param) throws IOException;
}
