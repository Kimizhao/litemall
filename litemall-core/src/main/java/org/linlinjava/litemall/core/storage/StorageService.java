package org.linlinjava.litemall.core.storage;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.storage.config.StorageProperties;
import org.linlinjava.litemall.core.util.CharUtil;
import org.linlinjava.litemall.db.domain.LitemallStorage;
import org.linlinjava.litemall.db.service.LitemallStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * 提供存储服务类，所有存储服务均由该类对外提供
 */
public class StorageService {
    private final Log logger = LogFactory.getLog(StorageService.class);

    private String active;
    private Storage storage;
    private StorageProperties.Compress compress;

    @Autowired
    private LitemallStorageService litemallStorageService;

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public StorageProperties.Compress getCompress() {
        return compress;
    }

    public void setCompress(StorageProperties.Compress compress) {
        this.compress = compress;
    }

    /**
     * 存储一个文件对象
     *
     * @param inputStream   文件输入流
     * @param contentLength 文件长度
     * @param contentType   文件类型
     * @param fileName      文件索引名
     */
    public LitemallStorage store(InputStream inputStream, long contentLength, String contentType, String fileName) {
        if (compress != null && compress.isEnabled() && contentLength > compress.getThresholdSize() && isCompressibleImage(contentType, fileName)) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Thumbnails.of(inputStream)
                        .size(compress.getMaxDimension(), compress.getMaxDimension())
                        .outputQuality(compress.getQuality())
                        .toOutputStream(baos);
                byte[] bytes = baos.toByteArray();
                inputStream = new ByteArrayInputStream(bytes);
                contentLength = bytes.length;
                logger.info("图片解压缩优化完成: " + fileName + ", 优化后文件大小: " + contentLength + " 字节");
            } catch (Exception e) {
                logger.warn("图片压缩处理异常 (" + fileName + ")，已自动降级为保存原始文件", e);
            }
        }

        String key = generateKey(fileName);
        storage.store(inputStream, contentLength, contentType, key);

        String url = generateUrl(key);
        LitemallStorage storageInfo = new LitemallStorage();
        storageInfo.setName(fileName);
        storageInfo.setSize((int) contentLength);
        storageInfo.setType(contentType);
        storageInfo.setKey(key);
        storageInfo.setUrl(url);
        litemallStorageService.add(storageInfo);

        return storageInfo;
    }

    private boolean isCompressibleImage(String contentType, String fileName) {
        String type = contentType != null ? contentType.toLowerCase() : "";
        String name = fileName != null ? fileName.toLowerCase() : "";

        boolean isImage = type.startsWith("image/jpeg") || type.startsWith("image/jpg") || type.startsWith("image/png")
                || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");

        boolean isGif = type.contains("gif") || name.endsWith(".gif");

        return isImage && !isGif;
    }

    private String generateKey(String originalFilename) {
        int index = originalFilename.lastIndexOf('.');
        String suffix = originalFilename.substring(index);

        String key = null;
        LitemallStorage storageInfo = null;

        do {
            key = CharUtil.getRandomString(20) + suffix;
            storageInfo = litemallStorageService.findByKey(key);
        }
        while (storageInfo != null);

        return key;
    }

    public Stream<Path> loadAll() {
        return storage.loadAll();
    }

    public Path load(String keyName) {
        return storage.load(keyName);
    }

    public Resource loadAsResource(String keyName) {
        return storage.loadAsResource(keyName);
    }

    public void delete(String keyName) {
        storage.delete(keyName);
    }

    private String generateUrl(String keyName) {
        return storage.generateUrl(keyName);
    }
}
