package com.skywash.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_types")
public class ServiceTypeEntity {

  @Id
  private String type;

  @Column(nullable = false)
  private String label;

  @Column(nullable = false)
  private int rate;

  @Column(nullable = false)
  private String unit;

  private String icon;

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }
  public int getRate() { return rate; }
  public void setRate(int rate) { this.rate = rate; }
  public String getUnit() { return unit; }
  public void setUnit(String unit) { this.unit = unit; }
  public String getIcon() { return icon; }
  public void setIcon(String icon) { this.icon = icon; }
}
