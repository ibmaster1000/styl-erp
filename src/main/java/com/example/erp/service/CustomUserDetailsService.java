package com.example.erp.service;

import com.example.erp.domain.User;
import com.example.erp.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		String storedPassword = user.getPassword();
		if (storedPassword != null && !storedPassword.startsWith("{")) {
			// Treat raw DB passwords as "{noop}" for legacy/dev use; production should
			// migrate to "{bcrypt}<hash>".
			user.setPassword("{noop}" + storedPassword);
		}

		var auth = Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
		return new CustomUserPrincipal(user, auth);
	}
}
