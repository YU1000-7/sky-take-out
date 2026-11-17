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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单与报表接口集成测试：覆盖下单事务、状态流转、越权拦截和报表口径。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ADDRESS_ID = 1L;
    private static final Long DISH_ID = 1L;
    private static final Long SETMEAL_ID = 1L;

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
    private String adminToken;

    @BeforeEach
    void setUp() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        jdbcTemplate.update("delete from order_detail");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from shopping_cart");
        jdbcTemplate.update("delete from address_book");
        jdbcTemplate.update("delete from setmeal");
        jdbcTemplate.update("delete from dish");

        jdbcTemplate.update("insert into dish(id, name, category_id, price, image, status, create_time, update_time)"
                + " values (?, '鱼香肉丝', 20, 3800, 'http://example.com/d.jpg', 1, now(), now())", DISH_ID);
        jdbcTemplate.update("insert into setmeal(id, category_id, name, price, image, status, create_time, update_time)"
                + " values (?, 10, '双人特惠餐', 6800, 'http://example.com/s.jpg', 1, now(), now())", SETMEAL_ID);
        jdbcTemplate.update("insert into address_book(id, user_id, consignee, phone, province_name, city_name,"
                + " district_name, detail, is_default, create_time, update_time)"
                + " values (?, ?, '张三', '13800138000', '江苏省', '苏州市', '姑苏区', '观前街1号', 1, now(), now())",
                ADDRESS_ID, USER_ID);

        insertDishCart(2, 3800L);
        insertSetmealCart(1, 6800L);

        userToken = jwtUtil.createUserToken(USER_ID);
        adminToken = jwtUtil.createAdminToken(1L);
    }

    @Test
    void shouldSubmitOrderAndClearCart() throws Exception {
        long orderId = submitOrder();

        mockMvc.perform(get("/user/order/list").header("token", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value(1))
                .andExpect(jsonPath("$.data.records[0].amount").value(14400))
                .andExpect(jsonPath("$.data.records[0].consignee").value("张三"))
                .andExpect(jsonPath("$.data.records[0].phone").value("13800138000"))
                .andExpect(jsonPath("$.data.records[0].address").value("江苏省苏州市姑苏区观前街1号"))
                .andExpect(jsonPath("$.data.records[0].remark").value("不要香菜"));

        mockMvc.perform(get("/user/order/{id}", orderId).header("token", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.status").value(1))
                .andExpect(jsonPath("$.data.order.payStatus").value(1))
                .andExpect(jsonPath("$.data.orderDetailList.length()").value(2))
                .andExpect(jsonPath("$.data.orderDetailList[0].name").value("鱼香肉丝"))
                .andExpect(jsonPath("$.data.orderDetailList[0].dishId").value(DISH_ID.intValue()))
                .andExpect(jsonPath("$.data.orderDetailList[0].number").value(2))
                .andExpect(jsonPath("$.data.orderDetailList[1].setmealId").value(SETMEAL_ID.intValue()))
                .andExpect(jsonPath("$.data.orderDetailList[1].amount").value(6800));

        // 下单成功后购物车被清空
        mockMvc.perform(get("/user/shoppingCart/list").header("token", userToken))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRejectSubmitWithInvalidRequest() throws Exception {
        String unknownAddress = """
                {"addressBookId": 999, "payMethod": 1}
                """;
        mockMvc.perform(post("/user/order/submit").header("token", userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(unknownAddress))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(containsString("收货地址")));

        String noAddress = """
                {"payMethod": 1}
                """;
        mockMvc.perform(post("/user/order/submit").header("token", userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(noAddress))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(containsString("收货地址")));

        jdbcTemplate.update("delete from shopping_cart");
        String emptyCart = """
                {"addressBookId": %d, "payMethod": 1}
                """.formatted(ADDRESS_ID);
        mockMvc.perform(post("/user/order/submit").header("token", userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(emptyCart))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(containsString("购物车为空")));
    }

    @Test
    void shouldRunThroughMerchantStatusFlow() throws Exception {
        long orderId = submitOrder();

        // 未接单不能直接完成
        mockMvc.perform(put("/admin/order/complete").header("token", adminToken)
                        .param("id", String.valueOf(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(containsString("无法完成")));

        mockMvc.perform(put("/admin/order/confirm").header("token", adminToken)
                        .param("id", String.valueOf(orderId)))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("接单成功"));

        mockMvc.perform(put("/admin/order/delivery").header("token", adminToken)
                        .param("id", String.valueOf(orderId)))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("派送成功"));

        mockMvc.perform(put("/admin/order/complete").header("token", adminToken)
                        .param("id", String.valueOf(orderId)))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("订单完成"));

        mockMvc.perform(get("/admin/order/details/{id}", orderId).header("token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.status").value(4))
                .andExpect(jsonPath("$.data.order.deliveryTime").exists())
                .andExpect(jsonPath("$.data.orderDetailList.length()").value(2));

        // 已完成订单不能重复流转
        mockMvc.perform(put("/admin/order/confirm").header("token", adminToken)
                        .param("id", String.valueOf(orderId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(containsString("已完成")));
    }

    @Test
    void shouldAllowCancelOnlyBeforeConfirmed() throws Exception {
        long cancelled = submitOrder();
        mockMvc.perform(put("/user/order/cancel").header("token", userToken)
                        .param("id", String.valueOf(cancelled)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("取消订单成功"));

        mockMvc.perform(get("/user/order/{id}", cancelled).header("token", userToken))
                .andExpect(jsonPath("$.data.order.status").value(5))
                .andExpect(jsonPath("$.data.order.cancelReason").value("用户取消订单"))
                .andExpect(jsonPath("$.data.order.cancelTime").exists());

        insertDishCart(1, 3800L);
        long confirmed = submitOrder();
        mockMvc.perform(put("/admin/order/confirm").header("token", adminToken)
                        .param("id", String.valueOf(confirmed)))
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(put("/user/order/cancel").header("token", userToken)
                        .param("id", String.valueOf(confirmed)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(containsString("已接单")));
    }

    @Test
    void shouldHideOtherCustomersOrder() throws Exception {
        long orderId = submitOrder();
        String otherToken = jwtUtil.createUserToken(OTHER_USER_ID);

        mockMvc.perform(get("/user/order/{id}", orderId).header("token", otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(containsString("无权查看")));

        mockMvc.perform(get("/user/order/list").header("token", otherToken))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(put("/user/order/cancel").header("token", otherToken)
                        .param("id", String.valueOf(orderId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(containsString("无权操作")));
    }

    @Test
    void shouldFilterAdminOrderPage() throws Exception {
        submitOrder();

        mockMvc.perform(get("/admin/order/page").header("token", adminToken)
                        .param("page", "1").param("pageSize", "10").param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value(1));

        mockMvc.perform(get("/admin/order/page").header("token", adminToken)
                        .param("status", "4"))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/admin/order/page").header("token", adminToken)
                        .param("number", "999999"))
                .andExpect(jsonPath("$.data.total").value(0));

        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/admin/order/page").header("token", adminToken)
                        .param("beginTime", today + " 00:00:00")
                        .param("endTime", today + " 23:59:59"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldReportTurnoverStatisticsAndTop10() throws Exception {
        LocalDate today = LocalDate.now();
        insertCompletedOrder("202611040101", 5000L, 2);
        insertCompletedOrder("202611040102", 3000L, 1);

        mockMvc.perform(get("/admin/report/turnover").header("token", adminToken)
                        .param("begin", today.toString()).param("end", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].date").value(today.toString()))
                .andExpect(jsonPath("$.data[0].turnover").value(8000));

        mockMvc.perform(get("/admin/report/orderStatistics").header("token", adminToken)
                        .param("begin", today.toString()).param("end", today.toString()))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.orderCountList[0].orderCount").value(2))
                .andExpect(jsonPath("$.data.validOrderCountList[0].validOrderCount").value(2));

        mockMvc.perform(get("/admin/report/top10").header("token", adminToken)
                        .param("begin", today.toString()).param("end", today.toString()))
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("鱼香肉丝"))
                .andExpect(jsonPath("$.data[0].sales").value(3));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/user/order/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/admin/order/page"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/admin/report/turnover"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private long submitOrder() throws Exception {
        String body = """
                {"addressBookId": %d, "payMethod": 1, "remark": "不要香菜"}
                """.formatted(ADDRESS_ID);

        MvcResult result = mockMvc.perform(post("/user/order/submit").header("token", userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, node.get("code").asInt(), node.get("msg").asText());
        return node.get("data").get("orderId").asLong();
    }

    private void insertDishCart(int number, long amount) {
        jdbcTemplate.update("insert into shopping_cart(user_id, dish_id, dish_flavor, number, amount, name, image,"
                + " create_time) values (?, ?, '微辣', ?, ?, '鱼香肉丝', 'http://example.com/d.jpg', now())",
                USER_ID, DISH_ID, number, amount);
    }

    private void insertSetmealCart(int number, long amount) {
        jdbcTemplate.update("insert into shopping_cart(user_id, setmeal_id, number, amount, name, image, create_time)"
                + " values (?, ?, ?, ?, '双人特惠餐', 'http://example.com/s.jpg', now())",
                USER_ID, SETMEAL_ID, number, amount);
    }

    /**
     * 直接落一笔已完成订单，用于报表统计。
     */
    private void insertCompletedOrder(String number, long amount, int dishNumber) {
        LocalDateTime orderTime = LocalDate.now().atTime(10, 0);
        jdbcTemplate.update("insert into orders(number, status, user_id, address_book_id, pay_status, pay_method,"
                        + " amount, order_time) values (?, 4, ?, ?, 1, 1, ?, ?)",
                number, USER_ID, ADDRESS_ID, amount, orderTime);
        Long orderId = jdbcTemplate.queryForObject("select id from orders where number = ?", Long.class, number);
        jdbcTemplate.update("insert into order_detail(name, image, order_id, dish_id, number, amount)"
                + " values ('鱼香肉丝', 'http://example.com/d.jpg', ?, ?, ?, 3800)", orderId, DISH_ID, dishNumber);
    }
}
