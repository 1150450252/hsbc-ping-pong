package com.hsbc.pingpong.ping.client;

import com.hsbc.pingpong.common.event.PingPongResult;

public final class PingResultClassifier {

    private PingResultClassifier() {
    }

    public static PingPongResult classify(boolean rateLimitedLocally, int httpStatus) {
        if (rateLimitedLocally) {
            return PingPongResult.RATE_LIMITED_LOCALLY;
        }
        return httpStatus == 200 ? PingPongResult.SENT_PONG_RESPONDED : PingPongResult.SENT_PONG_THROTTLED;
    }
}
