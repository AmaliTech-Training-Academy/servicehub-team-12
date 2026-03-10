package com.servicehub.service;

import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");
        if (name == null) name = email;

        // Provision new user or update name
        String finalName = name;
        userRepository.findByEmail(email).ifPresentOrElse(
                _ -> { /* already registered — no-op */ },
                () -> userRepository.save(
                        User.builder()
                                .email(email)
                                .fullName(finalName)
                                .role(Role.USER)
                                .provider("google")
                                .isActive(true)
                                .build()
                )
        );

        return oAuth2User;
    }
}

