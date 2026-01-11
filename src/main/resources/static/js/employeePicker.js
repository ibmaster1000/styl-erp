const initEmployeePickerModal = (modalElement) => {
	if (!modalElement) {
		return;
	}
	const modalId = modalElement.id;
	const apiUrl = modalElement.dataset.apiUrl;
	const deptUrl = modalElement.dataset.deptUrl;
	const targetInputId = modalElement.dataset.targetInput;
	const searchButton = modalElement.querySelector('[data-action="employee-search"]');
	const selectButton = modalElement.querySelector('[data-action="employee-select"]');
	const nameInput = modalElement.querySelector('[data-field="name"]');
	const deptSelect = modalElement.querySelector('[data-field="dept"]');
	const empCodeInput = modalElement.querySelector('[data-field="empCode"]');
	const resultsBody = modalElement.querySelector('[data-role="results"]');
	const openButtons = document.querySelectorAll(`[data-employee-picker-open="${modalId}"]`);

	if (!apiUrl || !resultsBody) {
		return;
	}

	const bootstrapModal = new bootstrap.Modal(modalElement);
	let selectedRow = null;
	let departmentsLoaded = false;
	let pendingDefaultDept = '';

	const clearSelection = () => {
		selectedRow = null;
		resultsBody.querySelectorAll('tr').forEach((row) => {
			row.classList.remove('selected', 'table-active');
		});
	};

	const setPlaceholder = (message) => {
		resultsBody.innerHTML = `<tr><td colspan="3" class="text-center text-muted">${message}</td></tr>`;
	};

	const renderRows = (items) => {
		clearSelection();
		if (!items || items.length === 0) {
			setPlaceholder('조건에 해당하는 사원이 없습니다.');
			return;
		}
		resultsBody.innerHTML = '';
		items.forEach((item) => {
			const empNo = item.empNo || item.empCode || '';
			const row = document.createElement('tr');
			row.classList.add('employee-picker-row');
			row.dataset.empNo = empNo;
			row.dataset.name = item.name || '';
			row.dataset.dept = item.dept || '';
			row.innerHTML = `
				<td class="text-center">${empNo || '-'}</td>
				<td class="text-center">${item.name || '-'}</td>
				<td class="text-center">${item.dept || '-'}</td>
			`;
			row.addEventListener('click', () => {
				resultsBody.querySelectorAll('tr').forEach((r) => r.classList.remove('selected', 'table-active'));
				row.classList.add('selected', 'table-active');
				selectedRow = row;
			});
			row.addEventListener('dblclick', () => {
				selectedRow = row;
				confirmSelection();
			});
			resultsBody.appendChild(row);
		});
	};

	const fetchDepartments = async () => {
		if (!deptUrl || departmentsLoaded || !deptSelect) {
			return;
		}
		try {
			const response = await fetch(deptUrl);
			if (!response.ok) {
				throw new Error('Failed to load departments');
			}
			const data = await response.json();
			deptSelect.innerHTML = '<option value="">전체</option>';
			(data || []).forEach((dept) => {
				const option = document.createElement('option');
				option.value = dept;
				option.textContent = dept;
				deptSelect.appendChild(option);
			});
			departmentsLoaded = true;
		} catch (error) {
			console.error(error);
		}
	};

	const applyDefaultDept = (defaultDept) => {
		if (!deptSelect) {
			return;
		}
		if (!defaultDept) {
			deptSelect.value = '';
			return;
		}
		const hasOption = Array.from(deptSelect.options || []).some((option) => option.value === defaultDept);
		deptSelect.value = hasOption ? defaultDept : '';
	};

	const fetchEmployees = async () => {
		const params = new URLSearchParams({
			name: nameInput?.value?.trim() || '',
			dept: deptSelect?.value || '',
			empCode: empCodeInput?.value?.trim() || ''
		});
		try {
			const response = await fetch(`${apiUrl}?${params.toString()}`);
			if (!response.ok) {
				throw new Error('Failed to load employees');
			}
			const data = await response.json();
			renderRows(data);
		} catch (error) {
			console.error(error);
			setPlaceholder('사원 정보를 불러오지 못했습니다.');
		}
	};

	const confirmSelection = () => {
		if (!selectedRow) {
			alert('선택된 사원이 없습니다.');
			return;
		}
		const targetInput = document.getElementById(targetInputId);
		if (targetInput) {
			targetInput.value = selectedRow.dataset.empNo || '';
		}
		bootstrap.Modal.getInstance(modalElement)?.hide();
	};

	openButtons.forEach((button) => {
		button.addEventListener('click', (event) => {
			event.preventDefault();
			pendingDefaultDept = button.dataset.defaultDept || '';
			bootstrapModal.show();
		});
	});

	modalElement.addEventListener('shown.bs.modal', async () => {
		await fetchDepartments();
		if (nameInput) {
			nameInput.value = '';
		}
		if (empCodeInput) {
			empCodeInput.value = '';
		}
		applyDefaultDept(pendingDefaultDept);
		pendingDefaultDept = '';
		fetchEmployees();
		nameInput?.focus();
	});

	searchButton?.addEventListener('click', fetchEmployees);
	selectButton?.addEventListener('click', confirmSelection);
};

document.addEventListener('DOMContentLoaded', () => {
	document.querySelectorAll('[data-employee-picker="true"]').forEach((modalElement) => {
		initEmployeePickerModal(modalElement);
	});
});
