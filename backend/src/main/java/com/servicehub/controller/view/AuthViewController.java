package com.servicehub.controller.view;

import com.servicehub.config.JwtService;
import com.servicehub.dto.AuthResponse;
import com.servicehub.dto.RegisterRequest;
import com.servicehub.exception.AuthException;
import com.servicehub.model.User;
import com.servicehub.repository.UserRepository;
import com.servicehub.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthViewController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest registerRequest,
                           BindingResult result,
                           Model model) {
        if (result.hasErrors()) {
            model.addAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
            return "auth/register";
        }
        try {
            authService.register(registerRequest);
            return "redirect:/login?registered";
        } catch (AuthException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    /**
     * After Google OAuth2 succeeds Spring redirects here.
     * We look up the provisioned user, issue a JWT cookie, then forward to /dashboard.
     */
    @GetMapping("/oauth2/success")
    public String oauth2Success(@AuthenticationPrincipal OAuth2User principal,
                                HttpServletResponse response) {
        if (principal == null) {
            return "redirect:/login?error";
        }
        String email = principal.getAttribute("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("OAuth2 user not found after provisioning"));
        String token = jwtService.generateToken(user);
        AuthResponse auth = AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .build();
        authService.issueJwtCookie(auth, response);
        return "redirect:/dashboard";
    }

    /**
     * Thymeleaf logout — called via a plain GET link from the sidebar.
     * Blacklists the JWT, clears the cookie, invalidates the session and
     * redirects to the login page.
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request);

        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return "redirect:/login?logout";
    }
}
