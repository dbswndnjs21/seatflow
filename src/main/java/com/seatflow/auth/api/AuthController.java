package com.seatflow.auth.api;

import com.seatflow.auth.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("인증이 필요합니다."));
        }

        String loginId = authentication.getName();
        try {
            String name = authService.findUserNameByLoginId(loginId);
            return ResponseEntity.ok(new MeResponse(loginId, name));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpRequest request) {
        try {
            authService.signUp(new AuthService.SignUpCommand(
                request.loginId(),
                request.password(),
                request.name(),
                request.phone(),
                request.email()
            ));
            return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("회원가입이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    public record SignUpRequest(String loginId, String password, String name, String phone, String email) {
    }

    public record MeResponse(String loginId, String name) {
    }

    public record MessageResponse(String message) {
    }
}
