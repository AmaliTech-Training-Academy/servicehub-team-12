package com.servicehub.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @GetMapping
    public String profile(Model model) {
        model.addAttribute("userRole", "USER");
        model.addAttribute("profileUser", stubUser());
        return "profile/profile";
    }

    @PostMapping
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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

    /** Temporary stub — replace with a real User object from the service layer. */
    private Object stubUser() {
        return new Object() {
            public final String firstName  = "Jane";
            public final String lastName   = "Doe";
            public final String email      = "jane.doe@company.com";
            public final String department = "Engineering";
            public final String role       = "USER";
        };
    }
}
