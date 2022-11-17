package com.onefly.united.oss.dto;

import com.aliyun.oss.model.PartETag;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AliyunPath {
    private String uploadId;
    private String objectName;
    private List<PartETag> partETags = Lists.newArrayList();
}
