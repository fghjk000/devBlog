package com.cos.devblog.config.auth;



import com.cos.devblog.user.entity.User;
import com.cos.devblog.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PrincipalDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // 디버그 로그 추가
        System.out.println("User found: " + user.getUsername());

        // PrincipalDetails 객체를 반환하도록 수정
        return new com.cos.devblog.config.auth.PrincipalDetails(user);
    }
}

