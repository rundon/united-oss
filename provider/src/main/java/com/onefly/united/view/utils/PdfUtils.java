package com.onefly.united.view.utils;

import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.service.cache.caffeine.CaffeineCache;
import lombok.extern.slf4j.Slf4j;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PDimension;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@Component
public class PdfUtils {
    @Autowired
    private FileUtils fileUtils;
    @Autowired
    private ThreadPoolTaskExecutor myExecutor;
    @Autowired
    private KkViewProperties kkViewProperties;
    @Autowired
    private CaffeineCache caffeineCache;
    @Value("${server.tomcat.uri-encoding:UTF-8}")
    private String uriEncoding;


    public List<String> pdf2jpg(String pdfFilePath, String pdfName) throws Exception {
        List<String> imageUrls = new ArrayList<>();
        Integer imageCount = caffeineCache.getConvertedPdfImage(pdfFilePath.replaceFirst(kkViewProperties.getFileSaveDir(), ""));
        String imageFileSuffix = ".jpg";
        String pathHead = pdfFilePath.replace(pdfName, "").replace(kkViewProperties.getFileSaveDir(), "");
        String pdfFolder = pathHead + pdfName.substring(0, pdfName.length() - 4);
        String urlPrefix = null;
        try {
            urlPrefix = URLEncoder.encode(URLEncoder.encode(pdfFolder, uriEncoding).replaceAll("\\+", "%20"), uriEncoding);
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException", e);
            urlPrefix = pdfFolder;
        }
        if (imageCount != null && imageCount > 0) {
            for (int i = 0; i < imageCount; i++)
                imageUrls.add(urlPrefix + "/" + i + imageFileSuffix);
            return imageUrls;
        }

        // open the url
        Document document = new Document();
        document.setFile(pdfFilePath);
        int index = pdfFilePath.lastIndexOf(".");
        String folder = pdfFilePath.substring(0, index);

        File path = new File(folder);
        if (!path.exists()) {
            path.mkdirs();
        }
        // create a list of callables.
        int pages = document.getNumberOfPages();
        for (int i = 0; i < pages; i++) {
            String imageFilePath = folder + File.separator + i + ".jpg";
            myExecutor.submit(new CapturePage(document, i, imageFilePath)).get();
            imageUrls.add(urlPrefix + "/" + i + ".jpg");
        }
        myExecutor.submit(new DocumentCloser(document)).get();
        caffeineCache.addConvertedPdfImage(pdfFilePath.replaceFirst(kkViewProperties.getFileSaveDir(), ""), pages);
        return imageUrls;
    }

    /**
     * Captures images found in a page  parse to file.
     */
    public class CapturePage implements Callable<Void> {
        private Document document;
        private int pageNumber;
        private String imageFilePath;
        private float scale = 1f;
        private float rotation = 0f;

        private CapturePage(Document document, int pageNumber, String imageFilePath) {
            this.document = document;
            this.pageNumber = pageNumber;
            this.imageFilePath = imageFilePath;
        }

        public Void call() {
            try {
                Page page = document.getPageTree().getPage(pageNumber);
                page.init();
                PDimension sz = page.getSize(Page.BOUNDARY_CROPBOX, rotation, scale);

                int pageWidth = (int) sz.getWidth();
                int pageHeight = (int) sz.getHeight();

                BufferedImage image = new BufferedImage(pageWidth,
                        pageHeight,
                        BufferedImage.TYPE_INT_RGB);
                Graphics g = image.createGraphics();

                page.paint(g, GraphicsRenderingHints.PRINT,
                        Page.BOUNDARY_CROPBOX, rotation, scale);
                g.dispose();
                // capture the page image to file
                //System.out.println("Capturing page " + pageNumber);
                File file = new File(imageFilePath);
                ImageIO.write(image, "jpg", file);
                image.flush();

            } catch (Throwable e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    /**
     * Disposes the document.
     */
    public class DocumentCloser implements Callable<Void> {
        private Document document;

        private DocumentCloser(Document document) {
            this.document = document;
        }

        public Void call() {
            if (document != null) {
                document.dispose();
                System.out.println("Document disposed");
            }
            return null;
        }
    }
}
