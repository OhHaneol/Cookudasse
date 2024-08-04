package com.ormi.cookudasse.auth.controller;

import com.ormi.cookudasse.auth.domain.User;
import com.ormi.cookudasse.auth.dto.FindPasswordRequest;
import com.ormi.cookudasse.auth.dto.LoginRequest;
import com.ormi.cookudasse.auth.dto.SignupRequest;
import com.ormi.cookudasse.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(path = "/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("user", new LoginRequest());
        model.addAttribute("signupRequest", new SignupRequest());
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequest loginRequest, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
            session.setAttribute("user", user);
            return "redirect:/";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인 실패: " + e.getMessage());
            return "redirect:/api/auth/login";
        }
    }

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("signupRequest", new SignupRequest());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute("signupRequest") SignupRequest signupRequest, Model model) {
        try {
            log.debug("Signup request: {}", signupRequest);
            userService.registerUser(signupRequest);
            model.addAttribute("message", "User registered successfully");
            return "redirect:/api/auth/login";
//            return "redirect:/";
        } catch (RuntimeException e) {
            log.error("Error while signup", e);
            model.addAttribute("error", e.getMessage());
            return "redirect:/signup";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, Model model) {
        session.invalidate();
        model.addAttribute("message", "Logged out successfully");
        return "redirect:/home";
    }

    @GetMapping("/find-id")
    public String findId(@RequestParam String email, Model model) {
        try {
            String username = userService.findUsernameByEmail(email);
            model.addAttribute("username", username);
            return "foundUsername";
        } catch (RuntimeException e) {
            model.addAttribute("error", "User not found");
            return "findIdError";
        }
    }

    @GetMapping("/find-password")
    public String showFindPasswordForm(Model model) {
        log.debug("Entering showFindPasswordForm method");
        model.addAttribute("findPasswordRequest", new FindPasswordRequest());
        log.debug("Returning 'find' view");
        return "find";
    }

    @PostMapping("/find-password")
    public String findPassword(@RequestParam String email, Model model) {
        try {
            String username = userService.findUsernameByEmail(email);
            String resetMessage = userService.initiatePasswordReset(email);
            model.addAttribute("message", "사용자 이름은 " + username + "입니다. " + resetMessage);
            return "find";
        } catch (RuntimeException e) {
            model.addAttribute("error", "사용자를 찾을 수 없습니다: " + e.getMessage());
            return "find";
        }
    }

    @DeleteMapping("/delete")
    @ResponseBody
    public ResponseEntity<?> deleteUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            try {
                userService.deleteUser(user.getEmail());
                session.invalidate(); // 세션 무효화
                return ResponseEntity.ok().body("계정이 정상적으로 탈퇴 되었습니다.");
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body("Failed to delete account: " + e.getMessage());
            }
        } else {
            return ResponseEntity.badRequest().body("You must be logged in to delete your account.");
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate(); // 세션 무효화
        redirectAttributes.addFlashAttribute("message", "로그아웃되었습니다.");
        return "redirect:/api/auth/login"; // 로그인 페이지로 리다이렉트
    }


}