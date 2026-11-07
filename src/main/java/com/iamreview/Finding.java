package com.iamreview;

public record Finding(String user, String check, Severity severity, String explanation, String recommendation) {
    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW, REVIEW }
}
