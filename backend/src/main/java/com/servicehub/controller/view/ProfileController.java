package com.servicehub.controller.view;

import com.servicehub.model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
/**
 * Serves profile management views for authenticated users.
 */

@Controller
@RequestMapping("/profile")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    @GetMapping
    public String profile(Model model, @AuthenticationPrincipal User principal) {
        if (principal != null) {
            model.addAttribute("userRole", principal.getRole().name());
            model.addAttribute("profileUser", principal);
        } else {
            model.addAttribute("userRole", "USER");
            model.addAttribute("profileUser", null);
        }
        return "profile/profile";
    }

    @PostMapping
    public String updateProfile(@RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam String email,
                                @RequestParam(required = false) String department,
                                RedirectAttributes redirectAttributes) {
        // TODO: update user via service layer
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New passwords do not match.");
            return "redirect:/profile";
        }
        // TODO: update password via service layer
        redirectAttributes.addFlashAttribute("success", "Password updated successfully.");
        return "redirect:/profile";
    }
}
