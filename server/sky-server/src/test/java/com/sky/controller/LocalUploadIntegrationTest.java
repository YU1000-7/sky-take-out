package com.sky.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.common.utils.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * sky.minio.enabled=false 的端到端校验：真实落盘 + 通过 /uploads/** 取回文件。
 * 证明「没装 MinIO 也能上传图片」这条降级路径是通的。
 */
@SpringBootTest(properties = {
        "sky.minio.enabled=false",
        "sky.minio.local-dir=target/test-uploads",
        "sky.minio.local-base-url=http://localhost:8080/uploads"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalUploadIntegrationTest {

    private static final Path UPLOAD_ROOT = Paths.get("target/test-uploads").toAbsolutePath().normalize();
    private static final String BASE_URL = "http://localhost:8080/uploads";
    private static final String CONTENT = "<svg>local-upload</svg>";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private StringRedisTemplate redisTemplate;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        cleanUploadDir();
        adminToken = jwtUtil.createAdminToken(1L);
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanUploadDir();
    }

    @Test
    void shouldStoreUploadOnDiskAndServeItBack() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "yuxiang.svg", "image/svg+xml", CONTENT.getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/admin/common/upload")
                        .file(file)
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();

        String url = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").asText();
        assertTrue(url.startsWith(BASE_URL + "/images/"), url);

        String objectName = url.substring((BASE_URL + "/").length());
        Path saved = UPLOAD_ROOT.resolve(objectName);
        assertTrue(Files.exists(saved), "应真实落盘：" + saved);

        mockMvc.perform(get("/uploads/" + objectName))
                .andExpect(status().isOk())
                .andExpect(content().string(CONTENT));
    }

    private void cleanUploadDir() throws Exception {
        if (!Files.exists(UPLOAD_ROOT)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(UPLOAD_ROOT)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // 清理失败不影响断言
                }
            });
        }
    }
}
