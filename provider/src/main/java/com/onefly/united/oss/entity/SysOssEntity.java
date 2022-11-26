package com.onefly.united.oss.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.onefly.united.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件上传
 *
 * @author rundon
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_oss")
public class SysOssEntity extends BaseEntity {
    private static final long serialVersionUID = 1L;

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
    private Long size;
}