package com.sky.service;

import com.sky.common.properties.MinioProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * sky.minio.enabled=false 时的本地降级单测：不依赖 MinIO，验证落盘位置与 URL 拼装。
 */
class MinioStorageServiceLocalModeTest {

    private static final String BASE_URL = "http://127.0.0.1:8080/uploads";

    @TempDir
    Path tempDir;

    private MinioProperties properties;
    private MinioStorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new MinioProperties();
        properties.setEnabled(false);
        properties.setLocalDir(tempDir.toString());
        properties.setLocalBaseUrl(BASE_URL);
        // 本地模式不会用到 MinioClient，这里传 null 即可
        storageService = new MinioStorageService(null, properties);
    }

    @Test
    void shouldSaveUnderImagesDateDirectoryAndReturnUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "yuxiang.svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8));

        String url = storageService.upload(file);

        String today = LocalDate.now().toString();
        assertTrue(url.startsWith(BASE_URL + "/images/" + today + "/"), url);
        assertTrue(url.endsWith(".svg"), url);

        String objectName = url.substring((BASE_URL + "/").length());
        Path saved = tempDir.resolve(objectName);
        assertTrue(Files.exists(saved), "文件应真实落盘：" + saved);
        assertEquals("<svg/>", Files.readString(saved));
    }

    @Test
    void shouldKeepExtensionFromOriginalName() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "dish.png", "image/png", "x".getBytes(StandardCharsets.UTF_8));

        assertTrue(storageService.upload(file).endsWith(".png"));
    }

    @Test
    void shouldNotUseOriginalNameAsFileName() throws Exception {
        // 只取扩展名，原文件名不参与路径，天然避免 ../ 穿越
        String url = storageService.upload(new MockMultipartFile(
                "file", "../../evil.svg", "image/svg+xml", "x".getBytes(StandardCharsets.UTF_8)));

        String objectName = url.substring((BASE_URL + "/").length());
        assertTrue(!objectName.contains(".."), "路径里不应出现 ..：" + objectName);
        assertEquals(1, countFiles(tempDir), "只应写入一个文件");
    }

    @Test
    void shouldGenerateDifferentNamesForSameOriginalName() {
        byte[] content = "same".getBytes(StandardCharsets.UTF_8);
        String first = storageService.upload(new MockMultipartFile("file", "a.jpg", "image/jpeg", content));
        String second = storageService.upload(new MockMultipartFile("file", "a.jpg", "image/jpeg", content));

        assertNotEquals(first, second, "同名文件不能互相覆盖");
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storageService.upload(empty));
        assertEquals("上传文件不能为空", exception.getMessage());
    }

    @Test
    void shouldTrimTrailingSlashOfBaseUrl() {
        properties.setLocalBaseUrl(BASE_URL + "/");

        String url = storageService.upload(new MockMultipartFile(
                "file", "a.jpg", "image/jpeg", "x".getBytes(StandardCharsets.UTF_8)));

        assertTrue(url.startsWith(BASE_URL + "/images/"), url);
        assertTrue(!url.contains("//images"), "不应出现双斜杠：" + url);
    }

    @Test
    void shouldHandleFileWithoutExtension() {
        String url = storageService.upload(new MockMultipartFile(
                "file", "noext", "application/octet-stream", "x".getBytes(StandardCharsets.UTF_8)));

        assertTrue(url.startsWith(BASE_URL + "/images/"), url);
        assertTrue(!url.endsWith("."), "无扩展名时不应多出点：" + url);
    }

    @Test
    void shouldCreateDateDirectoryAutomatically() throws Exception {
        storageService.upload(new MockMultipartFile(
                "file", "a.jpg", "image/jpeg", "x".getBytes(StandardCharsets.UTF_8)));

        Path dateDir = tempDir.resolve("images").resolve(LocalDate.now().toString());
        assertTrue(Files.isDirectory(dateDir), "应按日期建目录：" + dateDir);
    }

    private long countFiles(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = paths.filter(Files::isRegularFile).toList();
            return files.size();
        }
    }
}
