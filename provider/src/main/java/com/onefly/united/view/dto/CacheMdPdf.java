package com.onefly.united.view.dto;

import lombok.Data;

@Data
public class CacheMdPdf {
    private String pdfName;
    private String md5;
    private boolean doing = false;
}
