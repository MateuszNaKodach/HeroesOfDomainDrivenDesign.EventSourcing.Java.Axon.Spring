package com.dddheroes.heroesofddd.utils;

import org.awaitility.Awaitility;

import java.time.Duration;

public class AwaitilityUtils {

    public static void awaitUntilAsserted(Runnable assertion) {
        awaitUntilAsserted(Duration.ofSeconds(5), assertion);
    }

    public static void awaitUntilAsserted(Duration timeout, Runnable assertion) {
        Awaitility.await()
                  .pollInSameThread()
                  .atMost(timeout)
                  .untilAsserted(assertion::run);
    }

    private AwaitilityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}
