package com.seatflow.auth.service;

import com.seatflow.auth.repository.AppUserRepository;
import com.seatflow.ktx.domain.AppUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void signUp(SignUpCommand command) {
        validate(command);

        if (appUserRepository.existsByLoginId(command.loginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        if (StringUtils.hasText(command.email()) && appUserRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(command.password());
        AppUser user = AppUser.create(
            command.loginId(),
            encodedPassword,
            command.name(),
            blankToNull(command.phone()),
            blankToNull(command.email())
        );

        appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public String findUserNameByLoginId(String loginId) {
        return appUserRepository.findByLoginId(loginId)
            .map(AppUser::getName)
            .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
    }

    private static void validate(SignUpCommand command) {
        if (!StringUtils.hasText(command.loginId())) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        if (!StringUtils.hasText(command.password()) || command.password().length() < 4) {
            throw new IllegalArgumentException("비밀번호는 4자 이상이어야 합니다.");
        }
        if (!StringUtils.hasText(command.name())) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record SignUpCommand(
        String loginId,
        String password,
        String name,
        String phone,
        String email
    ) {
    }
}
