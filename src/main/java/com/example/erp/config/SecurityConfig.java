package com.example.erp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;


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
        		  .requestMatchers("/login", "/signup", "/css/**", "/js/**", "/images/**").permitAll()
                  .anyRequest().authenticated()
          )
          .formLogin(form -> form
              .loginPage("/login")
              .successHandler(roleAwareSuccessHandler())
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
    public AuthenticationSuccessHandler roleAwareSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            String loginRole = request.getParameter("loginRole");
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if ("ADMIN".equalsIgnoreCase(loginRole) && !isAdmin) {
                request.getSession().invalidate();
                response.sendRedirect("/login?error=not_admin");
                return;
            }

            response.sendRedirect("/dashboard");
        };
    }
}