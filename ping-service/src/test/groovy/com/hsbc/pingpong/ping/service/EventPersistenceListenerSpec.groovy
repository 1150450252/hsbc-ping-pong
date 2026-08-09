package com.hsbc.pingpong.ping.service

import com.hsbc.pingpong.common.event.PingPongEvent
import com.hsbc.pingpong.common.event.PingPongResult
import com.hsbc.pingpong.ping.dao.PingPongEventRepository
import spock.lang.Specification

import java.sql.Timestamp

class EventPersistenceListenerSpec extends Specification {

    PingPongEventRepository repository
    EventPersistenceListener listener

    def setup() {
        repository = Mock(PingPongEventRepository)
        listener = new EventPersistenceListener(repository)
    }

    def "persists an incoming ping-pong event to the repository"() {
        given:
        def event = new PingPongEvent("ping-1", "PING", PingPongResult.SENT_PONG_RESPONDED, 1234L)

        when:
        listener.onEvent(event)

        then:
        1 * repository.save({ record ->
            record != null
            record.instanceId == "ping-1"
            record.source == "PING"
            record.result == PingPongResult.SENT_PONG_RESPONDED.name()
            record.resultZh == "已发送，pong 已响应"
            record.createdAt == new Timestamp(1234L)
        })
    }

    def "persists an event without a result as a null result"() {
        given:
        def event = new PingPongEvent("ping-2", "PING", null, 5678L)

        when:
        listener.onEvent(event)

        then:
        1 * repository.save({ record -> record.result == null && record.resultZh == null })
    }
}
