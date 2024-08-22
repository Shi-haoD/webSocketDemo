package com.softdev.system.demo.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

@ServerEndpoint("/imserver/{userId}")
@Component
public class WebSocketServer {

    private static final Logger logger = Logger.getLogger(WebSocketServer.class);
    private static int onlineCount = 0; // 添加这行
    private static ConcurrentHashMap<String, List<WebSocketServer>> webSocketMap = new ConcurrentHashMap<>();
    private Session session;
    private String userId = "";
    private ScheduledExecutorService heartbeatScheduler;

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.session = session;
        this.userId = userId;

        // 如果 webSocketMap 已经存在 userId，直接添加到对应的连接列表中
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.get(userId).add(this);
        } else {
            // 如果 webSocketMap 不存在 userId，新建一个连接列表
            List<WebSocketServer> webSocketServerList = new CopyOnWriteArrayList<>();
            webSocketServerList.add(this);
            webSocketMap.put(userId, webSocketServerList);
            addOnlineCount(); // 在线数加1
        }

        logger.info("用户连接:" + userId + ",当前在线人数为:" + getOnlineCount());

        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            logger.error("用户:" + userId + ",网络异常!!!!!!");
        }

        // 启动心跳机制
        startHeartbeat();
    }

    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(userId)) {
            List<WebSocketServer> webSocketServerList = webSocketMap.get(userId);
            webSocketServerList.remove(this);
            if (webSocketServerList.isEmpty()) {
                webSocketMap.remove(userId); // 如果没有连接了，移除这个 userId
                subOnlineCount(); // 在线数减1
            }
        }
        logger.info("用户退出:" + userId + ",当前在线人数为:" + getOnlineCount());

        // 停止心跳机制
        stopHeartbeat();
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("用户消息:" + userId + ",报文:" + message);
        logger.info("用户消息:" + userId + ",session:" + session);
        if (StringUtils.isNotBlank(message)) {
            try {
                JSONObject jsonObject = JSON.parseObject(message);
                jsonObject.put("fromUserId", this.userId);
                String toUserId = jsonObject.getString("toUserId");
                if (StringUtils.isNotBlank(toUserId) && webSocketMap.containsKey(toUserId)) {
                    for (WebSocketServer item : webSocketMap.get(toUserId)) {
                        item.sendMessage(jsonObject.toJSONString());
                    }
                } else {
                    logger.error("请求的 userId:" + toUserId + "不在该服务器上");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("用户错误:" + this.userId + ",原因:" + error.getMessage());
        error.printStackTrace();

        // 停止心跳机制
        stopHeartbeat();
    }

    public void sendMessage(String message) throws IOException {
        if (this.session.isOpen()) {
            this.session.getAsyncRemote().sendText(message);
        } else {
            logger.error("WebSocket 连接已关闭，无法发送消息: " + message);
        }
    }

    public static void sendInfo(String message, @PathParam("userId") String userId) throws IOException {
        logger.info("发送消息到:" + userId + "，报文:" + message);
        if (StringUtils.isNotBlank(userId) && webSocketMap.containsKey(userId)) {
            for (WebSocketServer item : webSocketMap.get(userId)) {
                item.sendMessage(message);
            }
        } else {
            logger.error("用户" + userId + "，不在线！");
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }

    /**
     * 启动心跳机制
     */
    private void startHeartbeat() {
        heartbeatScheduler = Executors.newScheduledThreadPool(1);
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                if (this.session.isOpen()) {
                    this.sendMessage("heartbeat"); // 发送心跳消息
                    logger.info("发送心跳到用户: " + userId);
                } else {
                    stopHeartbeat();
                }
            } catch (IOException e) {
                logger.error("发送心跳失败: " + e.getMessage());
                stopHeartbeat();
            }
        }, 0, 30, TimeUnit.SECONDS); // 每30秒发送一次心跳
    }

    /**
     * 停止心跳机制
     */
    private void stopHeartbeat() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown();
            logger.info("停止心跳机制: " + userId);
        }
    }
}
