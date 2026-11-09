export function initializeNotificationManagement(apiUrl, i18n) {
        var notifEditModal = new bootstrap.Modal(document.getElementById('notifEditModal'));
        var editingNotifId = null;

        document.getElementById('notificationsModal').addEventListener('show.bs.modal', function () {
            loadNotifTable();
        });

        function loadNotifTable() {
            var spinner = document.getElementById('notif-spinner');
            var tableWrap = document.getElementById('notif-table-wrap');
            var errEl = document.getElementById('notif-error');
            var tbody = document.getElementById('notif-tbody');

            spinner.classList.remove('d-none');
            tableWrap.classList.add('d-none');
            errEl.classList.add('d-none');

            fetch(apiUrl('/api/notifications'))
                .then(function (res) {
                    if (!res.ok) throw new Error('Error ' + res.status);
                    return res.json();
                })
                .then(function (data) {
                    tbody.innerHTML = data.map(function (n) {
                        var fechaStr = n.creationDate
                            ? new Date(n.creationDate).toLocaleDateString(window.appLocale)
                            : '-';
                        return '<tr>' +
                            '<td><strong>' + (n.parameter ?? '-') + '</strong></td>' +
                            '<td>' + (n.value ?? '-') + ' ' + window.i18n.label.days + '</td>' +
                            '<td>' +
                            (n.active
                                ? '<span class="badge bg-success">' + window.i18n.label.yes + '</span>'
                                : '<span class="badge bg-secondary">No</span>') +
                            '</td>' +
                            '<td class="text-muted small">' + fechaStr + '</td>' +
                            '<td class="d-flex gap-1">' +
                            '<button class="btn btn-sm btn-outline-primary btn-edit-notif" ' +
                            'data-id="' + n.id + '" ' +
                            'data-parameter="' + (n.parameter ?? '') + '" ' +
                            'data-value="' + (n.value ?? '') + '" ' +
                            'data-active="' + (n.active ?? true) + '" ' +
                            'data-weekdays="' + (n.weekdays ?? 0) + '">' +
                            '<i class="bi bi-pencil"></i>' +
                            '</button>' +
                            '<button class="btn btn-sm btn-outline-danger btn-delete-notif" data-id="' + n.id + '">' +
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
                    errEl.innerText = i18n.error.load + ' ' + err.message;
                    errEl.classList.remove('d-none');
                });
        }

        // Edit
        document.getElementById('notif-tbody').addEventListener('click', function (e) {
            var btn = e.target.closest('.btn-edit-notif');
            if (!btn) return;

            editingNotifId = btn.dataset.id;
            document.getElementById('notif-edit-label').innerText = i18n.label.editNotification;
            document.getElementById('notif-id').value = editingNotifId;
            document.getElementById('notif-parameter').value = btn.dataset.parameter;
            document.getElementById('notif-value').value = btn.dataset.value;
            document.getElementById('notif-active').checked = btn.dataset.active === 'true';
            document.getElementById('notif-feedback').className = 'alert d-none';

            document.querySelectorAll('.notif-weekday').forEach(function (cb) {
                cb.checked = false;
            });
            var weekdaysMask = parseInt(btn.dataset.weekdays, 10) || 0;
            document.querySelectorAll('.notif-weekday').forEach(function (cb) {
                var bitValue = parseInt(cb.dataset.bit, 10);
                cb.checked = (weekdaysMask & bitValue) !== 0;
            });

            notifEditModal.show();
        });

        // Delete
        document.getElementById('notif-tbody').addEventListener('click', function (e) {
            var btn = e.target.closest('.btn-delete-notif');
            if (!btn) return;

            if (!confirm(i18n.confirm.deleteNotification)) return;

            fetch(apiUrl('/api/notifications/' + btn.dataset.id), {method: 'DELETE'})
                .then(function (res) {
                    if (!res.ok) throw new Error(i18n.error.deleteNotification + ' ' + res.status);
                    loadNotifTable();
                })
                .catch(function (err) {
                    alert(i18n.error.deleteNotification + ': ' + err.message);
                });
        });

        // New
        document.getElementById('btn-nuevo-notif').addEventListener('click', function () {
            editingNotifId = null;
            document.getElementById('notif-edit-label').innerText = i18n.label.newNotification;
            document.getElementById('notif-id').value = '';
            document.getElementById('notif-parameter').value = '';
            document.getElementById('notif-value').value = '';
            document.getElementById('notif-active').checked = true;
            document.getElementById('notif-feedback').className = 'alert d-none';
            document.querySelectorAll('.notif-weekday').forEach(function (cb) {
                cb.checked = false;
            });
            notifEditModal.show();
        });

        // Save
        document.getElementById('btn-guardar-notif').addEventListener('click', function () {
            var fb = document.getElementById('notif-feedback');
            var param = document.getElementById('notif-parameter').value.trim();
            var value = document.getElementById('notif-value').value;

            if (!param || !value) {
                fb.className = 'alert alert-warning';
                fb.innerText = i18n.validation.parameterDaysRequired;
                return;
            }

            var weekdaysMask = 0;
            document.querySelectorAll('.notif-weekday:checked').forEach(function (cb) {
                weekdaysMask |= parseInt(cb.dataset.bit, 10);
            });

            var payload = {
                parameter: param,
                value: parseInt(value),
                active: document.getElementById('notif-active').checked,
                weekdays: weekdaysMask
            };

            var isEdit = editingNotifId !== null;
            var url = isEdit ? '/api/notifications/' + editingNotifId : '/api/notifications';
            var method = isEdit ? 'PUT' : 'POST';

            var btnTxt = document.getElementById('btn-notif-txt');
            var btnSpinner = document.getElementById('btn-notif-spinner');
            btnTxt.innerText = i18n.button.saving;
            btnSpinner.classList.remove('d-none');
            document.getElementById('btn-guardar-notif').disabled = true;

            fetch(apiUrl(url), {
                method: method,
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            })
                .then(function (res) {
                    if (!res.ok) throw new Error(i18n.error.saveNotification + ' ' + res.status);
                    fb.className = 'alert alert-success';
                    fb.innerText = isEdit ? i18n.success.notificationUpdated : i18n.success.notificationCreated;
                    loadNotifTable();
                    setTimeout(function () {
                        notifEditModal.hide();
                    }, 1000);
                })
                .catch(function (err) {
                    fb.className = 'alert alert-danger';
                    fb.innerText = i18n.error.saveNotification + ': ' + err.message;
                })
                .finally(function () {
                    btnTxt.innerText = i18n.button.save;
                    btnSpinner.classList.add('d-none');
                    document.getElementById('btn-guardar-notif').disabled = false;
                });
        });

}
