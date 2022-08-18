package com.onefly.united.view.model;

import lombok.Data;

/**
 * Created by kl on 2018/1/17.
 * Content :
 */
@Data
public class FileAttribute {

    private FileType type;

    private String suffix;

    private String name;

    private String url;

    private StorageType storageType = StorageType.URL;

    public FileAttribute() {
    }

    public FileAttribute(FileType type, String suffix, String name, String url) {
        this.type = type;
        this.suffix = suffix;
        this.name = name;
        this.url = url;
    }
}
