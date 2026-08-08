package com.hsbc.pingpong.pong.dao;

import org.springframework.data.repository.CrudRepository;

public interface PingPongEventRepository extends CrudRepository<PingPongEventRecord, Long> {
}
