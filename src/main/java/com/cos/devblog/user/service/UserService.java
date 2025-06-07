package com.cos.devblog.user.service;

import com.cos.devblog.user.dto.UserDto;
import com.cos.devblog.user.entity.User;
import com.cos.devblog.board.entity.UserRole;
import com.cos.devblog.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public void UserCreate(@Valid UserDto userDto) {
        User user = new User();
        String rawPassword = userDto.getPassword();
        String encPassword = bCryptPasswordEncoder.encode(rawPassword);
        user.setUsername(userDto.getUsername());
        user.setPassword(encPassword);
        user.setRole(UserRole.valueOf("USER"));
        user.setEmail(userDto.getEmail());
        userRepository.save(user);
    }

    public User UserGet(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
}
