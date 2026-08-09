package com.hsbc.pingpong.ping.dao;

import org.springframework.data.repository.CrudRepository;

public interface PingPongEventRepository extends CrudRepository<PingPongEventRecord, Long> {
}
