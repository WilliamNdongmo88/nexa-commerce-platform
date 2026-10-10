package com.microservices.user.mapper;

import com.microservices.user.dto.response.UserProfileResponse;
import com.microservices.user.entity.UserProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    UserProfileResponse toResponse(UserProfile entity);
}