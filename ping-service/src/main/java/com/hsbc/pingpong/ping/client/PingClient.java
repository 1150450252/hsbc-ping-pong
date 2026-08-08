package com.hsbc.pingpong.ping.client;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class PingClient {

    private final WebClient webClient;

    public PingClient(String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public Mono<Integer> ping() {
        return webClient.get()
                .uri("/api/pong/ping")
                .exchangeToMono(response -> Mono.just(response.rawStatusCode()));
    }
}
