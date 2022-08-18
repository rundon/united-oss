package com.onefly.united.view.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "united.kkview")
public class KkViewProperties {

    @ToString.Exclude
    private Cache cache = new Cache();

    /**
     * 临时文件存储路径
     */
    private String fileDir;

    private String fileSaveDir = "/data/";
    /**
     * 文本类型，默认如下，可自定义添加
     */
    private String simText = "txt,html,htm,asp,jsp,xml,json,properties,md,gitignore,log,java,py,c,cpp,sql,sh,bat,m,bas,prg,cmd";
    /**
     * 多媒体类型
     */
    private String media = "mp3,wav,mp4,flv";
    /**
     * office类型文档(word ppt)样式，默认为图片(image)，可配置为pdf（预览时也有按钮切换
     */
    private String previewType = "image";
    /**
     * 是否允许切换
     */
    private boolean disableSwitch = true;

    private boolean disableDown = true;
    /**
     * 外部解压命令
     */
    private String unRar = "/usr/bin/unrar";
    /**
     * 认证方式
     */
    @ToString.Exclude
    private CustomizeCertification certification;

    public String getFileDir() {
        return fileSaveDir + "file/";
    }

    @ToString.Exclude
    private Ftp ftp = new Ftp();

    @ToString.Exclude
    private Watermark watermark = new Watermark();

    @Data
    public static class Cache {
        /**
         * 是否缓存
         */
        private boolean enabled = false;
        private boolean clean = true;
    }

    @Data
    public static class Ftp {
        private String username;
        private String password;
        private String encoding;
    }

    @Data
    public static class Watermark {
        private String txt = "";
        private String xSpace = "10";
        private String ySpace = "10";
        private String font = "微软雅黑";
        private String fontSize = "18px";
        private String color = "black";
        /**
         * 水印透明度，要求设置在大于等于0.005，小于1
         */
        private String alpha = "0.2";
        private String width = "180";
        private String height = "80";
        private String angle = "10";
    }

    @Data
    public static class CustomizeCertification {
        /**
         * 认证地址
         */
        private String resourceUri;

        private boolean bindIp = false;
    }
}
