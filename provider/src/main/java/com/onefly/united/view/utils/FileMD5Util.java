package com.onefly.united.view.utils;

import com.onefly.united.common.utils.SpringContextUtils;
import com.onefly.united.view.config.KkViewProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.*;
import java.security.cert.X509Certificate;

/**
 * 文件md5值
 */
@Slf4j
public class FileMD5Util {
    private static KkViewProperties kkViewProperties;

    static {
        FileMD5Util.kkViewProperties = SpringContextUtils.getBean(KkViewProperties.class);
        try {
            trustAllHttpsCertificates();
            HttpsURLConnection.setDefaultHostnameVerifier
                    (
                            (urlHostName, session) -> true
                    );
        } catch (Exception e) {
        }
    }

    /**
     * 获取md5
     *
     * @param path
     * @return
     * @throws FileNotFoundException
     */
    public static String getFileMD5(String path) throws FileNotFoundException, MalformedURLException {
        String value = null;
        if (path != null && path.toLowerCase().startsWith("http")) {
            URL url = new URL(path);
            InputStream inputStream = null;
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3 * 1000);
                inputStream = connection.getInputStream();
                value = DigestUtils.md5Hex(inputStream);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (inputStream != null) {
                        //关闭流
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {
            if (!path.startsWith(kkViewProperties.getFileSaveDir())) {
                path = kkViewProperties.getFileSaveDir() + path;
            }
            File file = new File(path);
            MappedByteBuffer byteBuffer = null;
            FileInputStream in = new FileInputStream(file);
            try {
                byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(byteBuffer);
                BigInteger bi = new BigInteger(1, md5.digest());
                value = bi.toString(16);
                if (value.length() < 32) {
                    value = "0" + value;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != in) {
                    try {
                        in.getChannel().close();
                        in.close();
                    } catch (IOException e) {
                        log.error("get file md5 error!!!", e);
                    }
                }
                if (null != byteBuffer) {
                    freedMappedByteBuffer(byteBuffer);
                }
            }
        }
        return value;
    }

    /**
     * 在MappedByteBuffer释放后再对它进行读操作的话就会引发jvm crash，在并发情况下很容易发生
     * 正在释放时另一个线程正开始读取，于是crash就发生了。所以为了系统稳定性释放前一般需要检 查是否还有线程在读或写
     *
     * @param mappedByteBuffer
     */
    public static void freedMappedByteBuffer(final MappedByteBuffer mappedByteBuffer) {
        try {
            if (mappedByteBuffer == null) {
                return;
            }
            mappedByteBuffer.force();
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    try {
                        Method getCleanerMethod = mappedByteBuffer.getClass().getMethod("cleaner", new Class[0]);
                        getCleanerMethod.setAccessible(true);
                        sun.misc.Cleaner cleaner = (sun.misc.Cleaner) getCleanerMethod.invoke(mappedByteBuffer,
                                new Object[0]);
                        cleaner.clean();
                    } catch (Exception e) {
                        log.error("clean MappedByteBuffer error!!!", e);
                    }
                    //log.info("clean MappedByteBuffer completed!!!");
                    return null;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 跳过ssl证书
     *
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private static void trustAllHttpsCertificates()
            throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[1];
        trustAllCerts[0] = (TrustManager) new TrustAllManager();
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    private static class TrustAllManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }
    }
}
