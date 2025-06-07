package com.cos.devblog.user.entity;

import com.cos.devblog.board.entity.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private String email;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
