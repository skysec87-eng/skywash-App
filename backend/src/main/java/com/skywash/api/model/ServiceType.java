package com.skywash.api.model;

public record ServiceType(
    String type,
    String label,
    int rate,
    String unit,
    String icon
) {}
