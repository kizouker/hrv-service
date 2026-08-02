package com.hrv.backend.web;

import com.hrv.backend.auth.AuthService;
import com.hrv.backend.auth.dto.LoginRequest;
import com.hrv.backend.hrv.HrvService;
import com.hrv.backend.user.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/thyme")
@RequiredArgsConstructor
public class WebController {

    private final AuthService authService;
    private final HrvService hrvService;

    @GetMapping("/login")
    public String loginPage() {
        return "thyme/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpServletResponse response,
                          Model model) {
        try {
            var auth = authService.login(new LoginRequest(email, password));
            Cookie cookie = new Cookie("hrv_token", auth.token());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(86400);
            response.addCookie(cookie);
            return "redirect:/thyme/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Fel email eller lösenord");
            return "thyme/login";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("readings", hrvService.getAll(user));
        model.addAttribute("summary", hrvService.getSummary(user, 30));
        model.addAttribute("trend", hrvService.getTrend(user, 30));
        return "thyme/dashboard";
    }

    @GetMapping("/sync")
    public String syncPage(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        return "thyme/sync";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("hrv_token", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/thyme/login";
    }
}
