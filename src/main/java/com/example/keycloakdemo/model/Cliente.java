package com.example.keycloakdemo.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(nullable = false)
   private String nombre;

   @Column(nullable = false, unique = true)
   private String email;

   private String direccion;
   private String telefono;

   @Column(nullable = false)
   private String keycloakUserId;

   @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
   private List<Pedido> pedidos = new ArrayList<>();
   
}
