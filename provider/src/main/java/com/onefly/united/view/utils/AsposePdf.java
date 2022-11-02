package com.onefly.united.view.utils;

import com.aspose.cells.*;
import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;
import com.aspose.words.Document;
import com.aspose.words.PdfCompliance;
import com.aspose.words.PdfSaveOptions;

import java.io.ByteArrayInputStream;
import java.io.File;

public class AsposePdf {

    public static boolean loadpptLicense() throws Exception {
        String license = "<License>\n" +
                "  <Data>\n" +
                "    <Products>\n" +
                "      <Product>Aspose.Total for Java</Product>\n" +
                "      <Product>Aspose.Words for Java</Product>\n" +
                "    </Products>\n" +
                "    <EditionType>Enterprise</EditionType>\n" +
                "    <SubscriptionExpiry>20991231</SubscriptionExpiry>\n" +
                "    <LicenseExpiry>20991231</LicenseExpiry>\n" +
                "    <SerialNumber>8bfe198c-7f0c-4ef8-8ff0-acc3237bf0d7</SerialNumber>\n" +
                "  </Data>\n" +
                "  <Signature>sNLLKGMUdF0r8O1kKilWAGdgfs2BvJb/2Xp8p5iuDVfZXmhppo+d0Ran1P9TKdjV4ABwAgKXxJ3jcQTqE/2IRfqwnPf8itN8aFZlV3TJPYeD3yWE7IT55Gz6EijUpC7aKeoohTb4w2fpox58wWoF3SNp6sK6jDfiAUGEHYJ9pjU=</Signature>\n" +
                "</License>";
        com.aspose.slides.License aposeLic = new com.aspose.slides.License();
        aposeLic.setLicense(new ByteArrayInputStream(license.getBytes()));
        return true;
    }

    public static boolean loadxlsLicense() throws Exception {
        String license = "<License>\n" +
                "  <Data>\n" +
                "    <Products>\n" +
                "      <Product>Aspose.Total for Java</Product>\n" +
                "      <Product>Aspose.Words for Java</Product>\n" +
                "    </Products>\n" +
                "    <EditionType>Enterprise</EditionType>\n" +
                "    <SubscriptionExpiry>20991231</SubscriptionExpiry>\n" +
                "    <LicenseExpiry>20991231</LicenseExpiry>\n" +
                "    <SerialNumber>8bfe198c-7f0c-4ef8-8ff0-acc3237bf0d7</SerialNumber>\n" +
                "  </Data>\n" +
                "  <Signature>sNLLKGMUdF0r8O1kKilWAGdgfs2BvJb/2Xp8p5iuDVfZXmhppo+d0Ran1P9TKdjV4ABwAgKXxJ3jcQTqE/2IRfqwnPf8itN8aFZlV3TJPYeD3yWE7IT55Gz6EijUpC7aKeoohTb4w2fpox58wWoF3SNp6sK6jDfiAUGEHYJ9pjU=</Signature>\n" +
                "</License>";
        com.aspose.cells.License aposeLic = new com.aspose.cells.License();
        aposeLic.setLicense(new ByteArrayInputStream(license.getBytes()));
        return true;
    }

    /**
     * doc转pdf
     *
     * @param wordPath
     * @param pdfPath
     */
    public static void doc2pdf(String wordPath, String pdfPath) throws Exception {
        try {
            System.out.println("start===========");
            long old = System.currentTimeMillis();
            PdfSaveOptions saveOptions = new PdfSaveOptions();
            saveOptions.setTempFolder(System.getProperty("java.io.tmpdir") + File.separator);
            saveOptions.setCompliance(PdfCompliance.PDF_17);
            //Address是将要被转化的word文档
            Document doc = new Document(wordPath);
            //全面支持DOC, DOCX, OOXML, RTF HTML, OpenDocument, PDF, EPUB, XPS, SWF 相互转换
            doc.save(pdfPath, saveOptions);
            long now = System.currentTimeMillis();
            //转化用时
            System.out.println("Word 转 Pdf 共耗时：" + ((now - old) / 1000.0) + "秒");
        } catch (Exception e) {
            System.out.println("Word 转 Pdf 失败...");
            e.printStackTrace();
        }
    }

    public static void ppt2pdf(String pptPath, String pdfPath) throws Exception {
        if (!loadpptLicense()) {
            System.out.println("阿西吧。。。。。。。");
        }
        long old = System.currentTimeMillis();
        Presentation pres = new Presentation(pptPath);//输入pdf路径
        pres.save(pdfPath, SaveFormat.Pdf);
        long now = System.currentTimeMillis();
        System.out.println("共耗时：" + ((now - old) / 1000.0) + "秒\n\n"); //转化过程耗时
    }

    /**
     * pdf不好
     *
     * @param xlsPath
     * @param pdfPath
     * @throws Exception
     */
    public static void exe2pdf(String xlsPath, String pdfPath) throws Exception {
        if (!loadxlsLicense()) {
            System.out.println("雅蠛蝶");
        }
        long old = System.currentTimeMillis();
        Workbook wb = new Workbook(xlsPath);// 原始excel路径
        wb.save(pdfPath, FileFormatType.PDF);
        long now = System.currentTimeMillis();
        System.out.println("共耗时：" + ((now - old) / 1000.0) + "秒");
    }

    public static void exe2html(String xlsPath, String pdfPath) throws Exception {
        if (!loadxlsLicense()) {
            System.out.println("雅蠛蝶");
        }
        long old = System.currentTimeMillis();
        Workbook wb = new Workbook(xlsPath);// 原始excel路径
        Worksheet worksheet = wb.getWorksheets().get(0);
        Style style = wb.createStyle();
        style.setTextWrapped(true);
        style.setBorder(BorderType.BOTTOM_BORDER, CellBorderType.THIN, Color.getBlack());
        style.setBorder(BorderType.LEFT_BORDER, CellBorderType.THIN, Color.getBlack());
        style.setBorder(BorderType.RIGHT_BORDER, CellBorderType.THIN, Color.getBlack());
        style.setBorder(BorderType.TOP_BORDER, CellBorderType.THIN, Color.getBlack());
        worksheet.autoFitColumns();
        worksheet.autoFitRows();
        Cells cells = worksheet.getCells();
        int col = cells.getColumns().getCount();
        int row = cells.getRows().getCount();
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                Cell cell = cells.get(i, j);
                cell.setStyle(style);
            }
        }
        HtmlSaveOptions options = new HtmlSaveOptions();
        options.setAddTooltipText(true);
        wb.save(pdfPath, options);
        long now = System.currentTimeMillis();
        System.out.println("共耗时：" + ((now - old) / 1000.0) + "秒");
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        // doc2pdf("F:/error.docx", "F:/opertion.pdf");
        // System.out.println("时间:"+(System.currentTimeMillis()-start)/1000);
        // ppt2pdf("E:/data/error.pptx","E:/data/p.pdf");
        ppt2pdf("F:/07f5c4aa9f7d482f808f729236b4c70a.pptx","F:/fuck.pdf");
    }

}
