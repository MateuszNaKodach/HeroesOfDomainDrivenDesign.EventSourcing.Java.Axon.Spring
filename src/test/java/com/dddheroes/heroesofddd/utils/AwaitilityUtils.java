package com.dddheroes.heroesofddd.utils;

import org.awaitility.Awaitility;

import java.time.Duration;

public class AwaitilityUtils {

    public static void awaitUntilAsserted(Runnable assertion) {
        Awaitility.await()
                  .pollInSameThread()
                  .atMost(Duration.ofSeconds(5))
                  .untilAsserted(assertion::run);
    }

    private AwaitilityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}
