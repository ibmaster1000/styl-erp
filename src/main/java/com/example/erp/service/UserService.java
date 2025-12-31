package com.example.erp.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.erp.controller.dto.SignupForm;
import com.example.erp.domain.User;
import com.example.erp.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public List<User> findAll() {
		return userRepository.findAll();
	}

	public List<User> searchActiveUsers(String empNo, String name, String dept) {
		return userRepository.searchActiveUsers(StringUtils.hasText(empNo) ? empNo.trim() : null,
				StringUtils.hasText(name) ? name.trim() : null, StringUtils.hasText(dept) ? dept.trim() : null);
	}

	public List<String> findActiveDepartments() {
		return userRepository.findDistinctActiveDepartments();
	}
	public boolean usernameExists(String username) {
		return userRepository.existsByUsername(username);
	}

	public User registerUser(SignupForm form) {
		if (userRepository.existsByUsername(form.getUsername())) {
			throw new IllegalArgumentException("Username already exists");
		}

		User user = new User();
		user.setEmpNo(generateEmpNo());
		user.setUsername(form.getUsername().trim());
		user.setPassword(passwordEncoder.encode(form.getPassword()));
		user.setName(form.getName().trim());
		user.setDept(StringUtils.hasText(form.getDept()) ? form.getDept().trim() : null);
		user.setEmail(StringUtils.hasText(form.getEmail()) ? form.getEmail().trim() : null);
		user.setPhone(StringUtils.hasText(form.getPhone()) ? form.getPhone().trim() : null);
		user.setActive(true);
		user.setRole("ROLE_USER");

		return userRepository.save(user);
	}

	private String generateEmpNo() {
		return "E" + System.currentTimeMillis();
	}
}