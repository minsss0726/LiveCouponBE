package com.project.coupon.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.project.coupon.entity.Users;
import com.project.coupon.repository.UsersRepository;

/**
 * DB 에 저장된 Users 엔티티를 기반으로 인증 정보를 로드하는 서비스.
 *
 * 로그인 시 user_login_id 를 username 으로 사용합니다.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;

    public CustomUserDetailsService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = usersRepository.findByUserLoginId(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with loginId: " + username));

        return CustomUserDetails.from(user);
    }
}

