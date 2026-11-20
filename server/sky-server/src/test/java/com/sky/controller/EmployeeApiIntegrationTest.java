package com.sky.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldCompleteEmployeeWorkflow() throws Exception {
        ValueOperations<String, String> valueOperations = mockValueOperations();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "username", "admin",
                "password", "123456"
        ));
        MvcResult loginResult = mockMvc.perform(post("/admin/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andReturn();

        JsonNode loginData = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data");
        String token = loginData.get("token").asText();

        String createBody = objectMapper.writeValueAsString(Map.of(
                "username", "zhangsan",
                "name", "张三",
                "phone", "13800138001",
                "sex", "男",
                "idNumber", "110101199001011234"
        ));
        mockMvc.perform(post("/admin/employee")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        MvcResult pageResult = mockMvc.perform(get("/admin/employee/page")
                        .header("token", token)
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("name", "张三"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("zhangsan"))
                .andReturn();

        JsonNode createdEmployee = objectMapper.readTree(pageResult.getResponse().getContentAsString())
                .path("data").path("records").get(0);
        long employeeId = createdEmployee.path("id").asLong();
        String createdUpdateTime = createdEmployee.path("updateTime").asText();
        // 数据库 DATETIME 精度到秒，确保一次真实更新能产生不同的更新时间。
        Thread.sleep(1100);

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "id", employeeId,
                "username", "zhangsan",
                "name", "张三丰",
                "phone", "13900139000",
                "sex", "男",
                "idNumber", "110101199001011234"
        ));
        mockMvc.perform(put("/admin/employee")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(put("/admin/employee/status/0")
                        .header("token", token)
                        .param("id", String.valueOf(employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        MvcResult detailResult = mockMvc.perform(get("/admin/employee/" + employeeId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("张三丰"))
                .andExpect(jsonPath("$.data.phone").value("13900139000"))
                .andExpect(jsonPath("$.data.status").value(0))
                .andReturn();
        String updatedUpdateTime = objectMapper.readTree(detailResult.getResponse().getContentAsString())
                .path("data").path("updateTime").asText();
        assertNotEquals(createdUpdateTime, updatedUpdateTime);

        mockMvc.perform(post("/admin/employee/logout")
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        mockMvc.perform(post("/admin/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        mockMvc.perform(post("/admin/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "bad-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    @Test
    void shouldCreateAndFindEmployeeWithNameFilter() throws Exception {
        String token = adminToken();
        String username = uniqueValue("employee");
        String name = uniqueValue("员工");

        mockMvc.perform(post("/admin/employee")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeBody(null, username, name, "13800138001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/admin/employee/page")
                        .header("token", token)
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("name", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value(username))
                .andExpect(jsonPath("$.data.records[0].name").value(name));
    }

    @Test
    void shouldRejectDuplicateUsername() throws Exception {
        String token = adminToken();
        String username = uniqueValue("duplicate");
        createEmployee(token, username, uniqueValue("员工"), "13800138001");

        mockMvc.perform(post("/admin/employee")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeBody(null, username, uniqueValue("员工"), "13800138002")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("用户名已存在"));
    }

    @Test
    void shouldUpdateEmployee() throws Exception {
        String token = adminToken();
        String username = uniqueValue("update");
        long employeeId = createEmployee(token, username, uniqueValue("员工"), "13800138001");
        String updatedName = uniqueValue("更新员工");

        mockMvc.perform(put("/admin/employee")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeBody(employeeId, username, updatedName, "13900139000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/admin/employee/" + employeeId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value(updatedName))
                .andExpect(jsonPath("$.data.phone").value("13900139000"));
    }

    @Test
    void shouldRejectUpdatingMissingEmployee() throws Exception {
        String token = adminToken();

        mockMvc.perform(put("/admin/employee")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeBody(999999L, uniqueValue("missing"),
                                uniqueValue("不存在员工"), "13800138001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("员工不存在"));
    }

    @Test
    void shouldGetEmployeeDetail() throws Exception {
        String token = adminToken();
        String username = uniqueValue("detail");
        String name = uniqueValue("详情员工");
        long employeeId = createEmployee(token, username, name, "13800138001");

        mockMvc.perform(get("/admin/employee/" + employeeId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(employeeId))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void shouldRejectMissingEmployeeDetail() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/admin/employee/999999")
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("员工不存在"));
    }

    @Test
    void shouldDisableEmployeeAndRejectLogin() throws Exception {
        String token = adminToken();
        String username = uniqueValue("disabled");
        long employeeId = createEmployee(token, username, uniqueValue("禁用员工"), "13800138001");

        mockMvc.perform(put("/admin/employee/status/0")
                        .header("token", token)
                        .param("id", String.valueOf(employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/admin/employee/" + employeeId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0));

        mockMvc.perform(post("/admin/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("账号已被禁用，请联系管理员"));
    }

    @Test
    void shouldRejectInvalidEmployeeStatus() throws Exception {
        String token = adminToken();
        long employeeId = createEmployee(token, uniqueValue("status"),
                uniqueValue("状态员工"), "13800138001");

        mockMvc.perform(put("/admin/employee/status/9")
                        .header("token", token)
                        .param("id", String.valueOf(employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("员工状态只能是 0 或 1"));
    }

    @Test
    void shouldOnlyInvalidateLoggedOutToken() throws Exception {
        String firstToken = adminToken();
        String secondToken = adminToken();
        assertNotEquals(firstToken, secondToken);

        ValueOperations<String, String> valueOperations = mockValueOperations();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        mockMvc.perform(post("/admin/employee/logout")
                        .header("token", firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        verify(valueOperations).set(
                eq("sky:token:blacklist:" + firstToken),
                eq("1"),
                any(Duration.class)
        );

        when(redisTemplate.hasKey("sky:token:blacklist:" + firstToken)).thenReturn(true);
        when(redisTemplate.hasKey("sky:token:blacklist:" + secondToken)).thenReturn(false);
        mockMvc.perform(get("/admin/employee/page")
                        .header("token", firstToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/admin/employee/page")
                        .header("token", secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void shouldRejectAdminRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/admin/employee/page"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("未登录或 Token 已失效，请重新登录"));
    }

    private String adminToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/admin/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    private long createEmployee(String token, String username, String name, String phone) throws Exception {
        mockMvc.perform(post("/admin/employee")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeBody(null, username, name, phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        MvcResult pageResult = mockMvc.perform(get("/admin/employee/page")
                        .header("token", token)
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("name", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andReturn();

        JsonNode records = objectMapper.readTree(pageResult.getResponse().getContentAsString())
                .path("data").path("records");
        assertNotNull(records);
        return records.get(0).path("id").asLong();
    }

    private String loginBody(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));
    }

    private String employeeBody(Long id, String username, String name, String phone) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        if (id != null) {
            body.put("id", id);
        }
        body.put("username", username);
        body.put("name", name);
        body.put("phone", phone);
        body.put("sex", "男");
        body.put("idNumber", "110101199001011234");
        return objectMapper.writeValueAsString(body);
    }

    private String uniqueValue(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> mockValueOperations() {
        return org.mockito.Mockito.mock(ValueOperations.class);
    }
}
