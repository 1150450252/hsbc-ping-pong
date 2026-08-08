package com.hsbc.pingpong.pong.service;

import com.hsbc.pingpong.common.event.PingPongEvent;
import com.hsbc.pingpong.common.event.PingPongResult;
import com.hsbc.pingpong.pong.dao.PingPongEventRecord;
import com.hsbc.pingpong.pong.dao.PingPongEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
@ConditionalOnProperty(name = "hsbc.pingpong.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class EventPersistenceListener {

    private static final Logger log = LoggerFactory.getLogger(EventPersistenceListener.class);

    private final PingPongEventRepository repository;

    public EventPersistenceListener(PingPongEventRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "${hsbc.pingpong.kafka.topic:ping-pong-events}",
            groupId = "${hsbc.pingpong.kafka.group-id:pong-service}")
    public void onEvent(PingPongEvent event) {
        persist(event);
        log.info("Persisted ping-pong event: {}", event.getInstanceId());
    }

    private void persist(PingPongEvent event) {
        PingPongEventRecord record = new PingPongEventRecord();
        record.setInstanceId(event.getInstanceId());
        record.setSource(event.getSource());
        PingPongResult result = event.getResult();
        record.setResult(result == null ? null : result.name());
        record.setResultZh(result == null ? null : result.getDescriptionZh());
        record.setCreatedAt(new Timestamp(event.getTimestamp()));
        repository.save(record);
    }
}
