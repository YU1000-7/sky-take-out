package com.sky.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.common.utils.JwtUtil;
import com.sky.service.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 图片上传 → 保存 → 列表回显 的接口级用例。
 *
 * <p>存储层用 MockBean 打桩：本用例验证的是「上传接口返回的 url 能被菜品保存并原样回显」这条链路，
 * 真实 MinIO 的读写属于环境依赖（对应待补清单里王的 MinIO 降级开关），不在本用例范围内。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DishImageFlowTest {

    private static final Long CATEGORY_ID = 30L;
    private static final String UPLOADED_URL = "http://localhost:9000/sky-take-out/dish/2026/11/uploaded.svg";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private StringRedisTemplate redisTemplate;
    @MockBean
    private MinioStorageService storageService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        jdbcTemplate.update("delete from dish_flavor");
        jdbcTemplate.update("delete from setmeal_dish");
        jdbcTemplate.update("delete from dish");
        jdbcTemplate.update("delete from category");
        jdbcTemplate.update("insert into category(id, type, name, sort, status, create_time, update_time)"
                + " values (?, 1, '热菜', 1, 1, now(), now())", CATEGORY_ID);

        adminToken = jwtUtil.createAdminToken(1L);
    }

    @Test
    void shouldUploadImageThenAttachItToDish() throws Exception {
        when(storageService.upload(any())).thenReturn(UPLOADED_URL);

        MockMultipartFile file = new MockMultipartFile(
                "file", "yuxiang.svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8));

        MvcResult uploadResult = mockMvc.perform(multipart("/admin/common/upload")
                        .file(file)
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();

        String imageUrl = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").asText();
        assertEquals(UPLOADED_URL, imageUrl);

        // 用上传返回的地址新增菜品
        String body = """
                {
                  "name": "鱼香肉丝",
                  "categoryId": %d,
                  "price": 3800,
                  "image": "%s",
                  "description": "经典川菜",
                  "status": 1,
                  "flavors": [{"name": "辣度", "value": "微辣,中辣,特辣"}]
                }
                """.formatted(CATEGORY_ID, imageUrl);
        mockMvc.perform(post("/admin/dish").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 列表回显：image 字段原样带出来，前端 <el-image> 直接可用
        mockMvc.perform(get("/admin/dish/page").header("token", adminToken)
                        .param("page", "1").param("pageSize", "10")
                        .param("categoryId", String.valueOf(CATEGORY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].image").value(UPLOADED_URL));

        // C 端首页同样能拿到图片地址
        mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(CATEGORY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].image").value(UPLOADED_URL));
    }

    @Test
    void shouldRejectUploadWithoutToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "yuxiang.svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/admin/common/upload").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
