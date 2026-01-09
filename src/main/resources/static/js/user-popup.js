const employeeRows = document.querySelectorAll('.clickable-row');
const employeeSelectBtn = document.getElementById('btnEmployeeSelect');
const body = document.body;
const targetInputId = body?.dataset?.targetInputId;
const targetNameId = body?.dataset?.targetNameId;
let selectedEmployeeRow = null;

const clearSelection = () => {
    employeeRows.forEach(row => row.classList.remove('selected'));
};

const selectRow = (row) => {
    if (!row) return;
    clearSelection();
    row.classList.add('selected');
    selectedEmployeeRow = row;
};

const applySelection = () => {
    if (!selectedEmployeeRow) {
        alert('선택된 사원이 없습니다.');
        return;
    }

    const payload = {
        empNo: selectedEmployeeRow.dataset.empNo,
        name: selectedEmployeeRow.dataset.name,
        dept: selectedEmployeeRow.dataset.dept
    };

    if (window.opener) {
        if (targetInputId) {
            const targetInput = window.opener.document.getElementById(targetInputId);
            if (targetInput) {
                targetInput.value = payload.empNo || '';
            }
            if (targetNameId) {
                const targetNameInput = window.opener.document.getElementById(targetNameId);
                if (targetNameInput) {
                    targetNameInput.value = payload.name || '';
                }
            }
        } else if (typeof window.opener.onEmployeeSelected === 'function') {
            window.opener.onEmployeeSelected(payload);
        }
    }

    window.close();
};

employeeRows.forEach(row => {
    row.addEventListener('click', () => selectRow(row));
    row.addEventListener('dblclick', () => {
        selectRow(row);
        applySelection();
    });
});

employeeSelectBtn?.addEventListener('click', applySelection);