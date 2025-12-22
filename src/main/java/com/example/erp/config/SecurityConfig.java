package com.example.erp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class SecurityConfig {
	@Bean
	public PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(
	        HttpSecurity http,
	        PasswordEncoder passwordEncoder,
	        UserDetailsService userDetailsService
	) throws Exception {
	    AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
	    builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
	    return builder.build();
	}


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
          .csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(auth -> auth
              .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
              .anyRequest().hasRole("ADMIN") // ✅ 관리자만 시스템 접근
          )
          .formLogin(form -> form
              .loginPage("/login")
              .successHandler(adminOnlySuccessHandler()) // ✅ 관리자 아니면 로그인 차단
              .failureUrl("/login?error")
              .permitAll()
          )
          .logout(logout -> logout
              .logoutUrl("/logout")
              .logoutSuccessUrl("/login?logout")
          );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler adminOnlySuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                request.getSession().invalidate(); // ✅ 세션 제거
                response.sendRedirect("/login?error=not_admin"); // ✅ 관리자 아니면 로그인 “차단”
                return;
            }

            response.sendRedirect("/dashboard");
        };
    }
}

