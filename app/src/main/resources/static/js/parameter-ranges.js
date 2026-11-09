export function initializeParameterRangeManagement(apiUrl, i18n) {
        var prangeEditModal = new bootstrap.Modal(document.getElementById('paramRangeEditModal'));
        var editingId = null;

        document.getElementById('parametersModal').addEventListener('show.bs.modal', function () {
            loadPrangeTable();
        });

        function loadPrangeTable() {
            var spinner = document.getElementById('prange-spinner');
            var tableWrap = document.getElementById('prange-table-wrap');
            var errEl = document.getElementById('prange-error');
            var tbody = document.getElementById('prange-tbody');

            spinner.classList.remove('d-none');
            tableWrap.classList.add('d-none');
            errEl.classList.add('d-none');

            fetch(apiUrl('/api/parameter-ranges'))
                .then(function (res) {
                    if (!res.ok) throw new Error(i18n.error.loadParameterRanges + ': ' + res.status);
                    return res.json();
                })
                .then(function (data) {
                    tbody.innerHTML = data.map(function (r) {
                        return '<tr>' +
                            '<td><strong>' + (r.parameter ?? '-') + '</strong></td>' +
                            '<td>' + (r.level1 ?? '-') + '</td>' +
                            '<td>' + (r.level2 ?? '-') + '</td>' +
                            '<td><span class="badge bg-success">' + (r.level3 ?? '-') + '</span></td>' +
                            '<td>' + (r.level4 ?? '-') + '</td>' +
                            '<td>' + (r.level5 ?? '-') + '</td>' +
                            '<td>' + (r.unit ?? '-') + '</td>' +
                            '<td>' +
                            '<button class="btn btn-sm btn-outline-primary btn-edit-prange" ' +
                            'data-id="' + r.id + '" ' +
                            'data-parameter="' + (r.parameter ?? '') + '" ' +
                            'data-level1="' + (r.level1 ?? '') + '" ' +
                            'data-level2="' + (r.level2 ?? '') + '" ' +
                            'data-level3="' + (r.level3 ?? '') + '" ' +
                            'data-level4="' + (r.level4 ?? '') + '" ' +
                            'data-level5="' + (r.level5 ?? '') + '" ' +
                            'data-unit="' + (r.unit ?? '') + '">' +
                            '<i class="bi bi-pencil"></i>' +
                            '</button>' +
                            '<button class="btn btn-sm btn-outline-danger btn-delete-prange" data-id="' + r.id + '">' +
                            '<i class="bi bi-trash"></i>' +
                            '</button>' +
                            '</td>' +
                            '</tr>';
                    }).join('');
                    spinner.classList.add('d-none');
                    tableWrap.classList.remove('d-none');
                })
                .catch(function (err) {
                    spinner.classList.add('d-none');
                    errEl.innerText = i18n.error.loadRanges + ': ' + err.message;
                    errEl.classList.remove('d-none');
                });
        }

        // Open in edit mode
        document.getElementById('prange-tbody').addEventListener('click', function (e) {
            var btn = e.target.closest('.btn-edit-prange');
            if (!btn) return;

            editingId = btn.dataset.id;
            document.getElementById('prange-edit-label').innerText = i18n.label.editRange;
            document.getElementById('prange-id').value = editingId;
            document.getElementById('prange-parameter').value = btn.dataset.parameter;
            document.getElementById('prange-level1').value = btn.dataset.level1;
            document.getElementById('prange-level2').value = btn.dataset.level2;
            document.getElementById('prange-level3').value = btn.dataset.level3;
            document.getElementById('prange-level4').value = btn.dataset.level4;
            document.getElementById('prange-level5').value = btn.dataset.level5;
            document.getElementById('prange-unit').value = btn.dataset.unit;
            document.getElementById('prange-feedback').className = 'alert d-none';

            prangeEditModal.show();
        });

        // Delete range
        document.getElementById('prange-tbody').addEventListener('click', function (e) {
            var btn = e.target.closest('.btn-delete-prange');
            if (!btn) return;

            if (!confirm(i18n.confirm.deleteRange)) return;

            var id = btn.dataset.id;

            fetch(apiUrl('/api/parameter-ranges/' + id), {method: 'DELETE'})
                .then(function (res) {
                    if (!res.ok) throw new Error(i18n.error.deleteRange + ': ' + res.status);
                    loadPrangeTable();
                })
                .catch(function (err) {
                    alert(i18n.error.deleteRange + ': ' + err.message);
                });
        });

        // Open in create mode
        document.getElementById('btn-nuevo-prange').addEventListener('click', function () {
            editingId = null;
            document.getElementById('prange-edit-label').innerText = i18n.label.newRange;
            ['id', 'parameter', 'level1', 'level2', 'level3', 'level4', 'level5', 'unit'].forEach(function (f) {
                document.getElementById('prange-' + f).value = '';
            });
            document.getElementById('prange-feedback').className = 'alert d-none';
            prangeEditModal.show();
        });

        // Save (PUT or POST depending on editingId)
        document.getElementById('btn-guardar-prange').addEventListener('click', function () {
            var fb = document.getElementById('prange-feedback');
            var param = document.getElementById('prange-parameter').value.trim();

            if (!param) {
                fb.className = 'alert alert-warning';
                fb.innerText = i18n.validation.parameterNameRequired;
                return;
            }

            var payload = {
                parameter: param,
                level1: document.getElementById('prange-level1').value || null,
                level2: document.getElementById('prange-level2').value || null,
                level3: document.getElementById('prange-level3').value || null,
                level4: document.getElementById('prange-level4').value || null,
                level5: document.getElementById('prange-level5').value || null,
                unit: document.getElementById('prange-unit').value || null
            };

            var isEdit = editingId !== null;
            var url = isEdit ? '/api/parameter-ranges/' + editingId : '/api/parameter-ranges';
            var method = isEdit ? 'PUT' : 'POST';

            var btnTxt = document.getElementById('btn-prange-txt');
            var btnSpinner = document.getElementById('btn-prange-spinner');
            btnTxt.innerText = i18n.button.saving;
            btnSpinner.classList.remove('d-none');
            document.getElementById('btn-guardar-prange').disabled = true;

            fetch(apiUrl(url), {
                method: method,
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            })
                .then(function (res) {
                    if (!res.ok) throw new Error(i18n.error.loadRanges + ': ' + res.status);
                    fb.className = 'alert alert-success';
                    fb.innerText = isEdit ? i18n.success.rangeUpdated : i18n.success.rangeCreated;
                    loadPrangeTable();
                    setTimeout(function () {
                        prangeEditModal.hide();
                    }, 1000);
                })
                .catch(function (err) {
                    fb.className = 'alert alert-danger';
                    fb.innerText = i18n.error.saveRange + ': ' + err.message;
                })
                .finally(function () {
                    btnTxt.innerText = i18n.button.save;
                    btnSpinner.classList.add('d-none');
                    document.getElementById('btn-guardar-prange').disabled = false;
                });
        });

}
