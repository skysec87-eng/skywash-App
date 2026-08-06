package com.skywash.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "partners")
public class PartnerEntity {

  @Id
  private String id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String city;

  @Column(nullable = false)
  private String area;

  @Column(nullable = false, length = 512)
  private String address;

  @Column(nullable = false)
  private double lat;

  @Column(nullable = false)
  private double lng;

  @Column(nullable = false)
  private double rating;

  private String phone;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }
  public String getArea() { return area; }
  public void setArea(String area) { this.area = area; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public double getLat() { return lat; }
  public void setLat(double lat) { this.lat = lat; }
  public double getLng() { return lng; }
  public void setLng(double lng) { this.lng = lng; }
  public double getRating() { return rating; }
  public void setRating(double rating) { this.rating = rating; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
}
