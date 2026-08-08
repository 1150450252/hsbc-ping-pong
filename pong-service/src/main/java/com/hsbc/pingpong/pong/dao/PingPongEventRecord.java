package com.hsbc.pingpong.pong.dao;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Data
@Table("ping_pong_event")
public class PingPongEventRecord {

    @Id
    private Long id;
    private String instanceId;
    private String source;
    private String result;
    private String resultZh;
    private Timestamp createdAt;
}
