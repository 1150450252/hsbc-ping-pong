package com.hsbc.pingpong.ping.scheduler

import com.hsbc.pingpong.ping.service.PingRequestService
import reactor.core.publisher.Mono
import spock.lang.Specification

class PingSchedulerSpec extends Specification {

    PingRequestService pingRequestService
    PingScheduler scheduler

    def setup() {
        pingRequestService = Mock(PingRequestService)
        scheduler = new PingScheduler(pingRequestService)
    }

    def "delegates each scheduled tick to the ping request service"() {
        when:
        scheduler.pingOnce()

        then:
        1 * pingRequestService.fireOnce() >> Mono.empty()
    }
}
