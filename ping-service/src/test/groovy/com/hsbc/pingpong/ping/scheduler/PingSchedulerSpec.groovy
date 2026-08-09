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
        when:
        scheduler.fire().block()

        then:
        1 * pingRequestService.fireOnce() >> Mono.empty()
    }

    def "scheduled tick subscribes to the fire sequence without throwing"() {
        given:
        pingRequestService.fireOnce() >> Mono.empty()

        when:
        scheduler.pingOnce()

        then:
        noExceptionThrown()
    }

    def "one-arg constructor used by Spring defaults jitter to the constant"() {
        given:
        pingRequestService.fireOnce() >> Mono.empty()

        when:
        def defaultScheduler = new PingScheduler(pingRequestService)
        defaultScheduler.fire().block()

        then:
        noExceptionThrown()
    }
}
