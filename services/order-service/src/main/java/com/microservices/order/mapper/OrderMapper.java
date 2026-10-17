package com.microservices.order.mapper;

import com.microservices.order.dto.response.OrderResponse;
import com.microservices.order.entity.Order;
import com.microservices.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    OrderResponse toResponse(Order order);

    OrderResponse.ItemResponse toItemResponse(OrderItem item);

    List<OrderResponse.ItemResponse> toItemResponses(List<OrderItem> items);
}