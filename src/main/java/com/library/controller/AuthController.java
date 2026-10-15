package com.library.controller;

import com.library.service.LibraryException;
import com.library.service.MemberService;
import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final MemberService members;

    public AuthController(MemberService members) {
        this.members = members;
    }

    @GetMapping("/")
    public String root(Authentication authentication) {
        boolean librarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
        return librarian ? "redirect:/librarian" : "redirect:/member";
    }

    @GetMapping("/login")
    public String login(Principal principal) {
        return principal != null ? "redirect:/" : "login";
    }

    @GetMapping("/register")
    public String registerForm(Principal principal) {
        return principal != null ? "redirect:/" : "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String email,
                           @RequestParam String password, Model model) {
        try {
            members.register(name, email, password);
        } catch (LibraryException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }
        return "redirect:/login?registered";
    }
}
