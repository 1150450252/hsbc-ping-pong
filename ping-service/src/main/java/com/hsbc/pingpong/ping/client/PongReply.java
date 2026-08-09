package com.hsbc.pingpong.ping.client;

public class PongReply {

    private final int status;
    private final String body;

    public PongReply(int status, String body) {
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }
}
