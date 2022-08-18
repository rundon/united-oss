package com.onefly.united.view.utils;

import com.onefly.united.common.exception.RenException;
import com.onefly.united.view.config.KkViewProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class Rar5Utils {

    private final KkViewProperties kkViewProperties;

    public Rar5Utils(KkViewProperties kkViewProperties) {
        this.kkViewProperties = kkViewProperties;
    }

    /**
     * 解压rar5
     *
     * @param inputFilePath
     * @param outputFilePath
     * @return
     */
    public boolean unRar5(String inputFilePath, String outputFilePath) {
        boolean result = true;
        File file = new File(outputFilePath);
        //如果保存路径不存在，则创建路径
        if (!file.exists()) {
            file.mkdirs();
        }
        String unrar = kkViewProperties.getUnRar();
        if (PlatformUtils.isWindows()) {//wind 拿本地的绿色版
            String separator = File.separator;
            String userDir = System.getProperty("user.dir");
            if (userDir.contains("united-view")) {
                unrar = userDir + separator + "srvhost" + separator + "src" + separator + "main" + separator + "WinRAR" + separator + "UnRAR.exe";
            }
        }
        if (!new File(unrar).exists()) {
            throw new RenException("WinRAR不存在，请安装WinRAR，并配置united.kkview.unRar");
        }
        CommandLine cmdLine = new CommandLine(unrar);
        cmdLine.addArgument("x");
        cmdLine.addArgument("-y");
        cmdLine.addArgument(inputFilePath, true);
        cmdLine.addArgument(outputFilePath, true);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(1);
        //非阻塞
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        try {
            executor.execute(cmdLine, resultHandler);
            resultHandler.waitFor();
        } catch (Exception e) {
            result = false;
            e.printStackTrace();
        }
        File input = new File(inputFilePath);
        if (input.exists()) {
            input.delete();
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        KkViewProperties kkViewProperties = new KkViewProperties();
        kkViewProperties.setUnRar("C:\\Program Files\\WinRAR\\UnRAR.exe");
        Rar5Utils rar5Utils = new Rar5Utils(kkViewProperties);
        Boolean result = rar5Utils.unRar5("D:\\data\\file\\9b5d5e0b-af87-4462-8fb2-bacd4b82e4b5.rar", "D:\\data\\file\\351bbc10-5243-450c-8f1f-2dfdfd1c2299");
        System.out.println(result);
    }
}
