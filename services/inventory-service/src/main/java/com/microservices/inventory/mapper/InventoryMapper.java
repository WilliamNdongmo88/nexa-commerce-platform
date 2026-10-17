package com.microservices.inventory.mapper;

import com.microservices.inventory.dto.response.InventoryResponse;
import com.microservices.inventory.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "available", expression = "java(inventory.getAvailable())")
    @Mapping(target = "lowStock", expression = "java(inventory.isLowStock())")
    InventoryResponse toResponse(Inventory inventory);
}