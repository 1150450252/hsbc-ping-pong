package com.hsbc.pingpong.ping.client

import com.hsbc.pingpong.common.event.PingPongResult
import spock.lang.Specification
import spock.lang.Unroll

class PingResultClassifierSpec extends Specification {

    @Unroll
    def "classifies rateLimitedLocally=#rateLimited and status=#status as #expected"() {
        expect:
        PingResultClassifier.classify(rateLimited, status) == expected

        where:
        rateLimited | status | expected
        true        | 200    | PingPongResult.RATE_LIMITED_LOCALLY
        true        | 429    | PingPongResult.RATE_LIMITED_LOCALLY
        false       | 200    | PingPongResult.SENT_PONG_RESPONDED
        false       | 429    | PingPongResult.SENT_PONG_THROTTLED
        false       | 500    | PingPongResult.SENT_PONG_THROTTLED
    }
}
