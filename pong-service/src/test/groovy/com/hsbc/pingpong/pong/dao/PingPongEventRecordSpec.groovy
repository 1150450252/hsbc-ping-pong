package com.hsbc.pingpong.pong.dao

import com.hsbc.pingpong.common.event.PingPongResult
import spock.lang.Specification

import java.sql.Timestamp

class PingPongEventRecordSpec extends Specification {

    def "exposes the id and business fields through accessors"() {
        given:
        def record = new PingPongEventRecord()

        when:
        record.id = 7L
        record.instanceId = "ping-1"
        record.source = "PING"
        record.result = PingPongResult.SENT_PONG_RESPONDED.name()
        record.resultZh = "已发送，pong 已响应"
        record.createdAt = new Timestamp(1234L)

        then:
        record.id == 7L
        record.instanceId == "ping-1"
        record.source == "PING"
        record.result == PingPongResult.SENT_PONG_RESPONDED.name()
        record.resultZh == "已发送，pong 已响应"
        record.createdAt == new Timestamp(1234L)
    }

    def "supports value equality, hashing and a readable toString"() {
        given:
        def a = new PingPongEventRecord()
        a.id = 1L
        a.instanceId = "ping-1"
        def b = new PingPongEventRecord()
        b.id = 1L
        b.instanceId = "ping-1"
        def c = new PingPongEventRecord()
        c.id = 2L

        expect:
        a == b
        a.hashCode() == b.hashCode()
        a != c
        a.toString().contains("ping-1")
    }
}
