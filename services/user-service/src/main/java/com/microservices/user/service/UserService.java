package com.microservices.user.service;

import com.microservices.user.dto.request.UpdateProfileRequest;
import com.microservices.user.dto.response.UserProfileResponse;
import com.microservices.user.entity.UserProfile;
import com.microservices.user.exception.UserProfileNotFoundException;
import com.microservices.user.mapper.UserProfileMapper;
import com.microservices.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    // ============================================================
    // GET
    // ============================================================
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID id) {
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> new UserProfileNotFoundException(id));
        return userProfileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByEmail(String email) {
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new UserProfileNotFoundException(email));
        return userProfileMapper.toResponse(profile);
    }

    // ============================================================
    // UPDATE
    // ============================================================
    @Transactional
    public UserProfileResponse updateProfile(UUID id, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> new UserProfileNotFoundException(id));

        // Mise à jour partielle : on ne touche que les champs non-null
        if (request.firstName() != null) profile.setFirstName(request.firstName());
        if (request.lastName() != null) profile.setLastName(request.lastName());
        if (request.phone() != null) profile.setPhone(request.phone());
        if (request.birthDate() != null) profile.setBirthDate(request.birthDate());
        if (request.addressLine1() != null) profile.setAddressLine1(request.addressLine1());
        if (request.addressLine2() != null) profile.setAddressLine2(request.addressLine2());
        if (request.city() != null) profile.setCity(request.city());
        if (request.postalCode() != null) profile.setPostalCode(request.postalCode());
        if (request.country() != null) profile.setCountry(request.country());
        if (request.avatarUrl() != null) profile.setAvatarUrl(request.avatarUrl());
        if (request.bio() != null) profile.setBio(request.bio());
        if (request.locale() != null) profile.setLocale(request.locale());
        if (request.timezone() != null) profile.setTimezone(request.timezone());

        UserProfile updated = userProfileRepository.save(profile);
        log.info("Profile updated for user {}", id);

        // TODO étape 13 : publier event "user.profile.updated"

        return userProfileMapper.toResponse(updated);
    }

    // ============================================================
    // DELETE
    // ============================================================
    @Transactional
    public void deleteProfile(UUID id) {
        if (!userProfileRepository.existsById(id)) {
            throw new UserProfileNotFoundException(id);
        }
        userProfileRepository.deleteById(id);
        log.info("Profile deleted for user {}", id);
    }

    // ============================================================
    // CREATE (appelé par le consumer Kafka plus tard)
    // ============================================================
    @Transactional
    public UserProfile createProfile(UUID id, String email, String firstName, String lastName) {
        if (userProfileRepository.existsById(id)) {
            log.warn("Profile already exists for user {}, skipping creation", id);
            return userProfileRepository.findById(id).orElseThrow();
        }

        UserProfile profile = UserProfile.builder()
                .id(id)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .locale("fr")
                .timezone("UTC")
                .build();

        UserProfile saved = userProfileRepository.save(profile);
        log.info("Profile created for user {} ({})", id, email);
        return saved;
    }
}