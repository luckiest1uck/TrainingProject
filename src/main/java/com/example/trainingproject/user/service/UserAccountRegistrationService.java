package com.example.trainingproject.user.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trainingproject.common.util.EmailNormalizer;
import com.example.trainingproject.user.api.UserAuthenticationSnapshot;
import com.example.trainingproject.user.api.UserRegistrationApi;
import com.example.trainingproject.user.entity.Authority;
import com.example.trainingproject.user.entity.UserEntity;
import com.example.trainingproject.user.entity.UserGrantedAuthority;
import com.example.trainingproject.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountRegistrationService implements UserRegistrationApi {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(
                Objects.requireNonNull(EmailNormalizer.normalize(email), "email must not be null"));
    }

    @Override
    @Transactional
    public UserAuthenticationSnapshot registerPasswordUser(
            String firstName, String lastName, String email, String encodedPassword) {
        return registerUser(firstName, lastName, email, encodedPassword, false);
    }

    @Override
    @Transactional
    public UserAuthenticationSnapshot registerOAuthUser(
            String firstName, String lastName, String email, String encodedPassword) {
        return registerUser(firstName, lastName, email, encodedPassword, true);
    }

    private UserAuthenticationSnapshot registerUser(
            String firstName, String lastName, String email, String encodedPassword, boolean oauthUser) {
        UserEntity user = UserEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(Objects.requireNonNull(EmailNormalizer.normalize(email), "email must not be null"))
                .password(encodedPassword)
                .oauthUser(oauthUser)
                .build();

        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setEnabled(true);
        user.addAuthority(
                UserGrantedAuthority.builder().authority(Authority.USER).build());

        UserEntity saved = userRepository.saveAndFlush(user);
        return new UserAuthenticationSnapshot(
                saved.getId(),
                saved.getEmail(),
                Objects.requireNonNull(saved.getPassword()),
                List.of(Authority.USER.name()),
                saved.isAccountNonExpired(),
                saved.isAccountNonLocked(),
                saved.isCredentialsNonExpired(),
                saved.isEnabled());
    }
}
