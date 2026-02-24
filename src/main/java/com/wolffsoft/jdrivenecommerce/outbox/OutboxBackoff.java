package com.wolffsoft.jdrivenecommerce.outbox;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Duration;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OutboxBackoff {

    public static Duration computeDelay(int attempts) {
        // attempts starts at 1 on first failure
        long seconds = 1L << Math.min(attempts, 8); // 2^attempts capped at 256s
        seconds = Math.min(seconds, 300);          // cap at 5 minutes
        return Duration.ofSeconds(seconds);
    }
}
