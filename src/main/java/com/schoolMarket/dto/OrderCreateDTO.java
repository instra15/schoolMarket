package com.schoolMarket.dto;

import com.schoolMarket.entity.User;
import com.schoolMarket.enums.OrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateDTO {

    @NotBlank
    private String orderNo;

    @NotNull
    private User buyer;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal totalPrice;

    @NotNull
    private OrderStatus status;
}
