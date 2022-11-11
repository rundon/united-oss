package com.onefly.united.view.service;

import com.onefly.united.view.model.FileAttribute;
import com.onefly.united.view.model.FileType;
import com.onefly.united.view.service.cache.CacheService;
import com.onefly.united.view.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ExtendedModelMap;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;

/**
 * Created by kl on 2018/1/19.
 * Content :消费队列中的转换文件
 */
@Service
public class FileConvertQueueTask {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FilePreviewFactory previewFactory;

    private final CacheService cacheService;

    private final FileUtils fileUtils;

    private final  ExecutorService myExecutor;

    public FileConvertQueueTask(FilePreviewFactory previewFactory,
                                CacheService cacheService,
                                FileUtils fileUtils,
                                ExecutorService myExecutor) {
        this.previewFactory = previewFactory;
        this.cacheService = cacheService;
        this.fileUtils=fileUtils;
        this.myExecutor=myExecutor;
    }

    @PostConstruct
    public void startTask(){
        myExecutor.submit(new ConvertTask(previewFactory, cacheService, fileUtils));
        logger.info("队列处理文件转换任务启动完成 ");
    }

    static class ConvertTask implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(ConvertTask.class);

        private final FilePreviewFactory previewFactory;

        private final CacheService cacheService;

        private final FileUtils fileUtils;

        public ConvertTask(FilePreviewFactory previewFactory,
                           CacheService cacheService,
                           FileUtils fileUtils) {
            this.previewFactory = previewFactory;
            this.cacheService = cacheService;
            this.fileUtils=fileUtils;
        }

        @Override
        public void run() {
            while (true) {
                String url = null;
                try {
                    url = cacheService.takeQueueTask();
                    if(url != null){
                        FileAttribute fileAttribute = fileUtils.getFileAttribute(url);
                        FileType fileType = fileAttribute.getType();
                        logger.info("正在处理预览转换任务，url：{}，预览类型：{}", url, fileType);
                        if(fileType.equals(FileType.compress) || fileType.equals(FileType.office) || fileType.equals(FileType.cad)) {
                            FilePreview filePreview = previewFactory.get(fileAttribute);
                            filePreview.filePreviewHandle(url, new ExtendedModelMap(), fileAttribute);
                        } else {
                            logger.info("预览类型无需处理，url：{}，预览类型：{}", url, fileType);
                        }
                    }
                } catch (Exception e) {
                    try {
                        Thread.sleep(1000*10);
                    } catch (Exception ex){
                        ex.printStackTrace();
                    }
                    logger.info("处理预览转换任务异常，url：{}", url, e);
                }
            }
        }
    }

}
