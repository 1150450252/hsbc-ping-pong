package com.hsbc.pingpong.common.event;

public enum PingPongResult {

    PONG_RESPONDED("Pong responded to the ping request", "pong 正常响应"),
    PONG_THROTTLED("Pong throttled the request (too many requests per second)", "pong 限流"),
    RATE_LIMITED_LOCALLY("Request not sent as being rate limited", "ping 本地限流"),
    SENT_PONG_RESPONDED("Request sent & Pong responded", "已发送，pong 已响应"),
    SENT_PONG_THROTTLED("Request sent & Pong throttled it", "已发送，pong 限流");

    private final String description;
    private final String descriptionZh;

    PingPongResult(String description, String descriptionZh) {
        this.description = description;
        this.descriptionZh = descriptionZh;
    }

    public String getDescription() {
        return description;
    }

    public String getDescriptionZh() {
        return descriptionZh;
    }
}
