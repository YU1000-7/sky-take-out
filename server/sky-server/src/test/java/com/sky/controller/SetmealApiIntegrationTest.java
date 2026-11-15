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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 套餐接口集成测试：覆盖新增、分页、详情、修改整体替换、起停售、删除规则和 C 端查询。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SetmealApiIntegrationTest {

    private static final Long SETMEAL_CATEGORY_ID = 10L;
    private static final Long DISH_CATEGORY_ID = 20L;
    private static final Long DISH_1 = 1L;
    private static final Long DISH_2 = 2L;
    private static final Long DISH_STOPPED = 3L;

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

        jdbcTemplate.update("delete from setmeal_dish");
        jdbcTemplate.update("delete from setmeal");
        jdbcTemplate.update("delete from shopping_cart");
        jdbcTemplate.update("delete from order_detail");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from dish");
        jdbcTemplate.update("delete from category");

        jdbcTemplate.update("insert into category(id, type, name, sort, status, create_time, update_time)"
                + " values (?, 2, '套餐分类', 1, 1, now(), now())", SETMEAL_CATEGORY_ID);
        jdbcTemplate.update("insert into category(id, type, name, sort, status, create_time, update_time)"
                + " values (?, 1, '热菜', 1, 1, now(), now())", DISH_CATEGORY_ID);

        insertDish(DISH_1, "鱼香肉丝", 3800, 1);
        insertDish(DISH_2, "宫保鸡丁", 4200, 1);
        insertDish(DISH_STOPPED, "停售小炒", 2800, 0);

        adminToken = jwtUtil.createAdminToken(1L);
    }

    @Test
    void shouldCreatePageAndQuerySetmealDetail() throws Exception {
        long setmealId = createSetmeal(1);

        mockMvc.perform(get("/admin/setmeal/page").header("token", adminToken)
                        .param("page", "1").param("pageSize", "10").param("name", "双人"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("双人特惠餐"))
                .andExpect(jsonPath("$.data.records[0].categoryName").value("套餐分类"))
                .andExpect(jsonPath("$.data.records[0].status").value(1));

        mockMvc.perform(get("/admin/setmeal/{id}", setmealId).header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.setmealDishes.length()").value(2))
                .andExpect(jsonPath("$.data.setmealDishes[0].dishName").value("鱼香肉丝"))
                .andExpect(jsonPath("$.data.setmealDishes[0].copies").value(1))
                .andExpect(jsonPath("$.data.setmealDishes[0].dishStatus").value(1))
                .andExpect(jsonPath("$.data.setmealDishes[1].dishName").value("宫保鸡丁"))
                .andExpect(jsonPath("$.data.setmealDishes[1].copies").value(2));
    }

    @Test
    void shouldReplaceSetmealDishesWhenUpdate() throws Exception {
        long setmealId = createSetmeal(1);

        String updateBody = """
                {
                  "id": %d,
                  "name": "单人特惠餐",
                  "categoryId": %d,
                  "price": 3800,
                  "image": "http://example.com/setmeal.jpg",
                  "description": "单人套餐",
                  "status": 0,
                  "setmealDishes": [{"dishId": %d, "copies": 3}]
                }
                """.formatted(setmealId, SETMEAL_CATEGORY_ID, DISH_1);

        mockMvc.perform(put("/admin/setmeal").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("修改成功"));

        mockMvc.perform(get("/admin/setmeal/{id}", setmealId).header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("单人特惠餐"))
                .andExpect(jsonPath("$.data.setmealDishes.length()").value(1))
                .andExpect(jsonPath("$.data.setmealDishes[0].dishId").value(DISH_1.intValue()))
                .andExpect(jsonPath("$.data.setmealDishes[0].copies").value(3));

        Integer relationCount = jdbcTemplate.queryForObject(
                "select count(1) from setmeal_dish where setmeal_id = ?", Integer.class, setmealId);
        assertEquals(1, relationCount.intValue());
    }

    @Test
    void shouldRejectOnSaleWhenDishesStopped() throws Exception {
        String body = """
                {
                  "name": "含停售菜品的套餐",
                  "categoryId": %d,
                  "price": 5000,
                  "status": 1,
                  "setmealDishes": [{"dishId": %d, "copies": 1}]
                }
                """.formatted(SETMEAL_CATEGORY_ID, DISH_STOPPED);

        mockMvc.perform(post("/admin/setmeal").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("停售")));

        Integer count = jdbcTemplate.queryForObject("select count(1) from setmeal", Integer.class);
        assertEquals(0, count.intValue(), "校验失败时套餐写入必须整体回滚");
    }

    @Test
    void shouldRejectDishCategory() throws Exception {
        String body = """
                {
                  "name": "分类错误的套餐",
                  "categoryId": %d,
                  "price": 5000,
                  "setmealDishes": [{"dishId": %d, "copies": 1}]
                }
                """.formatted(DISH_CATEGORY_ID, DISH_1);

        mockMvc.perform(post("/admin/setmeal").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("不是套餐分类")));
    }

    @Test
    void shouldExposeSetmealToCustomerOnlyWhenOnSale() throws Exception {
        long setmealId = createSetmeal(0);

        mockMvc.perform(get("/user/setmeal/list").header("token", userToken())
                        .param("categoryId", String.valueOf(SETMEAL_CATEGORY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(put("/admin/setmeal/status/1").header("token", adminToken)
                        .param("id", String.valueOf(setmealId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/user/setmeal/list").header("token", userToken())
                        .param("categoryId", String.valueOf(SETMEAL_CATEGORY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("双人特惠餐"))
                .andExpect(jsonPath("$.data[0].price").value(6800));

        mockMvc.perform(get("/user/setmeal/dish/{id}", setmealId).header("token", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(DISH_1.intValue()))
                .andExpect(jsonPath("$.data[0].name").value("鱼香肉丝"));
    }

    @Test
    void shouldOnlyDeleteStoppedSetmeal() throws Exception {
        long setmealId = createSetmeal(1);

        mockMvc.perform(delete("/admin/setmeal").header("token", adminToken)
                        .param("ids", String.valueOf(setmealId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("起售")));

        mockMvc.perform(put("/admin/setmeal/status/0").header("token", adminToken)
                        .param("id", String.valueOf(setmealId)))
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(delete("/admin/setmeal").header("token", adminToken)
                        .param("ids", String.valueOf(setmealId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("删除成功"));

        Integer relationCount = jdbcTemplate.queryForObject(
                "select count(1) from setmeal_dish where setmeal_id = ?", Integer.class, setmealId);
        assertEquals(0, relationCount.intValue(), "删除套餐时必须同时清理套餐菜品关联");
    }

    @Test
    void shouldRejectDeleteWhenSetmealInCartOrActiveOrder() throws Exception {
        long setmealId = createSetmeal(0);

        jdbcTemplate.update("insert into shopping_cart(user_id, setmeal_id, number, amount, name, create_time)"
                + " values (1, ?, 1, 6800, '双人特惠餐', now())", setmealId);

        mockMvc.perform(delete("/admin/setmeal").header("token", adminToken)
                        .param("ids", String.valueOf(setmealId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("购物车")));

        jdbcTemplate.update("delete from shopping_cart");
        jdbcTemplate.update("insert into orders(number, status, user_id, address_book_id, amount, order_time)"
                + " values ('202611040001', 2, 1, 1, 6800, now())");
        Long orderId = jdbcTemplate.queryForObject("select id from orders", Long.class);
        jdbcTemplate.update("insert into order_detail(name, image, order_id, setmeal_id, number, amount)"
                + " values ('双人特惠餐', null, ?, ?, 1, 6800)", orderId, setmealId);

        mockMvc.perform(delete("/admin/setmeal").header("token", adminToken)
                        .param("ids", String.valueOf(setmealId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("订单")));
    }

    private void insertDish(Long id, String name, long price, int status) {
        jdbcTemplate.update("insert into dish(id, name, category_id, price, image, description, status,"
                        + " create_time, update_time) values (?, ?, ?, ?, ?, ?, ?, now(), now())",
                id, name, DISH_CATEGORY_ID, price, "http://example.com/" + id + ".jpg", name, status);
    }

    private long createSetmeal(int status) throws Exception {
        String body = """
                {
                  "name": "双人特惠餐",
                  "categoryId": %d,
                  "price": 6800,
                  "image": "http://example.com/setmeal.jpg",
                  "description": "双人套餐",
                  "status": %d,
                  "setmealDishes": [
                    {"dishId": %d, "copies": 1},
                    {"dishId": %d, "copies": 2}
                  ]
                }
                """.formatted(SETMEAL_CATEGORY_ID, status, DISH_1, DISH_2);

        MvcResult result = mockMvc.perform(post("/admin/setmeal").header("token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, node.get("code").asInt(), node.get("msg").asText());

        return jdbcTemplate.queryForObject("select max(id) from setmeal", Long.class);
    }

    private String userToken() {
        return jwtUtil.createUserToken(1L);
    }
}
