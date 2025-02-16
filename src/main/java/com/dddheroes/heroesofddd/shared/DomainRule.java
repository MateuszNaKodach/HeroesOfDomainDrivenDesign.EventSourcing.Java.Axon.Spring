package com.dddheroes.heroesofddd.shared;

public interface DomainRule {

    class ViolatedException extends RuntimeException {

        public ViolatedException(String message) {
            super(message);
        }

        public ViolatedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    boolean isViolated();

    String message();

    default void verify() {
        if (isViolated()) {
            throw exception();
        }
    }

    default ViolatedException exception() {
        return new ViolatedException(message());
    }
}
