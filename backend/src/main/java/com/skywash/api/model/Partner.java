package com.skywash.api.model;

public record Partner(
    String id,
    String name,
    String city,
    String area,
    String address,
    double lat,
    double lng,
    double rating,
    String phone,
    boolean isActive
) {}
