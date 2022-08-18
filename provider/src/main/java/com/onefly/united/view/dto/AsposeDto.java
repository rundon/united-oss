package com.onefly.united.view.dto;

import com.onefly.united.view.model.FileAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.context.request.async.DeferredResult;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsposeDto {
    private AsposeType asposeType;
    private String inputFilePath;
    private String outputFilePath;
    private boolean isHtml;
    private String officePreviewType;
    private Model model;
    private FileAttribute fileAttribute;
    private String pdfName;
    private String md5;
    private DeferredResult<Object> result;
}
