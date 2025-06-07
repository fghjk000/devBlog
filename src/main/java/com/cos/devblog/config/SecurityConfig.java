package com.cos.devblog.config;

import com.cos.devblog.config.oauth.PrincipalOauth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private PrincipalOauth2UserService principalOauth2UserService;

    @Bean
    public BCryptPasswordEncoder encoderPwd() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // CSRF 비활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/user/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/join").permitAll() // /join 경로에 대한 POST 요청 허용
                        .requestMatchers("/comment/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().permitAll()
                )
                .formLogin(login -> login
                        .loginPage("/loginForm")          // 절대 경로가 아닌 상대 경로로 바꿔야 함
                        .loginProcessingUrl("/login")     // 절대 경로가 아닌 상대 경로
                        .defaultSuccessUrl("/")            // 절대 경로가 아닌 상대 경로
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")             // 상대 경로로
                        .logoutSuccessUrl("/")            // 상대 경로로
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/loginForm")          // 상대 경로로
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(principalOauth2UserService)
                        )
                );


        return http.build();
    }
}