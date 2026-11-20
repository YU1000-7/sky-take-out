package com.sky.controller;

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
 * 菜品接口集成测试：覆盖新增（含口味）、改口味整体覆盖、分页、批量删除、起停售规则和 C 端查询。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DishApiIntegrationTest {

    private static final Long DISH_CATEGORY_ID = 20L;
    private static final Long SETMEAL_CATEGORY_ID = 21L;

    private static final String TWO_FLAVORS = """
            {"name": "辣度", "value": "微辣,中辣,特辣"},
            {"name": "忌口", "value": "不要香菜,不要葱"}
            """;

    @Autowired
    private MockMvc mockMvc;
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

        jdbcTemplate.update("insert into category(id, type, name, sort, status, create_time, update_time)"
                + " values (?, 1, '热菜', 1, 1, now(), now())", DISH_CATEGORY_ID);
        jdbcTemplate.update("insert into category(id, type, name, sort, status, create_time, update_time)"
                + " values (?, 2, '套餐分类', 1, 1, now(), now())", SETMEAL_CATEGORY_ID);

        adminToken = jwtUtil.createAdminToken(1L);
    }

    @Test
    void shouldCreateDishWithFlavorsAndReturnDetail() throws Exception {
        long dishId = createDish("鱼香肉丝", DISH_CATEGORY_ID, 3800, 1, TWO_FLAVORS);

        mockMvc.perform(get("/admin/dish/" + dishId).header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("鱼香肉丝"))
                .andExpect(jsonPath("$.data.categoryName").value("热菜"))
                .andExpect(jsonPath("$.data.price").value(3800))
                .andExpect(jsonPath("$.data.flavors.length()").value(2))
                .andExpect(jsonPath("$.data.flavors[0].name").value("辣度"))
                .andExpect(jsonPath("$.data.flavors[0].value").value("微辣,中辣,特辣"));
    }

    @Test
    void shouldReplaceFlavorsInsteadOfAppendingOnUpdate() throws Exception {
        long dishId = createDish("鱼香肉丝", DISH_CATEGORY_ID, 3800, 1, TWO_FLAVORS);

        // 只留一组口味，值改掉：结果必须是 1 条，而不是 3 条
        String body = dishBody(dishId, "鱼香肉丝", DISH_CATEGORY_ID, 3800, 1,
                """
                {"name": "辣度", "value": "特辣"}
                """);
        mockMvc.perform(put("/admin/dish").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/admin/dish/" + dishId).header("token", adminToken))
                .andExpect(jsonPath("$.data.flavors.length()").value(1))
                .andExpect(jsonPath("$.data.flavors[0].value").value("特辣"));
    }

    @Test
    void shouldRejectDuplicateDishNameInSameCategory() throws Exception {
        createDish("鱼香肉丝", DISH_CATEGORY_ID, 3800, 1, "");

        mockMvc.perform(post("/admin/dish").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dishBody(null, "鱼香肉丝", DISH_CATEGORY_ID, 4200, 1, "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该分类下已存在同名菜品"));
    }

    @Test
    void shouldPageDishesWithCategoryNameAndStatusFilter() throws Exception {
        createDish("鱼香肉丝", DISH_CATEGORY_ID, 3800, 1, "");
        createDish("麻婆豆腐", DISH_CATEGORY_ID, 2800, 0, "");

        mockMvc.perform(get("/admin/dish/page").header("token", adminToken)
                        .param("page", "1").param("pageSize", "10")
                        .param("categoryId", String.valueOf(DISH_CATEGORY_ID))
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("鱼香肉丝"))
                .andExpect(jsonPath("$.data.records[0].categoryName").value("热菜"))
                .andExpect(jsonPath("$.data.records[0].status").value(1));
    }

    @Test
    void shouldBatchDeleteStoppedDishes() throws Exception {
        long first = createDish("麻婆豆腐", DISH_CATEGORY_ID, 2800, 0, "");
        long second = createDish("水煮牛肉", DISH_CATEGORY_ID, 5800, 0, "");

        mockMvc.perform(delete("/admin/dish").header("token", adminToken)
                        .param("ids", first + "," + second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Integer remaining = jdbcTemplate.queryForObject("select count(*) from dish", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(0, remaining);
    }

    @Test
    void shouldRejectDeletingOnSaleDish() throws Exception {
        long dishId = createDish("鱼香肉丝", DISH_CATEGORY_ID, 3800, 1, "");

        mockMvc.perform(delete("/admin/dish").header("token", adminToken)
                        .param("ids", String.valueOf(dishId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("菜品【鱼香肉丝】正在起售，请先停售"));
    }

    @Test
    void shouldRejectDeletingDishReferencedBySetmeal() throws Exception {
        long dishId = createDish("鱼香肉丝", DISH_CATEGORY_ID, 3800, 0, "");
        long setmealId = insertSetmeal(1L, 1, dishId);

        mockMvc.perform(delete("/admin/dish").header("token", adminToken)
                        .param("ids", String.valueOf(dishId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("菜品【鱼香肉丝】已被套餐关联，不能删除"));

        org.junit.jupiter.api.Assertions.assertNotNull(setmealId);
    }

    @Test
    void shouldStartAndStopDish() throws Exception {
        long dishId = createDish("鱼香肉丝", DISH_CATEGORY_ID, 3800, 1, "");

        mockMvc.perform(put("/admin/dish/status/0").header("token", adminToken)
                        .param("id", String.valueOf(dishId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/admin/dish/" + dishId).header("token", adminToken))
                .andExpect(jsonPath("$.data.status").value(0));

        mockMvc.perform(put("/admin/dish/status/1").header("token", adminToken)
                        .param("id", String.valueOf(dishId)))
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/admin/dish/" + dishId).header("token", adminToken))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    void shouldRejectStoppingDishReferencedByOnSaleSetmeal() throws Exception {
        long dishId = createDish("鱼香肉丝", DISH_CATEGORY_ID, 3800, 1, "");
        insertSetmeal(1L, 1, dishId);

        mockMvc.perform(put("/admin/dish/status/0").header("token", adminToken)
                        .param("id", String.valueOf(dishId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("该菜品已被起售中的套餐关联，不能停售"));
    }

    @Test
    void shouldExposeOnlyOnSaleDishesToCustomerWithoutToken() throws Exception {
        createDish("鱼香肉丝", DISH_CATEGORY_ID, 3800, 1, """
                {"name": "辣度", "value": "微辣,中辣,特辣"}
                """);
        createDish("麻婆豆腐", DISH_CATEGORY_ID, 2800, 0, "");

        mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(DISH_CATEGORY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("鱼香肉丝"))
                .andExpect(jsonPath("$.data[0].flavors.length()").value(1));
    }

    @Test
    void shouldRejectInvalidDishStatus() throws Exception {
        long dishId = createDish("鱼香肉丝", DISH_CATEGORY_ID, 3800, 1, "");

        mockMvc.perform(put("/admin/dish/status/9").header("token", adminToken)
                        .param("id", String.valueOf(dishId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("菜品状态只能是 0 或 1"));
    }

    private long createDish(String name, Long categoryId, int price, int status, String flavors) throws Exception {
        mockMvc.perform(post("/admin/dish").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dishBody(null, name, categoryId, price, status, flavors)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Long id = jdbcTemplate.queryForObject("select id from dish where name = ?", Long.class, name);
        org.junit.jupiter.api.Assertions.assertNotNull(id);
        return id;
    }

    private long insertSetmeal(Long id, int status, Long dishId) {
        jdbcTemplate.update("insert into setmeal(id, category_id, name, price, status, description, image,"
                        + " create_time, update_time) values (?, ?, ?, ?, ?, ?, null, now(), now())",
                id, SETMEAL_CATEGORY_ID, "双人套餐", 6800, status, "双人套餐");
        jdbcTemplate.update("insert into setmeal_dish(setmeal_id, dish_id, name, price, copies)"
                + " values (?, ?, ?, ?, 1)", id, dishId, "鱼香肉丝", 3800);
        return id;
    }

    private String dishBody(Long id, String name, Long categoryId, int price, int status, String flavors) {
        String idPart = (id == null) ? "" : "\"id\": " + id + ",\n              ";
        String flavorsPart = (flavors == null || flavors.isBlank()) ? "" : flavors;
        return """
                {
                  %s"name": "%s",
                  "categoryId": %d,
                  "price": %d,
                  "image": "/dishes/test.svg",
                  "description": "%s",
                  "status": %d,
                  "flavors": [%s]
                }
                """.formatted(idPart, name, categoryId, price, name, status, flavorsPart);
    }
}
