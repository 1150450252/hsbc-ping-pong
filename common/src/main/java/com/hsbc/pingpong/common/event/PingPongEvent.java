package com.hsbc.pingpong.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PingPongEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String instanceId;
    private String source;
    private PingPongResult result;
    private long timestamp;
}
