package com.sky.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启 SpringTask 定时任务。
 *
 * <p>本期只有订单超时未接单自动取消一个任务，见 {@code com.sky.task.OrderTimeoutTask}，
 * 默认每分钟扫描一次，可用 {@code sky.order.auto-cancel.enabled=false} 关闭。</p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
