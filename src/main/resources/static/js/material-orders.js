document.addEventListener('DOMContentLoaded', () => {
	const styleInput = document.getElementById('mo-style-input');
	const colorInput = document.getElementById('mo-color-input');
	const agreementInput = document.getElementById('mo-agreement-input');
	const verifyButton = document.getElementById('mo-style-verify');
	const statusMessage = document.getElementById('mo-style-status');
	const searchButton = document.getElementById('mo-search-button');
	const styleChip = document.getElementById('mo-style-chip');
	const ruleHead = document.getElementById('mo-rule-head');
	const ruleBody = document.getElementById('mo-rule-body');
	const supplierBody = document.getElementById('mo-supplier-body');
	const supplierSummary = document.getElementById('mo-supplier-summary');
	const materialBody = document.getElementById('mo-material-body');
	const materialNote = document.getElementById('mo-material-note');
	const warehouseModalEl = document.getElementById('moWarehouseModal');
	const warehouseKeyword = document.getElementById('moWarehouseKeyword');
	const warehouseSearchBtn = document.getElementById('moWarehouseSearchBtn');
	const warehouseResults = document.getElementById('moWarehouseResults');
	const warehouseSelectBtn = document.getElementById('moWarehouseSelectBtn');

	let styleVerified = false;
	let warehouseTargetRow = null;
	let selectedWarehouse = null;
	let inputState = { styleCode: '', colorCode: '', agreementCode: '' };
	let queryState = null;

	const createModal = (element, label) => {
		if (window.bootstrap?.Modal) {
			return new window.bootstrap.Modal(element);
		}
		console.warn(`${label} 모달을 초기화할 수 없습니다. Bootstrap JS가 필요합니다.`);
		return {
			show: () => { },
			hide: () => { }
		};
	};

	const warehouseModal = createModal(warehouseModalEl, '창고');

	const updateSelectOptions = (select, options, placeholder) => {
		select.innerHTML = '';
		const defaultOption = document.createElement('option');
		defaultOption.value = '';
		defaultOption.textContent = placeholder;
		select.appendChild(defaultOption);
		(options || []).forEach((optionValue) => {
			const option = document.createElement('option');
			option.value = optionValue;
			option.textContent = optionValue;
			select.appendChild(option);
		});
	};

	const clearStatusMessage = () => {
		statusMessage.textContent = '';
		statusMessage.classList.remove('text-success', 'text-danger');
		statusMessage.classList.add('text-muted');
	};

	const setStatusMessage = (message, isSuccess) => {
		statusMessage.textContent = message;
		statusMessage.classList.remove('text-muted', 'text-success', 'text-danger');
		statusMessage.classList.add(isSuccess ? 'text-success' : 'text-danger');
	};

	const updateSearchButtonState = () => {
		searchButton.disabled = !(styleVerified && colorInput.value && agreementInput.value);
	};

	const resetOptionState = () => {
		styleVerified = false;
		colorInput.disabled = true;
		agreementInput.disabled = true;
		updateSelectOptions(colorInput, [], '색상 선택');
		updateSelectOptions(agreementInput, [], '생산합의코드 선택');
		updateSearchButtonState();
	};

	const resetSupplierTable = (message) => {
		supplierBody.innerHTML = `\n\t\t<tr>\n\t\t\t<td colspan="6" class="mo-placeholder">${message}</td>\n\t\t</tr>\n\t`;
	};

	const resetMaterialTable = (message) => {
		materialBody.innerHTML = `\n\t\t<tr>\n\t\t\t<td colspan="15" class="mo-placeholder">${message}</td>\n\t\t</tr>\n\t`;
	};

	const resetContextState = ({ resetMatrix } = {}) => {
		styleChip.textContent = '-';
		resetSupplierTable('검색된 자재처가 없습니다.');
		resetMaterialTable('조회 후 원부자재 목록이 표시됩니다.');
		supplierSummary.textContent = '조건을 선택 후 조회하세요.';
		materialNote.textContent = '조회 전에는 목록이 표시되지 않습니다.';
		queryState = null;
		if (resetMatrix) {
			renderMatrixStatus('품번을 확인하세요.');
		}
	};

	const renderMatrixStatus = (message) => {
		ruleHead.innerHTML = '';
		ruleBody.innerHTML = '';
		const headRow = document.createElement('tr');
		headRow.innerHTML = '<th class="text-start">색상</th><th class="text-muted">-</th>';
		ruleHead.appendChild(headRow);

		const emptyRow = document.createElement('tr');
		const td = document.createElement('td');
		td.colSpan = 2;
		td.className = 'text-center text-muted';
		td.textContent = message;
		emptyRow.appendChild(td);
		ruleBody.appendChild(emptyRow);
	};

	const renderMatrixTable = (colors, sizes, quantities) => {
		ruleHead.innerHTML = '';
		ruleBody.innerHTML = '';
		const safeColors = colors && colors.length > 0 ? colors : [{ code: '-', name: '-' }];
		const safeSizes = sizes && sizes.length > 0 ? sizes : [{ code: '-', name: '-' }];
		const headRow = document.createElement('tr');
		const colorHead = document.createElement('th');
		colorHead.textContent = '색상';
		colorHead.classList.add('text-start');
		headRow.appendChild(colorHead);
		safeSizes.forEach((size) => {
			const th = document.createElement('th');
			th.textContent = size.name || size.code || '-';
			headRow.appendChild(th);
		});
		ruleHead.appendChild(headRow);

		safeColors.forEach((color) => {
			const row = document.createElement('tr');
			const colorCell = document.createElement('td');
			colorCell.classList.add('text-start');
			colorCell.textContent = color.name || color.code || '-';
			row.appendChild(colorCell);
			safeSizes.forEach((size) => {
				const td = document.createElement('td');
				const key = `${color.code || ''}|${size.code || ''}`;
				const qty = quantities && Object.prototype.hasOwnProperty.call(quantities, key)
					? quantities[key]
					: '-';
				td.textContent = qty;
				row.appendChild(td);
			});
			ruleBody.appendChild(row);
		});
	};

	const applyMatrix = (matrix) => {
		if (!matrix) {
			renderMatrixStatus('색상/사이즈 정보를 불러오지 못했습니다.');
			return;
		}
		const colors = Array.isArray(matrix.colors) ? matrix.colors : [];
		const sizes = Array.isArray(matrix.sizes) ? matrix.sizes : [];
		const quantities = matrix.quantities && typeof matrix.quantities === 'object' ? matrix.quantities : {};
		renderMatrixTable(colors, sizes, quantities);
	};

	const verifyStyleCode = async () => {
		const styleCode = styleInput.value.trim();
		resetContextState({ resetMatrix: true });
		if (!styleCode) {
			resetOptionState();
			setStatusMessage('품번을 입력하세요.', false);
			return;
		}
		try {
			const response = await fetch(`/api/material-orders/context?styleCode=${encodeURIComponent(styleCode)}`);
			if (!response.ok) {
				throw new Error('context');
			}
			const data = await response.json();
			if (data.styleVerified) {
				styleVerified = true;
				setStatusMessage(data.message || '품번이 확인되었습니다.', true);
				updateSelectOptions(colorInput, data.colors || [], '색상 선택');
				updateSelectOptions(agreementInput, data.agreements || [], '생산합의코드 선택');
				colorInput.disabled = false;
				agreementInput.disabled = false;
				applyMatrix(data.colorSizeMatrix);
				updateSearchButtonState();
			} else {
				resetOptionState();
				setStatusMessage(data.message || '없는 품번입니다.', false);
				applyMatrix(data.colorSizeMatrix);
			}
		} catch (error) {
			resetOptionState();
			setStatusMessage('품번 확인 중 오류가 발생했습니다.', false);
			renderMatrixStatus('색상/사이즈 정보를 불러오지 못했습니다.');
		}
	};

	const getTodayString = () => {
		const today = new Date();
		const yyyy = today.getFullYear();
		const mm = String(today.getMonth() + 1).padStart(2, '0');
		const dd = String(today.getDate()).padStart(2, '0');
		return `${yyyy}-${mm}-${dd}`;
	};

	const buildSupplierRow = (supplier, today) => {
		const tr = document.createElement('tr');
		tr.dataset.supplierCode = supplier.supplierCode || '';
		tr.dataset.supplierName = supplier.supplierName || '';
		tr.innerHTML = `\n\t\t<td>\n\t\t\t<div class="mo-supplier-cell">\n\t\t\t\t<span class="mo-supplier-code">${supplier.supplierCode || '-'}</span>\n\t\t\t\t<span class="mo-supplier-name">${supplier.supplierName || '-'}</span>\n\t\t\t</div>\n\t\t</td>\n\t\t<td><input type="date" class="form-control form-control-sm order-date" value="${today}" disabled></td>\n\t\t<td><input type="date" class="form-control form-control-sm due-date"></td>\n\t\t<td>\n\t\t\t<div class="input-group">\n\t\t\t\t<input type="text" class="form-control form-control-sm warehouse-input" placeholder="창고 선택" readonly>\n\t\t\t\t<input type="hidden" class="warehouse-code">\n\t\t\t\t<button type="button" class="btn btn--outline warehouse-search">검색</button>\n\t\t\t</div>\n\t\t</td>\n\t\t<td><input type="text" class="form-control form-control-sm order-remark" maxlength="200"></td>\n\t\t<td>\n\t\t\t<button type="button" class="btn ${supplier.ordered ? 'btn--outline' : 'btn--primary'} order-action-btn" ${supplier.ordered ? 'disabled' : ''}>${supplier.ordered ? '발주완료' : '발주'}</button>\n\t\t</td>\t\n\t`;
		const warehouseBtn = tr.querySelector('.warehouse-search');
		warehouseBtn.addEventListener('click', () => {
			warehouseTargetRow = tr;
			selectedWarehouse = null;
			warehouseKeyword.value = '';
			resetWarehouseResults('조회 중입니다...');
			warehouseModal.show();
			loadWarehouseResults('');
		});

		const actionBtn = tr.querySelector('.order-action-btn');
		actionBtn.addEventListener('click', () => handleOrderAction(tr, supplier));
		return tr;
	};

	const renderSuppliers = (list) => {
		supplierBody.innerHTML = '';
		if (!list || list.length === 0) {
			resetSupplierTable('조건에 맞는 자재처가 없습니다.');
			supplierSummary.textContent = '검색된 자재처가 없습니다.';
			return;
		}
		const today = getTodayString();
		supplierSummary.textContent = `총 ${list.length}건`;
		list.forEach((supplier) => {
			const row = buildSupplierRow(supplier, today);
			supplierBody.appendChild(row);
		});
	};

	const renderMaterials = (list, selectedColor) => {
		materialBody.innerHTML = '';
		if (!list || list.length === 0) {
			resetMaterialTable('조회된 원부자재가 없습니다.');
			return;
		}
		list.forEach((item) => {
			const tr = document.createElement('tr');
			const supplierLabel = [item.supplierCode, item.supplierName].filter(Boolean).join(' / ');
			tr.innerHTML = `\n\t\t<td>${selectedColor || item.colorCode || '-'}</td>\n\t\t<td>${item.category || '-'}</td>\n\t\t<td>${item.materialName || '-'}</td>\n\t\t<td>${item.materialUsage || '-'}</td>\n\t\t<td>${item.spec || '-'}</td>\n\t\t<td>${item.materialColor || '-'}</td>\n\t\t<td>${item.uom || '-'}</td>\n\t\t<td>${formatNumber(item.qtyPerPiece)}</td>\n\t\t<td>${supplierLabel || '-'}</td>\n\t\t<td>${formatNumber(item.lossRate)}</td>\n\t\t<td>${formatDecimal(item.orderAmount, 3)}</td>\n\t\t<td>${formatDecimal(item.orderPrice, 2)}</td>\n\t\t<td>${item.orderUom || '-'}</td>\n\t\t<td>${formatDecimal(item.unitPrice, 2)}</td>\n\t\t<td>${item.remark || '-'}</td>\t\n\t`;
			materialBody.appendChild(tr);
		});
	};

	const updateInputState = () => {
		inputState = {
			styleCode: styleInput.value.trim(),
			colorCode: colorInput.value.trim(),
			agreementCode: agreementInput.value.trim()
		};
	};

	const handleOrderAction = async (row, supplier) => {
		if (supplier.ordered) {
			return;
		}
		if (!confirm('발주하시겠습니까?')) {
			return;
		}
		const applied = queryState || {
			styleCode: styleInput.value.trim(),
			colorCode: colorInput.value.trim(),
			agreementCode: agreementInput.value.trim()
		};
		const payload = {
			styleCode: applied.styleCode || null,
			prdAgreeCode: applied.agreementCode || null,
			colorCode: applied.colorCode || null,
			supplierCode: supplier.supplierCode || null,
			orderDate: row.querySelector('.order-date')?.value || null,
			dueDate: row.querySelector('.due-date')?.value || null,
			warehouseCode: row.querySelector('.warehouse-code')?.value || null,
			remark: row.querySelector('.order-remark')?.value || null
		};
		try {
			const response = await fetch('/api/material-orders/request', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(payload)
			});
			if (!response.ok) {
				throw new Error('order');
			}
			const result = await response.json();
			if (result && result.success === false) {
				alert(result.message || '발주 처리에 실패했습니다.');
				return;
			}
			const btn = row.querySelector('.order-action-btn');
			btn.textContent = '발주완료';
			btn.classList.remove('btn--primary');
			btn.classList.add('btn--outline');
			btn.disabled = true;
		} catch (error) {
			alert('발주 처리 중 오류가 발생했습니다.');
		}
	};

	const doSearch = async () => {
		updateInputState();
		const { styleCode, colorCode, agreementCode } = inputState;
		if (!styleVerified) {
			alert('품번 확인 후 조회할 수 있습니다.');
			return;
		}
		if (!colorCode || !agreementCode) {
			alert('색상과 생산합의코드를 선택하세요.');
			return;
		}
		const requestedState = { styleCode, colorCode, agreementCode };
		try {
			const response = await fetch(`/api/material-orders/search?styleCode=${encodeURIComponent(styleCode)}&agreementCode=${encodeURIComponent(agreementCode)}&colorCode=${encodeURIComponent(colorCode)}`);
			if (!response.ok) {
				throw new Error('search');
			}
			const data = await response.json();
			queryState = requestedState;
			styleChip.textContent = queryState.styleCode || '-';
			renderSuppliers(data.suppliers || []);
			renderMaterials(data.materialsToOrder || [], queryState.colorCode);
			materialNote.textContent = `품번 ${queryState.styleCode} / 색상 ${queryState.colorCode} / 생산합의 ${queryState.agreementCode}`;
			if (data.colorSizeMatrix) {
				applyMatrix(data.colorSizeMatrix);
			}
		} catch (error) {
			resetSupplierTable('조건에 맞는 자재처가 없습니다.');
			resetMaterialTable('조회된 원부자재가 없습니다.');
			supplierSummary.textContent = '조회 중 오류가 발생했습니다.';
			materialNote.textContent = '조회 중 오류가 발생했습니다.';
		}
	};

	const resetWarehouseResults = (message) => {
		warehouseResults.innerHTML = `\n\t\t<tr>\n\t\t\t<td colspan="3" class="text-center text-muted">${message}</td>\n\t\t</tr>\n\t`;
	};

	const renderWarehouseResults = (list) => {
		warehouseResults.innerHTML = '';
		if (!list || list.length === 0) {
			resetWarehouseResults('검색 결과가 없습니다.');
			return;
		}
		list.forEach((warehouse) => {
			const row = document.createElement('tr');
			row.className = 'mo-warehouse-row';
			row.innerHTML = `\n\t\t<td class="text-start">${warehouse.code || ''}</td>\n\t\t<td class="text-start">${warehouse.codeName || ''}</td>\n\t\t<td class="text-start">${warehouse.remark || ''}</td>\n\t`;
			row.addEventListener('click', () => {
				warehouseResults.querySelectorAll('.mo-warehouse-row').forEach((r) => r.classList.remove('table-primary'));
				row.classList.add('table-primary');
				selectedWarehouse = warehouse;
			});
			row.addEventListener('dblclick', () => {
				selectedWarehouse = warehouse;
				applyWarehouseSelection();
			});
			warehouseResults.appendChild(row);
		});
	};

	const loadWarehouseResults = async (keyword) => {
		try {
			const response = await fetch(`/api/codes/warehouses?keyword=${encodeURIComponent(keyword || '')}`);
			if (!response.ok) {
				throw new Error('warehouse');
			}
			const data = await response.json();
			renderWarehouseResults(data);
		} catch (error) {
			renderWarehouseResults([]);
		}
	};

	const applyWarehouseSelection = () => {
		if (!warehouseTargetRow || !selectedWarehouse) {
			alert('선택된 창고가 없습니다.');
			return;
		}
		const code = selectedWarehouse.code || '';
		const name = selectedWarehouse.codeName || '';
		const input = warehouseTargetRow.querySelector('.warehouse-input');
		const hidden = warehouseTargetRow.querySelector('.warehouse-code');
		if (input) {
			input.value = [code, name].filter(Boolean).join(' ');
		}
		if (hidden) {
			hidden.value = code;
		}
		warehouseModal.hide();
	};

	const formatNumber = (value) => {
		if (value === null || value === undefined) {
			return '-';
		}
		const num = Number(value);
		return Number.isNaN(num) ? '-' : num.toLocaleString();
	};

	const formatDecimal = (value, digits) => {
		if (value === null || value === undefined) {
			return '-';
		}
		const num = Number(value);
		if (Number.isNaN(num)) {
			return '-';
		}
		return num.toLocaleString(undefined, {
			minimumFractionDigits: digits,
			maximumFractionDigits: digits
		});
	};

	verifyButton.addEventListener('click', verifyStyleCode);
	searchButton.addEventListener('click', doSearch);

	styleInput.addEventListener('input', () => {
		clearStatusMessage();
		resetOptionState();
		resetContextState({ resetMatrix: true });
		updateInputState();
	});

	colorInput.addEventListener('change', () => {
		updateSearchButtonState();
		updateInputState();
		if (queryState) {
			materialNote.textContent = '조건이 변경되었습니다. 조회를 눌러 반영하세요.';
		}
	});

	agreementInput.addEventListener('change', () => {
		updateSearchButtonState();
		updateInputState();
		if (queryState) {
			materialNote.textContent = '조건이 변경되었습니다. 조회를 눌러 반영하세요.';
		}
	});

	warehouseSearchBtn.addEventListener('click', () => {
		const keyword = warehouseKeyword.value.trim();
		loadWarehouseResults(keyword);
	});

	warehouseSelectBtn.addEventListener('click', applyWarehouseSelection);

	warehouseKeyword.addEventListener('keydown', (event) => {
		if (event.key === 'Enter') {
			event.preventDefault();
			loadWarehouseResults(warehouseKeyword.value.trim());
		}
	});

	resetOptionState();
	resetContextState({ resetMatrix: true });
	clearStatusMessage();
});
