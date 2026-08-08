CREATE TABLE IF NOT EXISTS ping_pong_event (
    id          BIGSERIAL PRIMARY KEY,
    instance_id VARCHAR(100),
    source      VARCHAR(20),
    result      VARCHAR(50),
    result_zh   VARCHAR(100),
    created_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ping_pong_event_created_at
    ON ping_pong_event (created_at);
