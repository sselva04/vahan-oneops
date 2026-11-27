package com.vahan.oneops.material;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** A raw material (OHNS, EN8, ...): its density for weight calculation and its price per kg. */
@Entity
@Table(name = "material")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    /** g/cm3 */
    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal density;

    @Column(name = "price_per_kg", precision = 10, scale = 2)
    private BigDecimal pricePerKg;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Material() {
    }

    public Material(String name, BigDecimal density, BigDecimal pricePerKg) {
        this.name = name;
        this.density = density;
        this.pricePerKg = pricePerKg;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getDensity() { return density; }
    public void setDensity(BigDecimal density) { this.density = density; }
    public BigDecimal getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(BigDecimal pricePerKg) { this.pricePerKg = pricePerKg; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
