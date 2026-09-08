package com.schoolMarket.dto;

import com.schoolMarket.entity.Category;
import com.schoolMarket.enums.ProductStatus;
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
public class ProductCreateDTO {
    @NotBlank
    private String title;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal price;

    @NotNull
    @DecimalMin("0")
    private BigDecimal stock;//库存

    @NotNull
    private ProductStatus status;

    @NotNull
    private Category category;

    @NotNull
    private Integer version;
}
