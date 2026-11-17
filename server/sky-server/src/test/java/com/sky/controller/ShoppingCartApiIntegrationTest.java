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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 购物车接口集成测试：菜品与套餐共用同一购物车，按商品和口味合并数量。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShoppingCartApiIntegrationTest {

    private static final Long DISH_ON_SALE = 1L;
    private static final Long DISH_STOPPED = 2L;
    private static final Long SETMEAL_ON_SALE = 1L;

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

    private String userToken;

    @BeforeEach
    void setUp() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        jdbcTemplate.update("delete from shopping_cart");
        jdbcTemplate.update("delete from setmeal_dish");
        jdbcTemplate.update("delete from setmeal");
        jdbcTemplate.update("delete from dish");

        jdbcTemplate.update("insert into dish(id, name, category_id, price, image, status, create_time, update_time)"
                + " values (?, '鱼香肉丝', 20, 3800, 'http://example.com/1.jpg', 1, now(), now())", DISH_ON_SALE);
        jdbcTemplate.update("insert into dish(id, name, category_id, price, image, status, create_time, update_time)"
                + " values (?, '停售小炒', 20, 2800, 'http://example.com/2.jpg', 0, now(), now())", DISH_STOPPED);
        jdbcTemplate.update("insert into setmeal(id, category_id, name, price, image, status, create_time, update_time)"
                + " values (?, 10, '双人特惠餐', 6800, 'http://example.com/s.jpg', 1, now(), now())", SETMEAL_ON_SALE);

        userToken = jwtUtil.createUserToken(1L);
    }

    @Test
    void shouldMergeDishByFlavorAndSupportSetmealInSameCart() throws Exception {
        addCart(dishBody(DISH_ON_SALE, "微辣"));
        addCart(dishBody(DISH_ON_SALE, "微辣"));

        mockMvc.perform(get("/user/shoppingCart/list").header("token", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("鱼香肉丝"))
                .andExpect(jsonPath("$.data[0].dishFlavor").value("微辣"))
                .andExpect(jsonPath("$.data[0].number").value(2))
                .andExpect(jsonPath("$.data[0].amount").value(3800));

        addCart(dishBody(DISH_ON_SALE, "中辣"));
        addCart(setmealBody(SETMEAL_ON_SALE));

        mockMvc.perform(get("/user/shoppingCart/list").header("token", userToken))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[2].setmealId").value(SETMEAL_ON_SALE.intValue()))
                .andExpect(jsonPath("$.data[2].name").value("双人特惠餐"))
                .andExpect(jsonPath("$.data[2].number").value(1))
                .andExpect(jsonPath("$.data[2].amount").value(6800));
    }

    @Test
    void shouldReduceAndRemoveWhenNumberReachesZero() throws Exception {
        addCart(dishBody(DISH_ON_SALE, "微辣"));
        addCart(dishBody(DISH_ON_SALE, "微辣"));

        mockMvc.perform(put("/user/shoppingCart/sub").header("token", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dishBody(DISH_ON_SALE, "微辣")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/user/shoppingCart/list").header("token", userToken))
                .andExpect(jsonPath("$.data[0].number").value(1));

        mockMvc.perform(put("/user/shoppingCart/sub").header("token", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dishBody(DISH_ON_SALE, "微辣")))
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/user/shoppingCart/list").header("token", userToken))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRejectInvalidCartRequests() throws Exception {
        // 停售菜品不能加购
        mockMvc.perform(post("/user/shoppingCart").header("token", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dishBody(DISH_STOPPED, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("停售")));

        // 菜品和套餐必须二选一
        String bothBody = objectMapper.writeValueAsString(java.util.Map.of("dishId", 1, "setmealId", 1));
        mockMvc.perform(post("/user/shoppingCart").header("token", userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(bothBody))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("只能选择其中一个")));

        // 减少不存在的商品
        mockMvc.perform(put("/user/shoppingCart/sub").header("token", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dishBody(DISH_ON_SALE, "特辣")))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("没有该商品")));

        // 未登录
        mockMvc.perform(get("/user/shoppingCart/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldCleanCart() throws Exception {
        addCart(dishBody(DISH_ON_SALE, "微辣"));
        addCart(setmealBody(SETMEAL_ON_SALE));

        mockMvc.perform(delete("/user/shoppingCart/clean").header("token", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("清空购物车成功"));

        mockMvc.perform(get("/user/shoppingCart/list").header("token", userToken))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private void addCart(String body) throws Exception {
        mockMvc.perform(post("/user/shoppingCart").header("token", userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    private String dishBody(Long dishId, String flavor) throws Exception {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("dishId", dishId);
        body.put("dishFlavor", flavor);
        return objectMapper.writeValueAsString(body);
    }

    private String setmealBody(Long setmealId) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("setmealId", setmealId));
    }
}
