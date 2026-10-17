package com.microservices.order.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDto {
    private UUID id;
    private String sku;
    private String name;
    private BigDecimal price;
    private String currency;
    private Boolean active;
}