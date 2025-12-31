package com.example.erp.service;

import com.example.erp.domain.User;
import com.example.erp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
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
}