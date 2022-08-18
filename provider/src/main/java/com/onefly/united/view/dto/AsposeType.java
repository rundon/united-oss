package com.onefly.united.view.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支持的格式
 */
@Getter
@AllArgsConstructor
public enum AsposeType {
    WORD("doc,docx,rtf,dot,dotx,dotm,odt,ott,wordml,html,mhtml"),
    CELL("xls,xlt,xlsx,xlsb,xltx,xltm,xlsm,xml,ods,csv,tsv"),
    SLIDES("ppt,pptx,pps,pot,ppsx,pptm,ppsm,potx,potm,odp");
    private String desc;
}
