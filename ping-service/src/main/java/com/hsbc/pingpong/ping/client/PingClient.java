package com.hsbc.pingpong.ping.client;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class PingClient {

    private final WebClient webClient;
    private final String instanceId;

    public PingClient(String baseUrl, String instanceId) {
        this.instanceId = instanceId;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public Mono<PongReply> ping() {
        return webClient.post()
                .uri("/api/pong/ping")
                .header("X-Ping-Instance", instanceId)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("Hello")
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new PongReply(response.rawStatusCode(), body)));
    }
}
