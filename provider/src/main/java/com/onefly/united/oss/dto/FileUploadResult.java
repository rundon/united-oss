package com.onefly.united.oss.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadResult implements Serializable {

    /**
     * 文件id(MD5)
     */
    private String fileId;

    private Integer chunks;
    /**
     * is finish upload file
     */
    private Boolean status = false;
}
