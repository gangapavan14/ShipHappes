package com.shiphappens.logistics.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "customers")
public class Customer {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column(name="customer_code", nullable=false, unique=true) private String customerCode;
 @Column(nullable=false) private String name; @Column(nullable=false) private String email; @Column(nullable=false) private String phone;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Statuses.CustomerStatus status = Statuses.CustomerStatus.ACTIVE;
 @Column(name="created_at", updatable=false) private Instant createdAt; @Column(name="updated_at") private Instant updatedAt;
 @PrePersist void created(){ createdAt=Instant.now(); updatedAt=createdAt; } @PreUpdate void updated(){ updatedAt=Instant.now(); }
 public Long getId(){return id;} public String getCustomerCode(){return customerCode;} public void setCustomerCode(String v){customerCode=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;} public Statuses.CustomerStatus getStatus(){return status;} public void setStatus(Statuses.CustomerStatus v){status=v;} public Instant getCreatedAt(){return createdAt;}
}
