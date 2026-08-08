package com.hsbc.pingpong.ping.scheduler

import com.hsbc.pingpong.ping.service.PingRequestService
import reactor.core.publisher.Mono
import spock.lang.Specification

class PingSchedulerSpec extends Specification {

    PingRequestService pingRequestService
    PingScheduler scheduler

    def setup() {
        pingRequestService = Mock(PingRequestService)
        // jitter 上界取 1ms,nextLong(1) 恒为 0,让测试同步、不真实等待
        scheduler = new PingScheduler(pingRequestService, 1)
    }

    def "fires the ping request service after the jitter delay"() {
        given:
        pingRequestService.fireOnce() >> Mono.empty()

        when:
        scheduler.fire().block()

        then:
        1 * pingRequestService.fireOnce()
    }

    def "scheduled tick subscribes to the fire sequence without throwing"() {
        when:
        scheduler.pingOnce()

        then:
        noExceptionThrown()
    }
}
