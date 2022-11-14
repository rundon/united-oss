package com.onefly.united.view.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.onefly.united.common.exception.RenException;
import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.model.FileType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.CollationKey;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yudian-it
 * @date 2017/11/27
 */
@Slf4j
@Component
public class ZipReader {
    static Pattern pattern = Pattern.compile("^\\d+");

    private final FileUtils fileUtils;

    private final ThreadPoolTaskExecutor myExecutor;

    private final KkViewProperties kkViewProperties;

    private final Rar5Utils rar5Utils;

    public ZipReader(FileUtils fileUtils, KkViewProperties kkViewProperties, ThreadPoolTaskExecutor myExecutor, Rar5Utils rar5Utils) {
        this.fileUtils = fileUtils;
        this.kkViewProperties = kkViewProperties;
        this.myExecutor = myExecutor;
        this.rar5Utils = rar5Utils;
    }

    public String readZipFile(String filePath, String fileKey) {
        String archiveSeparator = "/";
        Map<String, FileNode> appender = Maps.newHashMap();
        List<String> imgUrls = Lists.newArrayList();
        String archiveFileName = fileUtils.getFileNameFromPath(filePath);
        try {
            ZipFile zipFile = loadCycleZipFile(filePath);
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            // 排序
            entries = sortZipEntries(entries);
            List<Map<String, ZipArchiveEntry>> entriesToBeExtracted = Lists.newArrayList();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                String fullName = entry.getName();
                int level = fullName.split(archiveSeparator).length;
                // 展示名
                String originName = getLastFileName(fullName, archiveSeparator);
                String childName = level + "_" + transformChina(originName);
                boolean directory = entry.isDirectory();
                if (!directory) {
                    childName = archiveFileName + "_" + transformChina(originName);
                    entriesToBeExtracted.add(Collections.singletonMap(childName, entry));
                    childName = "file/" + childName;
                }
                String parentName = getLast2FileName(fullName, archiveSeparator, archiveFileName);
                parentName = (level - 1) + "_" + parentName;
                FileType type = fileUtils.typeFromUrl(childName);
                if (type.equals(FileType.picture)) {//添加图片文件到图片列表
                    imgUrls.add(childName);
                }
                FileNode node = new FileNode(originName, childName, parentName, new ArrayList<>(), directory, fileKey);
                addNodes(appender, parentName, node);
                appender.put(childName, node);
            }
            // 开启新的线程处理文件解压
            myExecutor.submit(new ZipExtractorWorker(entriesToBeExtracted, zipFile, filePath));
            return new ObjectMapper().writeValueAsString(appender.get(""));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ZipFile loadCycleZipFile(String filePath) throws IOException {
        ZipFile zipFile;
        String ncode = fileUtils.getFileEncodeUTFGBK(filePath);
        try {
            zipFile = new ZipFile(filePath, ncode);
        } catch (IOException e) {
            if ("GBK".equals(ncode)) {
                ncode = StandardCharsets.UTF_8.name();
            }
            zipFile = new ZipFile(filePath, ncode);
        }
        return zipFile;
    }

    /**
     * 中文转换
     *
     * @param originName
     * @return
     */
    private static String transformChina(String originName) {
        if (StringUtils.isNotEmpty(originName)) {
            String[] names = originName.split("\\.");
            if (names.length > 1) {
                String name = StringUtils.join(names, ".", 0, names.length - 1);
                return DigestUtils.md5DigestAsHex(name.getBytes()) + "." + names[names.length - 1];
            } else {
                return DigestUtils.md5DigestAsHex(originName.getBytes());
            }
        } else {
            return originName;
        }
    }

    private Enumeration<ZipArchiveEntry> sortZipEntries(Enumeration<ZipArchiveEntry> entries) {
        List<ZipArchiveEntry> sortedEntries = Lists.newArrayList();
        while (entries.hasMoreElements()) {
            sortedEntries.add(entries.nextElement());
        }
        sortedEntries.sort(Comparator.comparingInt(o -> o.getName().length()));
        return Collections.enumeration(sortedEntries);
    }

    public String unRar(String filePath, String fileKey) throws JsonProcessingException {
        Map<String, FileNode> appender = Maps.newHashMap();
        List<String> imgUrls = Lists.newArrayList();
        try {
            Archive archive = new Archive(new FileInputStream(new File(filePath)));
            List<FileHeader> headers = archive.getFileHeaders();
            headers = sortedHeaders(headers);
            String archiveFileName = fileUtils.getFileNameFromPath(filePath);
            List<Map<String, FileHeader>> headersToBeExtracted = Lists.newArrayList();
            for (FileHeader header : headers) {
                String fullName;
                if (header.isUnicode()) {
                    fullName = header.getFileNameW();
                } else {
                    fullName = header.getFileNameString();
                }
                // 展示名
                String originName = getLastFileName(fullName, "\\");
                String childName = transformChina(originName);
                boolean directory = header.isDirectory();
                if (!directory) {
                    // TODO: 2020/12/28 0028 问题 如果不同文件夹重名岂不是悲剧了
                    childName = archiveFileName + "_" + transformChina(originName);
                    headersToBeExtracted.add(Collections.singletonMap(childName, header));
                    childName = "file/" + childName;
                }
                String parentName = getLast2FileName(fullName, "\\", archiveFileName);
                FileType type = fileUtils.typeFromUrl(childName);
                if (type.equals(FileType.picture)) {//添加图片文件到图片列表
                    imgUrls.add(childName);
                }
                FileNode node = new FileNode(originName, childName, parentName, new ArrayList<>(), directory, fileKey);
                addNodes(appender, parentName, node);
                appender.put(childName, node);
            }
            myExecutor.submit(new RarExtractorWorker(headersToBeExtracted, archive, filePath));
            return new ObjectMapper().writeValueAsString(appender.get(""));
        } catch (RarException | IOException e) {
            if (e instanceof RarException) {
                RarException rarException = (RarException) e;
                if (RarException.RarExceptionType.unsupportedRarArchive == rarException.getType()) {
                    log.error("不支持rar5改为外部解压");
                    String outPath = filePath.split("\\.")[0];
                    if (rar5Utils.unRar5(filePath, outPath)) {
                        String archiveFileName = fileUtils.getFileNameFromPath(filePath);
                        loadAppenderStr(archiveFileName, outPath, fileKey, appender, imgUrls);
                        return new ObjectMapper().writeValueAsString(appender.get(""));
                    }
                } else {
                    rarException.printStackTrace();
                }
            } else {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 解析替换 返回树结构
     *
     * @param outPath
     * @return
     */
    private void loadAppenderStr(String archiveFileName, String outPath, String fileKey, Map<String, FileNode> appender, List<String> imgUrls) {
        File parent = new File(outPath);
        if (!parent.exists()) {
            throw new RenException("文件夹不存在请确认解压是否正常");
        }
        Lists.newArrayList(parent.listFiles()).stream().forEach(fileChild -> encryptionFile(fileChild, parent.getPath(), archiveFileName, fileKey, appender, imgUrls));
    }

    private void encryptionFile(File file, String parent, String archiveFileName, String fileKey, Map<String, FileNode> appender, List<String> imgUrls) {
        String originName = file.getName();
        String fullName = file.getPath().replace(parent, "");
        if (!file.getPath().equals(parent)) {
            fullName = file.getPath().replace(parent + File.separator, "");
            if (file.isDirectory()) {
                fullName = fullName + "/";
            }
            fullName = fullName.replace("\\", "/");
        }
        int level = fullName.split("/").length;
        String childName = level + "_" + transformChina(originName);
        if (!file.isDirectory()) {
            childName = "file/" + archiveFileName + "_" + transformChina(originName);
            String parentName = getLast2FileName(fullName, "/", archiveFileName);
            parentName = (level - 1) + "_" + parentName;
            FileType type = fileUtils.typeFromUrl(childName);
            if (type.equals(FileType.picture)) {//添加图片文件到图片列表
                imgUrls.add(childName);
            }
            FileNode node = new FileNode(originName, childName, parentName, new ArrayList<>(), false, fileKey);
            addNodes(appender, parentName, node);
            appender.put(childName, node);
            File rename = new File(kkViewProperties.getFileSaveDir() + childName);
            file.renameTo(rename);
            return;
        } else {
            String parentName = getLast2FileName(fullName, "/", archiveFileName);
            parentName = (level - 1) + "_" + parentName;
            FileNode node = new FileNode(originName, childName, parentName, new ArrayList<>(), true, fileKey);
            addNodes(appender, parentName, node);
            appender.put(childName, node);
            Lists.newArrayList(file.listFiles()).stream().forEach(fileChild -> encryptionFile(fileChild, parent, archiveFileName, fileKey, appender, imgUrls));
            if (file.isDirectory() && (file.list() == null || file.list().length == 0)) {
                file.delete();
            }
        }
    }

    public String read7zFile(String filePath, String fileKey) {
        String archiveSeparator = "/";
        Map<String, FileNode> appender = Maps.newHashMap();
        List<String> imgUrls = Lists.newArrayList();
        String archiveFileName = fileUtils.getFileNameFromPath(filePath);
        try {
            SevenZFile zipFile = new SevenZFile(new File(filePath));
            Iterable<SevenZArchiveEntry> entries = zipFile.getEntries();
            // 排序
            Enumeration<SevenZArchiveEntry> newEntries = sortSevenZEntries(entries);
            List<Map<String, SevenZArchiveEntry>> entriesToBeExtracted = Lists.newArrayList();
            while (newEntries.hasMoreElements()) {
                SevenZArchiveEntry entry = newEntries.nextElement();
                String fullName = entry.getName();
                int level = fullName.split(archiveSeparator).length;
                // 展示名
                String originName = getLastFileName(fullName, archiveSeparator);
                String childName = level + "_" + transformChina(originName);
                boolean directory = entry.isDirectory();
                if (!directory) {
                    childName = archiveFileName + "_" + transformChina(originName);
                    entriesToBeExtracted.add(Collections.singletonMap(childName, entry));
                    childName = "file/" + childName;
                }
                String parentName = getLast2FileName(fullName, archiveSeparator, archiveFileName);
                parentName = (level - 1) + "_" + parentName;
                FileType type = fileUtils.typeFromUrl(childName);
                if (type.equals(FileType.picture)) {//添加图片文件到图片列表
                    imgUrls.add(childName);
                }
                FileNode node = new FileNode(originName, childName, parentName, new ArrayList<>(), directory, fileKey);
                addNodes(appender, parentName, node);
                appender.put(childName, node);
            }
            // 开启新的线程处理文件解压
            myExecutor.submit(new SevenZExtractorWorker(entriesToBeExtracted, filePath));
            return new ObjectMapper().writeValueAsString(appender.get(""));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private Enumeration<SevenZArchiveEntry> sortSevenZEntries(Iterable<SevenZArchiveEntry> entries) {
        List<SevenZArchiveEntry> sortedEntries = Lists.newArrayList();
        for (SevenZArchiveEntry entry : entries) {
            sortedEntries.add(entry);
        }
        return Collections.enumeration(sortedEntries);
    }

    private void addNodes(Map<String, FileNode> appender, String parentName, FileNode node) {
        if (appender.containsKey(parentName)) {
            appender.get(parentName).getChildList().add(node);
            appender.get(parentName).getChildList().sort(sortComparator);
        } else {
            // 根节点
            FileNode nodeRoot = new FileNode(parentName, parentName, "", new ArrayList<>(), true);
            nodeRoot.getChildList().add(node);
            appender.put("", nodeRoot);
            appender.put(parentName, nodeRoot);
        }
    }

    private List<FileHeader> sortedHeaders(List<FileHeader> headers) {
        List<FileHeader> sortedHeaders = new ArrayList<>();
        Map<Integer, FileHeader> mapHeaders = new TreeMap<>();
        headers.forEach(header -> mapHeaders.put(new Integer(0).equals(header.getFileNameW().length()) ? header.getFileNameString().length() : header.getFileNameW().length(), header));
        for (Map.Entry<Integer, FileHeader> entry : mapHeaders.entrySet()) {
            for (FileHeader header : headers) {
                if (entry.getKey().equals(new Integer(0).equals(header.getFileNameW().length()) ? header.getFileNameString().length() : header.getFileNameW().length())) {
                    sortedHeaders.add(header);
                }
            }
        }
        return sortedHeaders;
    }

    private static String getLast2FileName(String fullName, String seperator, String rootName) {
        if (fullName.endsWith(seperator)) {
            fullName = fullName.substring(0, fullName.length() - 1);
        }
        // 1.获取剩余部分
        int endIndex = fullName.lastIndexOf(seperator);
        String leftPath = fullName.substring(0, endIndex == -1 ? 0 : endIndex);
        if (leftPath.length() > 1) {
            // 2.获取倒数第二个
            return transformChina(getLastFileName(leftPath, seperator));
        } else {
            return rootName;
        }
    }

    private static String getLastFileName(String fullName, String seperator) {
        if (fullName.endsWith(seperator)) {
            fullName = fullName.substring(0, fullName.length() - 1);
        }
        String newName = fullName;
        if (fullName.contains(seperator)) {
            newName = fullName.substring(fullName.lastIndexOf(seperator) + 1);
        }
        return newName;
    }

    public static Comparator<FileNode> sortComparator = new Comparator<FileNode>() {
        final Collator cmp = Collator.getInstance(Locale.US);

        @Override
        public int compare(FileNode o1, FileNode o2) {
            // 判断两个对比对象是否是开头包含数字，如果包含数字则获取数字并按数字真正大小进行排序
            BigDecimal num1, num2;
            if (null != (num1 = isStartNumber(o1))
                    && null != (num2 = isStartNumber(o2))) {
                return num1.subtract(num2).intValue();
            }
            CollationKey c1 = cmp.getCollationKey(o1.getOriginName());
            CollationKey c2 = cmp.getCollationKey(o2.getOriginName());
            return cmp.compare(c1.getSourceString(), c2.getSourceString());
        }
    };

    private static BigDecimal isStartNumber(FileNode src) {
        Matcher matcher = pattern.matcher(src.getOriginName());
        if (matcher.find()) {
            return new BigDecimal(matcher.group());
        }
        return null;
    }

    public static class FileNode {

        private String originName;
        private String fileName;
        private String parentFileName;
        private boolean directory;
        //用于图片预览时寻址
        private String fileKey;
        private List<FileNode> childList;

        public FileNode(String originName, String fileName, String parentFileName, List<FileNode> childList, boolean directory) {
            this.originName = originName;
            this.fileName = fileName;
            this.parentFileName = parentFileName;
            this.childList = childList;
            this.directory = directory;
        }

        public FileNode(String originName, String fileName, String parentFileName, List<FileNode> childList, boolean directory, String fileKey) {
            this.originName = originName;
            this.fileName = fileName;
            this.parentFileName = parentFileName;
            this.childList = childList;
            this.directory = directory;
            this.fileKey = fileKey;
        }

        public String getFileKey() {
            return fileKey;
        }

        public void setFileKey(String fileKey) {
            this.fileKey = fileKey;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getParentFileName() {
            return parentFileName;
        }

        public void setParentFileName(String parentFileName) {
            this.parentFileName = parentFileName;
        }

        public List<FileNode> getChildList() {
            return childList;
        }

        public void setChildList(List<FileNode> childList) {
            this.childList = childList;
        }

        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return "";
            }
        }

        public String getOriginName() {
            return originName;
        }

        public void setOriginName(String originName) {
            this.originName = originName;
        }

        public boolean isDirectory() {
            return directory;
        }

        public void setDirectory(boolean directory) {
            this.directory = directory;
        }
    }

    class ZipExtractorWorker implements Runnable {

        private final List<Map<String, ZipArchiveEntry>> entriesToBeExtracted;
        private final ZipFile zipFile;
        private final String filePath;

        public ZipExtractorWorker(List<Map<String, ZipArchiveEntry>> entriesToBeExtracted, ZipFile zipFile, String filePath) {
            this.entriesToBeExtracted = entriesToBeExtracted;
            this.zipFile = zipFile;
            this.filePath = filePath;
        }

        @Override
        public void run() {
            for (Map<String, ZipArchiveEntry> entryMap : entriesToBeExtracted) {
                String childName = entryMap.keySet().iterator().next();
                ZipArchiveEntry entry = entryMap.values().iterator().next();
                try {
                    extractZipFile(childName, zipFile.getInputStream(entry));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                zipFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (new File(filePath).exists()) {
                new File(filePath).delete();
            }
        }

        private void extractZipFile(String childName, InputStream zipFile) {
            String outPath = kkViewProperties.getFileDir() + childName;
            try (OutputStream ot = new FileOutputStream(outPath)) {
                byte[] inByte = new byte[1024];
                int len;
                while ((-1 != (len = zipFile.read(inByte)))) {
                    ot.write(inByte, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class SevenZExtractorWorker implements Runnable {

        private final List<Map<String, SevenZArchiveEntry>> entriesToBeExtracted;
        private final String filePath;

        public SevenZExtractorWorker(List<Map<String, SevenZArchiveEntry>> entriesToBeExtracted, String filePath) {
            this.entriesToBeExtracted = entriesToBeExtracted;
            this.filePath = filePath;
        }

        @Override
        public void run() {
            try {
                SevenZFile sevenZFile = new SevenZFile(new File(filePath));
                SevenZArchiveEntry entry = sevenZFile.getNextEntry();
                while (entry != null) {
                    if (entry.isDirectory()) {
                        entry = sevenZFile.getNextEntry();
                        continue;
                    }
                    String childName = "default_file";
                    SevenZArchiveEntry entry1;
                    for (Map<String, SevenZArchiveEntry> entryMap : entriesToBeExtracted) {
                        childName = entryMap.keySet().iterator().next();
                        entry1 = entryMap.values().iterator().next();
                        if (entry.getName().equals(entry1.getName())) {
                            break;
                        }
                    }
                    FileOutputStream out = new FileOutputStream(kkViewProperties.getFileDir() + childName);
                    byte[] content = new byte[(int) entry.getSize()];
                    sevenZFile.read(content, 0, content.length);
                    out.write(content);
                    out.close();
                    entry = sevenZFile.getNextEntry();
                }
                sevenZFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (new File(filePath).exists()) {
                new File(filePath).delete();
            }
        }
    }

    class RarExtractorWorker implements Runnable {
        private final List<Map<String, FileHeader>> headersToBeExtracted;
        private final Archive archive;
        /**
         * 用以删除源文件
         */
        private final String filePath;

        public RarExtractorWorker(List<Map<String, FileHeader>> headersToBeExtracted, Archive archive, String filePath) {
            this.headersToBeExtracted = headersToBeExtracted;
            this.archive = archive;
            this.filePath = filePath;
        }

        @Override
        public void run() {
            for (Map<String, FileHeader> entryMap : headersToBeExtracted) {
                String childName = entryMap.keySet().iterator().next();
                extractRarFile(childName, entryMap.values().iterator().next(), archive);
            }
            try {
                archive.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (new File(filePath).exists()) {
                new File(filePath).delete();
            }
        }

        private void extractRarFile(String childName, FileHeader header, Archive archive) {
            String outPath = kkViewProperties.getFileDir() + childName;
            try (OutputStream ot = new FileOutputStream(outPath)) {
                archive.extractFile(header, ot);
            } catch (IOException | RarException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws JsonProcessingException {
        Map<String, FileNode> appender = Maps.newHashMap();
        List<String> imgUrls = Lists.newArrayList();
        KkViewProperties kkViewProperties = new KkViewProperties();
        FileUtils fileUtils = new FileUtils(kkViewProperties);
        ZipReader zipReader = new ZipReader(fileUtils, kkViewProperties, null, null);
        String archiveFileName = fileUtils.getFileNameFromPath("D:/data/file/b16a89f1-e2d6-4330-90a5-7acd026e68ca.zip");
        zipReader.loadAppenderStr(archiveFileName, "D:/data/file/b16a89f1-e2d6-4330-90a5-7acd026e68ca", "d1493891e8f0400d87290335249103a2.zip", appender, imgUrls);
        System.out.println(new ObjectMapper().writeValueAsString(appender.get("")));
    }
}
