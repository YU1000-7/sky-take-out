package com.sky.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.common.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 分类接口集成测试：覆盖新增、分页筛选、修改（含类型不可改）、删除规则和按类型查列表。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryApiIntegrationTest {

    private static final Long DISH_CATEGORY_ID = 10L;
    private static final Long SETMEAL_CATEGORY_ID = 11L;
    private static final Long EMPTY_CATEGORY_ID = 12L;

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

    private String adminToken;

    @BeforeEach
    void setUp() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        jdbcTemplate.update("delete from dish_flavor");
        jdbcTemplate.update("delete from setmeal_dish");
        jdbcTemplate.update("delete from setmeal");
        jdbcTemplate.update("delete from dish");
        jdbcTemplate.update("delete from category");

        insertCategory(DISH_CATEGORY_ID, 1, "热菜", 2);
        insertCategory(SETMEAL_CATEGORY_ID, 2, "套餐分类", 1);
        insertCategory(EMPTY_CATEGORY_ID, 1, "凉菜", 3);

        adminToken = jwtUtil.createAdminToken(1L);
    }

    @Test
    void shouldCreateCategoryAndFindItByPage() throws Exception {
        mockMvc.perform(post("/admin/category").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody(null, 1, "甜品", 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/admin/category/page").header("token", adminToken)
                        .param("page", "1").param("pageSize", "10").param("name", "甜品"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("甜品"))
                .andExpect(jsonPath("$.data.records[0].type").value(1))
                .andExpect(jsonPath("$.data.records[0].sort").value(5))
                .andExpect(jsonPath("$.data.records[0].status").value(1));
    }

    @Test
    void shouldRejectDuplicateNameInSameType() throws Exception {
        mockMvc.perform(post("/admin/category").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody(null, 1, "热菜", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("同类型下分类名称已存在"));
    }

    @Test
    void shouldAllowSameNameInDifferentType() throws Exception {
        // 库里已有 type=1 的「热菜」，再建 type=2 的同名分类应当成功
        mockMvc.perform(post("/admin/category").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody(null, 2, "热菜", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/admin/category/page").header("token", adminToken)
                        .param("page", "1").param("pageSize", "10")
                        .param("name", "热菜").param("type", "2"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldUpdateCategoryNameAndSort() throws Exception {
        mockMvc.perform(put("/admin/category").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody(DISH_CATEGORY_ID, 1, "招牌热菜", 9)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/admin/category/page").header("token", adminToken)
                        .param("page", "1").param("pageSize", "10").param("name", "招牌热菜"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(DISH_CATEGORY_ID))
                .andExpect(jsonPath("$.data.records[0].sort").value(9));
    }

    @Test
    void shouldRejectChangingCategoryType() throws Exception {
        mockMvc.perform(put("/admin/category").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody(DISH_CATEGORY_ID, 2, "热菜", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("分类类型不允许修改"));
    }

    @Test
    void shouldDeleteEmptyCategory() throws Exception {
        mockMvc.perform(delete("/admin/category").header("token", adminToken)
                        .param("id", String.valueOf(EMPTY_CATEGORY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/admin/category/page").header("token", adminToken)
                        .param("page", "1").param("pageSize", "10").param("name", "凉菜"))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void shouldRejectDeletingCategoryWithDish() throws Exception {
        insertDish(1L, "鱼香肉丝", DISH_CATEGORY_ID, 1);

        mockMvc.perform(delete("/admin/category").header("token", adminToken)
                        .param("id", String.valueOf(DISH_CATEGORY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该分类下还有菜品，不能删除"));
    }

    @Test
    void shouldRejectDeletingCategoryWithSetmeal() throws Exception {
        insertSetmeal(1L, SETMEAL_CATEGORY_ID, 1);

        mockMvc.perform(delete("/admin/category").header("token", adminToken)
                        .param("id", String.valueOf(SETMEAL_CATEGORY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该分类下还有套餐，不能删除"));
    }

    @Test
    void shouldListCategoriesByTypeOrderedBySort() throws Exception {
        // 同类目下 sort：套餐分类(1) < 热菜(2)，凉菜(3) 也是 type=1
        mockMvc.perform(get("/admin/category/list").header("token", adminToken)
                        .param("type", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("热菜"))
                .andExpect(jsonPath("$.data[1].name").value("凉菜"))
                .andExpect(jsonPath("$.data[0].type").value(1))
                .andExpect(jsonPath("$.data[1].type").value(1));
    }

    @Test
    void shouldRejectMissingCategoryOnUpdate() throws Exception {
        mockMvc.perform(put("/admin/category").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody(999999L, 1, "不存在", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("分类不存在"));
    }

    @Test
    void shouldRejectCategoryPageWithoutToken() throws Exception {
        mockMvc.perform(get("/admin/category/page").param("page", "1").param("pageSize", "10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private void insertCategory(Long id, int type, String name, int sort) {
        jdbcTemplate.update("insert into category(id, type, name, sort, status, create_time, update_time)"
                + " values (?, ?, ?, ?, 1, now(), now())", id, type, name, sort);
    }

    private void insertDish(Long id, String name, Long categoryId, int status) {
        jdbcTemplate.update("insert into dish(id, name, category_id, price, image, description, status,"
                        + " create_time, update_time) values (?, ?, ?, ?, null, ?, ?, now(), now())",
                id, name, categoryId, 3800, name, status);
    }

    private void insertSetmeal(Long id, Long categoryId, int status) {
        jdbcTemplate.update("insert into setmeal(id, category_id, name, price, status, description, image,"
                        + " create_time, update_time) values (?, ?, ?, ?, ?, ?, null, now(), now())",
                id, categoryId, "双人套餐", 6800, status, "双人套餐");
    }

    private String categoryBody(Long id, int type, String name, int sort) throws Exception {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        if (id != null) {
            body.put("id", id);
        }
        body.put("type", type);
        body.put("name", name);
        body.put("sort", sort);
        return objectMapper.writeValueAsString(body);
    }
}
