package com.xwms.core.notification.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.notification.entity.*;
import com.xwms.core.notification.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 消息推送与通知 Controller */
@Tag(name = "消息推送与通知", description = "通知模板/通知发送/通知规则/用户设置")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ============================================================

    // 通知模板
    // ============================================================

    @Operation(summary = "创建通知模板")
    @PostMapping("/templates")
    public Result<NotifyTemplate> createTemplate(@RequestBody NotifyTemplate template) {
        return Result.success(notificationService.createTemplate(template));
    }

    @Operation(summary = "查询模板详情")
    @GetMapping("/templates/{id}")
    public Result<NotifyTemplate> getTemplate(@PathVariable Long id) {
        return Result.success(notificationService.getTemplate(id));
    }

    @Operation(summary = "分页查询模板")
    @GetMapping("/templates")
    public Result<Page<NotifyTemplate>> pageTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String channel) {
        return Result.success(
                notificationService.pageTemplates(new Page<>(page, size), type, channel));
    }

    // ============================================================

    // 通知发送
    // ============================================================

    @Operation(summary = "发送通知")
    @PostMapping("/send")
    public Result<NotifyRecord> sendNotification(
            @RequestParam String templateCode,
            @RequestParam String receiverType,
            @RequestParam String receiverId,
            @RequestParam String receiverName,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessNo,
            @RequestParam(required = false) String priority,
            @RequestBody Map<String, Object> variables) {
        return Result.success(
                notificationService.sendNotification(
                        templateCode,
                        variables,
                        receiverType,
                        receiverId,
                        receiverName,
                        businessType,
                        businessNo,
                        priority));
    }

    @Operation(summary = "触发事件通知")
    @PostMapping("/trigger/{eventType}")
    public Result<Void> triggerEvent(
            @PathVariable String eventType,
            @RequestParam String receiverType,
            @RequestParam String receiverId,
            @RequestParam String receiverName,
            @RequestBody Map<String, Object> eventData) {
        notificationService.triggerEvent(
                eventType, eventData, receiverType, receiverId, receiverName);
        return Result.success();
    }

    // ============================================================

    // 通知记录
    // ============================================================

    @Operation(summary = "分页查询通知记录")
    @GetMapping("/records")
    public Result<Page<NotifyRecord>> pageRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String receiverId,
            @RequestParam(required = false) String status) {
        return Result.success(
                notificationService.pageRecords(new Page<>(page, size), receiverId, status));
    }

    @Operation(summary = "查询我的通知")
    @GetMapping("/my")
    public Result<List<NotifyRecord>> getMyNotifications(@RequestParam String receiverId) {
        return Result.success(notificationService.getMyNotifications(receiverId));
    }

    @Operation(summary = "查询未读数量")
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount(@RequestParam String receiverId) {
        return Result.success(notificationService.getUnreadCount(receiverId));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/records/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/records/read-all")
    public Result<Void> markAllAsRead(@RequestParam String receiverId) {
        notificationService.markAllAsRead(receiverId);
        return Result.success();
    }

    @Operation(summary = "重试失败通知")
    @PostMapping("/retry")
    public Result<Integer> retryFailed() {
        return Result.success(notificationService.retryFailedNotifications());
    }

    // ============================================================

    // 通知规则
    // ============================================================

    @Operation(summary = "创建通知规则")
    @PostMapping("/rules")
    public Result<NotifyRule> createRule(@RequestBody NotifyRule rule) {
        return Result.success(notificationService.createRule(rule));
    }

    @Operation(summary = "查询事件规则")
    @GetMapping("/rules/event/{eventType}")
    public Result<List<NotifyRule>> getRulesByEvent(@PathVariable String eventType) {
        return Result.success(notificationService.getRulesByEvent(eventType));
    }

    // ============================================================

    // 用户通知设置
    // ============================================================

    @Operation(summary = "查询用户通知设置")
    @GetMapping("/settings/{userId}")
    public Result<List<UserNotifySetting>> getUserSettings(@PathVariable Long userId) {
        return Result.success(notificationService.getUserSettings(userId));
    }

    @Operation(summary = "更新用户通知设置")
    @PutMapping("/settings")
    public Result<UserNotifySetting> updateUserSetting(@RequestBody UserNotifySetting setting) {
        return Result.success(notificationService.updateUserSetting(setting));
    }
}
