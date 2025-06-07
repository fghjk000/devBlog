package com.cos.devblog.config.oauth;

import com.cos.devblog.config.auth.PrincipalDetails;
import com.cos.devblog.user.entity.User;
import com.cos.devblog.board.entity.UserRole;
import com.cos.devblog.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("getClientRegistration:" + userRequest.getClientRegistration());
        System.out.println("getAttributes:" + super.loadUser(userRequest));

        OAuth2User oauth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getClientId();
        String providerId = oauth2User.getAttribute("sub");
        String username = provider + "_" + providerId;
        String password = new BCryptPasswordEncoder().encode("password");  // 기본 비밀번호 설정
        String email = oauth2User.getAttribute("email");
        UserRole role = UserRole.USER;  // 기본값을 USER로 설정

        // 기존 사용자 검색
        User userEntity = userRepository.findByUsername(username);

        if (userEntity == null) {
            // 새 사용자 등록
            userEntity = new User(
                    null,  // id는 자동 생성되므로 null로 두고
                    username,
                    password,
                    email,
                    role,
                    null,  // createdAt, updatedAt은 생성 시 자동으로 처리하도록 설정
                    null
            );
            userRepository.save(userEntity);
        }

        // 사용자 정보를 PrincipalDetails에 담아서 반환
        return new PrincipalDetails(userEntity, oauth2User.getAttributes());
    }
}
