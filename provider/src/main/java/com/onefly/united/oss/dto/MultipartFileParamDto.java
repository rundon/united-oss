package com.onefly.united.oss.dto;


import com.onefly.united.common.validator.group.DefaultGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Data
@ToString
@ApiModel(value = "分块上传大文件")
public class MultipartFileParamDto {

    //总分片数量
    private int chunks;
    //当前为第几块分片
    private int chunk;
    //当前分片大小
    private long size = 0L;
    //文件名
    private String name;
    //分片对象
    @ApiModelProperty(value = "文件")
    @NotNull(message = "文件", groups = DefaultGroup.class)
    private MultipartFile file;

    // MD5
    @NotNull(message = "文件的MD5不能为空", groups = DefaultGroup.class)
    private String md5;

    private long total;
}
