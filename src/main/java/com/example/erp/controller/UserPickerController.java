package com.example.erp.controller;

import com.example.erp.controller.dto.EmployeePickerItem;
import com.example.erp.domain.User;
import com.example.erp.service.UserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserPickerController {

	private final UserService userService;

	public UserPickerController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/picker")
	public List<EmployeePickerItem> findEmployees(@RequestParam(required = false) String name,
			@RequestParam(required = false) String dept, @RequestParam(required = false) String empCode) {
		List<User> users = userService.searchActiveUsersForPicker(empCode, name, dept);
		return users.stream()
				.map(user -> new EmployeePickerItem(resolveEmpCode(user), user.getName(), user.getDept()))
				.toList();
	}

	@GetMapping("/depts")
	public List<String> findDepartments() {
		return userService.findActiveDepartments();
	}

	private String resolveEmpCode(User user) {
		if (StringUtils.hasText(user.getEmpCode())) {
			return user.getEmpCode();
		}
		return user.getEmpNo();
	}
}