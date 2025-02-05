package com.dddheroes.heroesofddd.shared;

public record Amount(int raw) {

    public Amount {
        if (raw < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }

    public static Amount of(int raw) {
        return new Amount(raw);
    }

    public static Amount zero() {
        return new Amount(0);
    }

    public int compareTo(Amount o) {
        return Integer.compare(this.raw, o.raw);
    }

    public Amount plus(Amount o) {
        return new Amount(this.raw + o.raw);
    }

    public Amount minus(Amount o) {
        return new Amount(this.raw - o.raw);
    }
}
