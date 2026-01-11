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
	public List<EmployeePickerItem> findEmployees(@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "dept", required = false) String dept,
			@RequestParam(name = "empCode", required = false) String empCode) {
		List<User> users = userService.searchActiveUsersForPicker(empCode, name, dept);
		return users.stream()
				.map(user -> new EmployeePickerItem(user.getEmpNo(), resolveEmpCode(user), user.getName(), user.getDept()))
				.toList();
	}

	@GetMapping("/search")
	public List<EmployeePickerItem> searchEmployees(@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "dept", required = false) String dept,
			@RequestParam(name = "empCode", required = false) String empCode) {
		return findEmployees(name, dept, empCode);
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