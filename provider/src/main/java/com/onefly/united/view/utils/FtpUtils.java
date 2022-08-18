package com.onefly.united.view.utils;

import com.onefly.united.view.config.KkViewProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.util.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @auther: chenjh
 * @since: 2019/6/18 14:36
 */
@Slf4j
public class FtpUtils {

    public static FTPClient connect(String host, int port, String username, String password, String controlEncoding) throws IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(host, port);
        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            ftpClient.login(username, password);
        }
        int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
        }
        ftpClient.setControlEncoding(controlEncoding);
        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
        return ftpClient;
    }

    public static void download(String ftpUrl, String localFilePath, String ftpUsername, String ftpPassword, String ftpControlEncoding, KkViewProperties kkViewProperties) throws IOException {
        String username = StringUtils.isEmpty(ftpUsername) ? kkViewProperties.getFtp().getUsername() : ftpUsername;
        String password = StringUtils.isEmpty(ftpPassword) ? kkViewProperties.getFtp().getPassword() : ftpPassword;
        String controlEncoding = StringUtils.isEmpty(ftpControlEncoding) ? kkViewProperties.getFtp().getEncoding() : ftpControlEncoding;
        URL url = new URL(ftpUrl);
        String host = url.getHost();
        int port = (url.getPort() == -1) ? url.getDefaultPort() : url.getPort();
        String remoteFilePath = url.getPath();
        log.debug("FTP connection url:{}, username:{}, password:{}, controlEncoding:{}, localFilePath:{}", ftpUrl, username, password, controlEncoding, localFilePath);
        FTPClient ftpClient = connect(host, port, username, password, controlEncoding);
        OutputStream outputStream = new FileOutputStream(localFilePath);
        ftpClient.enterLocalPassiveMode();
        boolean downloadResult = ftpClient.retrieveFile(new String(remoteFilePath.getBytes(controlEncoding), StandardCharsets.ISO_8859_1), outputStream);
        log.debug("FTP download result {}", downloadResult);
        outputStream.flush();
        outputStream.close();
        ftpClient.logout();
        ftpClient.disconnect();
    }
}
