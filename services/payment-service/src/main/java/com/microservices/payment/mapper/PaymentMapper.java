package com.microservices.payment.mapper;

import com.microservices.payment.dto.response.PaymentResponse;
import com.microservices.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "status", expression = "java(payment.getStatus().name())")
    PaymentResponse toResponse(Payment payment);
}