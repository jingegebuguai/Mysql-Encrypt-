package com.core;

import java.util.Random;

public class OPEAlgorithm {
    private double a = 0.0;
    private double b = 0.0;
    private double sens = 0.0;

    public OPEAlgorithm(double a, double b, double sens) {
        this.a = a;
        this.b = b;
        this.sens = sens;
    }

    private double function(double value) {
        return Math.abs(value);
    }

    private double noise(double value) {
        Random random = new Random();
        return random.nextDouble() * (a * function(value + sens) * (value + sens) - a * function(value) * value);
    }

    public double nindex(double value, boolean isPutNoise) {
        if (isPutNoise == true) {
            return a * function(value) * value + b + noise(value);
        } else {
            return a * function(value) * value + b;
        }
    }
}
