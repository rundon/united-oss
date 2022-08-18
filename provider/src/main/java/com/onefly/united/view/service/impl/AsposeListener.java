package com.onefly.united.view.service.impl;

import com.aspose.cells.HtmlSaveOptions;
import com.aspose.cells.Workbook;
import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;
import com.aspose.words.Document;
import com.aspose.words.PdfCompliance;
import com.aspose.words.PdfSaveOptions;
import com.onefly.united.common.exception.RenException;
import com.onefly.united.view.dto.AsposeDto;
import com.onefly.united.view.dto.AsposeType;
import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.service.cache.caffeine.CaffeineCache;
import com.onefly.united.view.utils.PdfUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.File;

import static com.onefly.united.view.service.cache.caffeine.CaffeineCache.current;

@Slf4j
@Component
public class AsposeListener {

    @Autowired
    private PdfUtils pdfUtils;

    @Autowired
    private CaffeineCache caffeineCache;

    public static final String OFFICE_PREVIEW_TYPE_IMAGE = "image";

    public static final String OFFICE_PREVIEW_TYPE_ALL_IMAGES = "allImages";

    @Async
    @EventListener
    public String office2pdf(AsposeDto asposeDto) {
        String result = "";
        long old = System.currentTimeMillis();
        log.info("开始异步任务转换:");
        String inputFilePath = asposeDto.getInputFilePath();
        String outputFilePath = asposeDto.getOutputFilePath();
        String officePreviewType = asposeDto.getOfficePreviewType();
        Model model = asposeDto.getModel();
        FileAttribute fileAttribute = asposeDto.getFileAttribute();
        boolean isHtml = asposeDto.isHtml();
        DeferredResult<Object> deferredResult = asposeDto.getResult();
        try {
            if (AsposeType.WORD == asposeDto.getAsposeType()) {
                PdfSaveOptions saveOptions = new PdfSaveOptions();
                saveOptions.setTempFolder(System.getProperty("java.io.tmpdir") + File.separator);
                saveOptions.setCompliance(PdfCompliance.PDF_17);
                Document doc = new Document(inputFilePath);
                doc.save(outputFilePath, saveOptions);
            } else if (AsposeType.CELL == asposeDto.getAsposeType()) {
                Workbook wb = new Workbook(inputFilePath);// 原始excel路径
                HtmlSaveOptions options = new HtmlSaveOptions();
                options.setAddTooltipText(true);
                wb.save(outputFilePath, options);
            } else {
                Presentation pres = new Presentation(inputFilePath);//输入pdf路径
                pres.save(outputFilePath, SaveFormat.Pdf);
            }
            long now = System.currentTimeMillis();
            log.info("转换成功，共耗时：" + ((now - old) / 1000.0) + "秒");
            File inputFile = new File(inputFilePath);
            if (inputFile.exists()) {
                log.info("删除文件:" + inputFilePath);
                inputFile.delete();
            }
            if (!isHtml && (OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType) || OFFICE_PREVIEW_TYPE_ALL_IMAGES.equals(officePreviewType))) {
                String res = OfficeFilePreviewImpl.getPreviewType(model, fileAttribute, officePreviewType, outputFilePath, pdfUtils, OFFICE_PREVIEW_TYPE_IMAGE);
                deferredResult.setResult(res);
                //log.info("结果：" + res);
                result = res;
            } else {
                deferredResult.setResult(isHtml ? "html" : "pdf");
                //log.info("结束了");
                result = isHtml ? "html" : "pdf";
            }
        } catch (Exception e) {
            e.printStackTrace();
            caffeineCache.deleteConvertedFile(asposeDto.getPdfName(), asposeDto.getMd5());
            throw new RenException("转换错误:" + e.getMessage());
        } finally {
            if (StringUtils.isNotBlank(asposeDto.getMd5())) {
                current.remove(asposeDto.getMd5());
                log.info("移除正在进行任务：" + asposeDto.getMd5());
            }
        }
        return result;
    }

}
