package com.onefly.united.oss.dto;

import lombok.Data;

@Data
public class SysOssDto {
    /**
     * URL地址
     */
    private String url;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件大小（k）
     */
    private String size;
}
