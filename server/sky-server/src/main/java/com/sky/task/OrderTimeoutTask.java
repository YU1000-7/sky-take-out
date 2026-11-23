package com.sky.task;

import com.sky.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时未接单自动取消。
 *
 * <p>只处理仍处于「待接单」且下单时间超过 {@code timeout-minutes} 的订单，
 * 取消原因写入「超时未接单，系统自动取消」，客户在订单详情里能看到。
 * 默认每分钟扫一次，可用 {@code sky.order.auto-cancel.enabled=false} 关闭，
 * 测试环境已在 application-test.yml 里关闭。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sky.order.auto-cancel", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class OrderTimeoutTask {

    private final OrderService orderService;

    @Value("${sky.order.auto-cancel.timeout-minutes:15}")
    private int timeoutMinutes;

    @Scheduled(
            initialDelayString = "${sky.order.auto-cancel.initial-delay-ms:60000}",
            fixedDelayString = "${sky.order.auto-cancel.interval-ms:60000}"
    )
    public void cancelTimeoutOrders() {
        try {
            orderService.cancelTimeoutOrders(timeoutMinutes);
        } catch (Exception exception) {
            // 单次失败不能中断后续调度，记录后等下一轮
            log.error("自动取消超时订单失败", exception);
        }
    }
}
