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

    def "one-arg constructor used by Spring works"() {
        given:
        pingRequestService.fireOnce() >> Mono.empty()

        when:
        def defaultScheduler = new PingScheduler(pingRequestService)
        defaultScheduler.fire().block()

        then:
        noExceptionThrown()
    }
}
