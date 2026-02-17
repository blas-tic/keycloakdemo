package com.example.keycloakdemo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.keycloakdemo.model.EstadoPedido;

import lombok.Data;

@Data
public class PedidoResumenDTO {
   private Long id;
   private LocalDateTime fechaPedido;
   private EstadoPedido estado;
   private BigDecimal total;
   private Integer cantidadItems; // Número total de items (sum de cantidades)

}
