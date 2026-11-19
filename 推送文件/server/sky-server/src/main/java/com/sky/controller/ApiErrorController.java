package com.sky.controller;

import com.sky.common.result.Result;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * 统一 /error 出口，避免 404/405 返回 Spring 默认的 timestamp/status/error/path 结构。
 */
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public Result<Void> handleError(HttpServletRequest request) {
        Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttribute instanceof Integer ? (Integer) statusAttribute : 500;
        String message;
        switch (status) {
            case 400:
                message = "请求参数不正确";
                break;
            case 404:
                message = "接口不存在";
                break;
            case 405:
                message = "请求方法不支持";
                break;
            default:
                message = status >= 500 ? "服务器开小差了，请稍后重试" : "请求处理失败";
        }
        return Result.error(status, message);
    }
}
