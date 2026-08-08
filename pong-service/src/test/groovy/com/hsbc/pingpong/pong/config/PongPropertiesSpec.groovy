package com.hsbc.pingpong.pong.config

import spock.lang.Specification

class PongPropertiesSpec extends Specification {

    def "holds sensible default values"() {
        given:
        def properties = new PongProperties()

        expect:
        properties.maxRequestsPerSecond == 1
    }

    def "exposes setters for all configuration"() {
        given:
        def properties = new PongProperties()

        when:
        properties.maxRequestsPerSecond = 5

        then:
        properties.maxRequestsPerSecond == 5
    }

    def "supports value equality, hashing and a readable toString"() {
        given:
        def a = new PongProperties()
        def b = new PongProperties()
        def c = new PongProperties()
        c.maxRequestsPerSecond = 9

        expect:
        a == b
        a.hashCode() == b.hashCode()
        a != c
        a.toString().contains("maxRequestsPerSecond")
    }
}
