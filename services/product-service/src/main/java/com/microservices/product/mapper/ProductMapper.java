package com.microservices.product.mapper;

import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.entity.Category;
import com.microservices.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "category", source = "category", qualifiedByName = "toCategoryInfo")
    ProductResponse toResponse(Product product);

    @Named("toCategoryInfo")
    default ProductResponse.CategoryInfo toCategoryInfo(Category category) {
        if (category == null) return null;
        return new ProductResponse.CategoryInfo(
                category.getId(),
                category.getName(),
                category.getSlug()
        );
    }
}