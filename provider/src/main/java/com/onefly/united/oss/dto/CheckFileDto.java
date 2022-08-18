package com.onefly.united.oss.dto;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@ApiModel(value = "秒传判断，断点判断")
public class CheckFileDto {
    private ResultStatus resultStatus;
    private List<String> missChunkList;
}
