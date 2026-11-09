import {initializeNotificationManagement} from './js/notifications.js';
import {initializeParameterRangeManagement} from './js/parameter-ranges.js';
import {createLastPhotosLoader} from './js/last-photos.js';
import {initializeDashboardData} from './js/dashboard-data.js';
import {initializeHistoryExport} from './js/history-export.js';
import {initializeLifeManagement} from './js/life.js';
import {escapeHtml} from './js/utils.js';

document.addEventListener('DOMContentLoaded', function () {
    var aquariumId = new URLSearchParams(window.location.search).get('aquariumId')
        || window.aquariumId;
    if (!aquariumId) {
        console.error('Missing aquariumId in management URL');
        return;
    }

    function apiUrl(url) {
        return '/api/aquariums/' + encodeURIComponent(aquariumId) + url.substring(4);
    }

    var i18n = window.i18n;
    var openLifeRegistration = initializeLifeManagement(apiUrl, i18n.life);

    // ── FullCalendar ──────────────────────────────────────────────
    var calendarEl = document.getElementById('calendar');
    if (!calendarEl) {
        console.error("Calendar element #calendar was not found");
        return;
    }

    var dashboardData = initializeDashboardData(apiUrl, i18n);
    var loadDashboardData = dashboardData.loadDashboardData;
    var loadNotifications = dashboardData.loadNotifications;
    var loadLastPhotos = createLastPhotosLoader(apiUrl, i18n);
    initializeNotificationManagement(apiUrl, i18n);
    initializeParameterRangeManagement(apiUrl, i18n);

    var calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        locale: window.appLocale,

        eventSources: [
            {
                url: apiUrl('/api/events'),
                method: 'GET',
                failure: function () {
                    alert(i18n.error.loadEvents);
                }
            },
            {
                events: function (fetchInfo, successCallback, failureCallback) {
                    var start = fetchInfo.startStr.substring(0, 10);
                    var end = fetchInfo.endStr.substring(0, 10);
                    fetch('/api/aquariums/' + encodeURIComponent(aquariumId)
                        + '/tasks/calendar?start=' + encodeURIComponent(start)
                        + '&end=' + encodeURIComponent(end))
                        .then(function (response) {
                            if (!response.ok) throw new Error('Error ' + response.status);
                            return response.json();
                        })
                        .then(successCallback)
                        .catch(failureCallback);
                }
            }
        ],

        eventContent: function (arg) {
            if (arg.event.extendedProps.type === 'reminder') {
                var clock = document.createElement('i');
                clock.className = 'bi bi-clock-fill text-warning fs-6';
                clock.setAttribute('aria-label', i18n.task.reminder);
                clock.title = arg.event.extendedProps.description;
                return {domNodes: [clock]};
            }
            return {
                html: '<span style="font-size:1.6rem; cursor:pointer; display:inline-block;">'
                    + arg.event.title + '</span>'
            };
        },

        eventClick: function (info) {
            var event = info.event;
            var props = event.extendedProps;

            if (props.type === 'reminder') {
                openReminderDetails(Math.abs(Number(event.id)));
                return;
            }

            // ── Parameters ────────────────────────────────────────────
            if (props.type === 'parameters') {
                var paramModal = new bootstrap.Modal(document.getElementById('parametrosViewModal'));

                document.getElementById('param-view-fecha').innerText =
                    event.start ? event.start.toLocaleDateString(window.appLocale) : '';
                document.getElementById('param-view-spinner').classList.remove('d-none');
                document.getElementById('param-view-content').classList.add('d-none');
                document.getElementById('param-view-error').classList.add('d-none');
                document.getElementById('btn-borrar-parametro').dataset.id = event.id;
                ['nh3', 'no2', 'no3', 'ph', 'kh', 'gh'].forEach(function (f) {
                    document.getElementById('pv-' + f).innerText = '-';
                });

                paramModal.show();

                fetch(apiUrl('/api/parameters/' + event.id))
                    .then(function (res) {
                        if (!res.ok) throw new Error('Error ' + res.status);
                        return res.json();
                    })
                    .then(function (data) {
                        document.getElementById('pv-nh3').innerText = data.nh3 ?? '-';
                        document.getElementById('pv-no2').innerText = data.no2 ?? '-';
                        document.getElementById('pv-no3').innerText = data.no3 ?? '-';
                        document.getElementById('pv-ph').innerText = data.ph ?? '-';
                        document.getElementById('pv-kh').innerText = data.kh ?? '-';
                        document.getElementById('pv-gh').innerText = data.gh ?? '-';
                        document.getElementById('param-view-spinner').classList.add('d-none');
                        document.getElementById('param-view-content').classList.remove('d-none');
                    })
                    .catch(function (err) {
                        document.getElementById('param-view-spinner').classList.add('d-none');
                        var errEl = document.getElementById('param-view-error');
                        errEl.innerText = i18n.error.loadParameters + ': ' + err.message;
                        errEl.classList.remove('d-none');
                    });

                return;
            }

            // ── Co2 ───────────────────────────────────────────────────
            if (props.type === 'co2') {
                var items = props.items || [];

                document.getElementById('modal-icon').innerHTML = event.title;
                document.getElementById('modal-type').innerText = i18n.label.co2Treatments;
                document.getElementById('modal-single').style.display = 'none';
                document.getElementById('modal-items').style.display = 'block';

                document.getElementById('modal-items-list').innerHTML = items.length === 0
                    ? '<li class="list-group-item text-muted text-center">' + i18n.label.noData + '</li>'
                    : items.map(function (item) {
                        return `<li class="list-group-item d-flex justify-content-between align-items-center">
                                <span class="fw-semibold">${item.co2Product ?? '-'}</span>
                                <div class="d-flex align-items-center gap-2">
                                    <span class="badge bg-info text-dark">
                                        <i class="bi bi-droplet"></i> ${item.co2Value ?? '-'} ml
                                    </span>
                                    <button class="btn btn-danger btn-sm btn-borrar-co2" data-id="${item.id}">
                                        <i class="bi bi-trash"></i>
                                    </button>
                                </div>
                            </li>`;
                    }).join('');

                new bootstrap.Modal(document.getElementById('eventoModal')).show();
                return;
            }

            // ── Photo ─────────────────────────────────────────────────────
            if (props.type === 'photo') {
                var photoModal = new bootstrap.Modal(document.getElementById('photoViewModal'));

                var fechaStr = event.start ?
                    event.start.getFullYear() + '-' +
                    String(event.start.getMonth() + 1).padStart(2, '0') + '-' +
                    String(event.start.getDate()).padStart(2, '0') : null;

                document.getElementById('photo-view-fecha').innerText =
                    event.start ? event.start.toLocaleDateString(window.appLocale) : '';
                document.getElementById('photo-view-spinner').classList.remove('d-none');
                document.getElementById('photo-carousel-wrap').classList.add('d-none');
                document.getElementById('photo-view-empty').classList.add('d-none');
                document.getElementById('photo-view-error').classList.add('d-none');
                document.getElementById('photo-carousel-inner').innerHTML = '';
                document.getElementById('btn-borrar-foto').dataset.id = '';

                photoModal.show();

                fetch(apiUrl('/api/photos/by-day/' + fechaStr))
                    .then(function (res) {
                        if (!res.ok) throw new Error('Error ' + res.status);
                        return res.json();
                    })
                    .then(function (data) {
                        document.getElementById('photo-view-spinner').classList.add('d-none');

                        if (!data || data.length === 0) {
                            document.getElementById('photo-view-empty').classList.remove('d-none');
                            return;
                        }

                        var inner = document.getElementById('photo-carousel-inner');
                        inner.innerHTML = data.map(function (item, idx) {
                            return `<div class="carousel-item ${idx === 0 ? 'active' : ''}" data-photo-id="${item.id}" data-event-id="${item.eventId}">
                        <img src="${apiUrl('/api/photos/' + item.id + '/image')}"
                             class="d-block mx-auto img-fluid rounded"
                             style="max-height:400px; object-fit:contain;"
                             alt="Foto ${idx + 1}">
                    </div>`;
                        }).join('');

                        // Delete button targets the active photo
                        var carousel = document.getElementById('photoCarousel');

                        function updateDeleteButtonState() {
                            var active = carousel.querySelector('.carousel-item.active');
                            if (active) {
                                document.getElementById('btn-borrar-foto').dataset.id = active.dataset.photoId;
                                document.getElementById('btn-borrar-foto').dataset.eventId = active.dataset.eventId;
                                document.getElementById('photo-counter').innerText =
                                    (Array.from(carousel.querySelectorAll('.carousel-item')).indexOf(active) + 1) +
                                    ' / ' + data.length;
                            }
                        }

                        // Calculate active index
                        carousel.addEventListener('slid.bs.carousel', updateDeleteButtonState);
                        updateDeleteButtonState();

                        document.getElementById('photo-carousel-wrap').classList.remove('d-none');
                        document.getElementById('photo-counter').innerText = '1 / ' + data.length;
                    })
                    .catch(function (err) {
                        document.getElementById('photo-view-spinner').classList.add('d-none');
                        var errEl = document.getElementById('photo-view-error');
                        errEl.innerText = i18n.error.load + ': ' + err.message;
                        errEl.classList.remove('d-none');
                    });

                return;
            }

            // ── Maintenance ─────────────────────────────────────────────
            if (props.type === 'manteiment') {
                var mantModal = new bootstrap.Modal(document.getElementById('maintenanceViewModal'));

                document.getElementById('mant-view-fecha').innerText =
                    event.start ? event.start.toLocaleDateString(window.appLocale) : '';
                document.getElementById('mant-view-spinner').classList.remove('d-none');
                document.getElementById('mant-view-content').classList.add('d-none');
                document.getElementById('mant-view-error').classList.add('d-none');
                document.getElementById('btn-delete-maintenance').dataset.id = event.id;

                mantModal.show();

                fetch(apiUrl('/api/maintenances/' + event.id))
                    .then(function (res) {
                        if (!res.ok) throw new Error('Error ' + res.status);
                        return res.json();
                    })
                    .then(function (data) {
                        document.getElementById('mant-view-water').innerText = data.water ? data.water + ' L' : '-';
                        document.getElementById('mant-view-prime').innerText = data.primeDose ? data.primeDose + ' ml' : '-';
                        document.getElementById('mant-view-glass').innerText = data.glass ? i18n.label.yes : i18n.label.no;
                        document.getElementById('mant-view-pruning').innerText = data.pruning || '-';
                        document.getElementById('mant-view-observations').innerText = data.observations || '-';

                        // ── Filters (perlon, sponge, biological, mechanical, biomechanical) ──
                        var filters = data.filters || [];
                        var filtro1 = filters[0] || null;
                        var filtro2 = filters[1] || null;
                        document.getElementById('mant-view-filter1').innerText = describeFilter(filtro1);
                        document.getElementById('mant-view-filter2').innerText = describeFilter(filtro2);

                        document.getElementById('mant-view-spinner').classList.add('d-none');
                        document.getElementById('mant-view-content').classList.remove('d-none');
                    })
                    .catch(function (err) {
                        document.getElementById('mant-view-spinner').classList.add('d-none');
                        var errEl = document.getElementById('mant-view-error');
                        errEl.innerText = i18n.error.load + ': ' + err.message;
                        errEl.classList.remove('d-none');
                    });

                return;
            }

            // ── Info ──────────────────────────────────────────────────────
            if (props.type === 'info' || props.type === 'live') {
                var infoModal = new bootstrap.Modal(document.getElementById('infoViewModal'));

                var fechaStr = event.start ?
                    event.start.getFullYear() + '-' +
                    String(event.start.getMonth() + 1).padStart(2, '0') + '-' +
                    String(event.start.getDate()).padStart(2, '0') : null;

                document.getElementById('info-view-fecha').innerText =
                    event.start ? event.start.toLocaleDateString(window.appLocale) : '';
                document.getElementById('info-view-spinner').classList.remove('d-none');
                document.getElementById('info-view-content').classList.add('d-none');
                document.getElementById('info-view-error').classList.add('d-none');

                // Ocultar todos los grupos
                ['info', 'fish', 'plants'].forEach(function (t) {
                    document.getElementById('info-group-' + t).classList.add('d-none');
                    document.getElementById('info-list-' + t).innerHTML = '';
                });

                infoModal.show();

                fetch(apiUrl('/api/events/information/by-day/' + fechaStr))
                    .then(function (res) {
                        if (!res.ok) throw new Error(i18n.error.load + ': ' + res.status);
                        return res.json();
                    })
                    .then(function (data) {
                        var tipos = {'info': [], 'fish': [], 'plants': []};

                        data.forEach(function (item) {
                            var subtype = (item.type || 'info').toLowerCase();
                            var t = subtype === 'fish' || subtype === 'plants' ? subtype : 'info';
                            tipos[t].push(item);
                        });

                        Object.keys(tipos).forEach(function (t) {
                            if (tipos[t].length === 0) return;

                            var group = document.getElementById('info-group-' + t);
                            var list = document.getElementById('info-list-' + t);
                            group.classList.remove('d-none');

                            list.innerHTML = tipos[t].map(function (item) {
                                var status = item.status === 'TASK_COMPLETED'
                                    ? i18n.task.completedStatus
                                    : item.status;
                                return `<li class="list-group-item">
                            <div class="d-flex justify-content-between align-items-start">
                                <div>
                                    ${status ? '<span class="badge bg-secondary me-2">' + escapeHtml(status) + '</span>' : ''}
                                    <span class="small">${escapeHtml(item.observation ?? '-')}</span>
                                </div>
                                <button class="btn btn-danger btn-sm btn-borrar-info ms-2" data-id="${item.idEvent}">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </div>
                        </li>`;
                            }).join('');
                        });

                        document.getElementById('info-view-spinner').classList.add('d-none');
                        document.getElementById('info-view-content').classList.remove('d-none');
                    })
                    .catch(function (err) {
                        document.getElementById('info-view-spinner').classList.add('d-none');
                        var errEl = document.getElementById('info-view-error');
                        errEl.innerText = i18n.error.load + ': ' + err.message;
                        errEl.classList.remove('d-none');
                    });

                return;
            }

            // ── Resto de tipos ────────────────────────────────────────
            document.getElementById('modal-icon').innerHTML = event.title;
            document.getElementById('modal-type').innerText = props.type ?? '-';
            document.getElementById('modal-single').style.display = 'block';
            document.getElementById('modal-items').style.display = 'none';
            document.getElementById('modal-start').innerText = event.start
                ? event.start.toLocaleDateString(window.appLocale) : '-';
            document.getElementById('modal-end').innerText = event.end
                ? event.end.toLocaleDateString(window.appLocale) : '-';
            document.getElementById('modal-co2Tratamiento-row').style.setProperty('display', 'none', 'important');
            document.getElementById('modal-co2Tratamiento-value-row').style.setProperty('display', 'none', 'important');
            document.getElementById('btn-borrar-evento-modal').dataset.id = event.id;

            new bootstrap.Modal(document.getElementById('eventoModal')).show();
        }
    });

    calendar.render();
    loadLastPhotos();
    var updates = new EventSource('/api/updates');
    updates.addEventListener('data-updated', function (event) {
        var type = JSON.parse(event.data).type;
        var reloads = [];

        if (type === 'event' || type === 'task' || type === 'parameters' || type === 'maintenance'
            || type === 'treatment' || type === 'photo') {
            calendar.refetchEvents();
        }
        if (type === 'event' || type === 'parameters' || type === 'maintenance' || type === 'treatment'
            || type === 'notification' || type === 'parameter-range') {
            reloads.push(loadUpcomingDashboard());
        }
        if (type === 'parameters' || type === 'parameter-range') {
            reloads.push(loadLatestParameters(), loadDashboardData());
        }
        if (type === 'maintenance') {
            reloads.push(loadLatestMaintenance());
        }
        if (type === 'photo') {
            reloads.push(loadLastPhotos());
        }
        if (type === 'task') {
            reloads.push(loadUpcomingDashboard());
        }
        if (type === 'notification' || type === 'parameter-range') {
            reloads.push(loadNotifications());
        }

        Promise.all(reloads).catch(function (err) {
            console.error('Error refreshing dashboard after update', err);
        });
    });
    updates.onerror = function () {
        console.warn('The live update stream was interrupted; the browser will retry automatically.');
    };

    var taskForm = document.getElementById('task-create-form');
    var taskList = document.getElementById('aquarium-task-list');
    var taskModal = bootstrap.Modal.getOrCreateInstance(document.getElementById('task-create-modal'));
    var taskModalFeedback = document.getElementById('task-modal-feedback');
    var taskDateFromCalendar = null;
    var taskDetailsModalEl = document.getElementById('task-details-modal');
    var taskDetailsModal = bootstrap.Modal.getOrCreateInstance(taskDetailsModalEl);
    var taskDetailsFeedback = document.getElementById('task-details-feedback');
    var taskDetailsView = document.getElementById('task-details-view');
    var taskEditForm = document.getElementById('task-edit-form');
    var taskCompletionForm = document.getElementById('task-completion-form');
    var upcomingModal = document.getElementById('upcomingEventsModal');
    var upcomingFullList = document.getElementById('upcoming-events-full-list');
    var upcomingEventsData = [];
    var aquariumTasksData = [];
    var currentReminderId = null;
    var currentReminder = null;

    function taskUrl(path) {
        return '/api/aquariums/' + encodeURIComponent(aquariumId) + '/tasks' + path;
    }

    function toIsoTaskDate(value) {
        var match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
        if (!match) return null;
        var year = Number(match[1]);
        var month = Number(match[2]);
        var day = Number(match[3]);
        var date = new Date(0);
        date.setUTCHours(0, 0, 0, 0);
        date.setUTCFullYear(year, month - 1, day);
        if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1
            || date.getUTCDate() !== day) return null;
        return value;
    }

    function formatTaskDate(isoDate) {
        return formatLocalizedDate(isoDate);
    }

    function currentTaskDate() {
        var date = new Date();
        return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'),
            String(date.getDate()).padStart(2, '0')].join('-');
    }

    function validateTaskDate(input) {
        input.setCustomValidity(input.value && !toIsoTaskDate(input.value)
            ? i18n.task.dateInvalid : '');
    }

    ['task-action-date', 'task-edit-date', 'task-completion-date'].forEach(function (id) {
        var dateInput = document.getElementById(id);
        dateInput.addEventListener('input', function () {
            validateTaskDate(dateInput);
        });
    });

    function showReminderError(error) {
        taskDetailsFeedback.className = 'alert alert-danger mt-3 mb-0';
        taskDetailsFeedback.textContent = error.message;
    }

    function completeReminder(taskId, formData) {
        return fetch(taskUrl('/' + encodeURIComponent(taskId) + '/complete'), {
            method: 'POST',
            body: formData
        })
            .then(function (response) {
                if (!response.ok) throw new Error(i18n.task.completeError + ' (' + response.status + ')');
                taskDetailsModal.hide();
                calendar.refetchEvents();
                return loadUpcomingDashboard();
            });
    }

    function openReminderDetails(taskId) {
        currentReminderId = taskId;
        currentReminder = null;
        taskDetailsFeedback.className = 'alert d-none mt-3 mb-0';
        taskDetailsFeedback.textContent = '';
        taskDetailsView.classList.add('d-none');
        taskEditForm.classList.add('d-none');
        taskCompletionForm.classList.add('d-none');
        document.getElementById('task-details-actions').classList.add('d-none');
        document.getElementById('task-details-actions').classList.remove('d-flex');
        document.getElementById('task-edit-actions').classList.add('d-none');
        document.getElementById('task-edit-actions').classList.remove('d-flex');
        document.getElementById('task-completion-actions').classList.add('d-none');
        document.getElementById('task-completion-actions').classList.remove('d-flex');
        taskDetailsModal.show();

        fetch(taskUrl('/' + encodeURIComponent(taskId)))
            .then(function (response) {
                if (!response.ok) throw new Error(i18n.task.detailError + ' (' + response.status + ')');
                return response.json();
            })
            .then(function (task) {
                currentReminder = task;
                document.getElementById('task-details-title').textContent = task.title;
                document.getElementById('task-details-date').textContent = formatTaskDate(task.actionDate);
                document.getElementById('task-details-event-type').textContent =
                    i18n.task.eventTypes[task.eventType] || task.eventType;
                document.getElementById('task-details-description').textContent = task.description || '';
                document.getElementById('task-edit-title').value = task.title;
                document.getElementById('task-edit-date').value = task.actionDate;
                document.getElementById('task-edit-date').setCustomValidity('');
                document.getElementById('task-edit-event-type').value = task.eventType;
                document.getElementById('task-edit-description').value = task.description || '';
                taskDetailsView.classList.remove('d-none');
                document.getElementById('task-details-actions').classList.remove('d-none');
                document.getElementById('task-details-actions').classList.add('d-flex');
            })
            .catch(showReminderError);
    }

    taskForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var actionDateInput = document.getElementById('task-action-date');
        validateTaskDate(actionDateInput);
        if (!taskForm.reportValidity()) return;
        var actionDate = toIsoTaskDate(actionDateInput.value);

        taskModalFeedback.className = 'alert d-none mt-3 mb-0';
        taskModalFeedback.textContent = '';
        var submitButton = taskForm.querySelector('[type="submit"]');
        submitButton.disabled = true;
        var payload = {
            title: document.getElementById('task-title').value.trim(),
            description: document.getElementById('task-description').value.trim() || null,
            actionDate: actionDate,
            eventType: document.getElementById('task-event-type').value
        };

        fetch('/api/aquariums/' + encodeURIComponent(aquariumId) + '/tasks', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        })
            .then(function (response) {
                if (!response.ok) throw new Error(i18n.task.saveError + ' (' + response.status + ')');
                taskForm.reset();
                taskModal.hide();
                calendar.refetchEvents();
                return loadUpcomingDashboard();
            })
            .catch(function (error) {
                taskModalFeedback.className = 'alert alert-danger mt-3 mb-0';
                taskModalFeedback.textContent = error.message;
            })
            .finally(function () {
                submitButton.disabled = false;
            });
    });

    document.getElementById('task-create-modal').addEventListener('show.bs.modal', function () {
        taskForm.reset();
        var actionDateInput = document.getElementById('task-action-date');
        actionDateInput.setCustomValidity('');
        if (taskDateFromCalendar) {
            actionDateInput.value = taskDateFromCalendar;
            taskDateFromCalendar = null;
        }
        taskModalFeedback.className = 'alert d-none mt-3 mb-0';
        taskModalFeedback.textContent = '';
    });

    taskList.addEventListener('click', function (event) {
        var button = event.target.closest('.task-complete-button');
        if (!button) return;
        openReminderDetails(button.dataset.taskId);
    });
    document.getElementById('proximos-eventos-list').addEventListener('click', function (event) {
        var button = event.target.closest('.task-complete-button');
        if (!button) return;
        openReminderDetails(button.dataset.taskId);
    });

    document.getElementById('task-details-complete').addEventListener('click', function () {
        if (!currentReminder) return;
        taskDetailsView.classList.add('d-none');
        taskEditForm.classList.add('d-none');
        taskCompletionForm.classList.remove('d-none');
        document.getElementById('task-details-actions').classList.add('d-none');
        document.getElementById('task-details-actions').classList.remove('d-flex');
        document.getElementById('task-edit-actions').classList.add('d-none');
        document.getElementById('task-edit-actions').classList.remove('d-flex');
        document.getElementById('task-completion-actions').classList.remove('d-none');
        document.getElementById('task-completion-actions').classList.add('d-flex');
        taskCompletionForm.reset();
        document.getElementById('task-completion-date').value = currentTaskDate();
        document.getElementById('task-completion-observation').value =
            currentReminder.description || currentReminder.title;
        document.getElementById('task-completion-maintenance-observations').value =
            currentReminder.description || currentReminder.title;
        taskCompletionForm.querySelectorAll('.task-completion-section').forEach(function (section) {
            var supportedTypes = section.dataset.eventType.split(' ');
            section.classList.toggle('d-none', !supportedTypes.includes(currentReminder.eventType));
        });
        var infoType = document.getElementById('task-completion-info-type');
        Array.from(infoType.options).forEach(function (option) {
            option.hidden = currentReminder.eventType === 'info' && option.value !== 'INFO'
                || currentReminder.eventType === 'live' && option.value === 'INFO';
        });
        infoType.value = currentReminder.eventType === 'info' ? 'INFO' : 'FISH';
    });

    document.getElementById('task-completion-cancel').addEventListener('click', function () {
        taskCompletionForm.classList.add('d-none');
        taskDetailsView.classList.remove('d-none');
        document.getElementById('task-completion-actions').classList.add('d-none');
        document.getElementById('task-completion-actions').classList.remove('d-flex');
        document.getElementById('task-details-actions').classList.remove('d-none');
        document.getElementById('task-details-actions').classList.add('d-flex');
    });

    function completionNumber(id) {
        var value = document.getElementById(id).value;
        return value === '' ? null : Number(value);
    }

    taskCompletionForm.addEventListener('submit', function (event) {
        event.preventDefault();
        if (!currentReminderId || !currentReminder) return;
        var dateInput = document.getElementById('task-completion-date');
        validateTaskDate(dateInput);
        var product = document.getElementById('task-completion-product');
        var value = document.getElementById('task-completion-value');
        var photoInput = document.getElementById('task-completion-photo');
        var parameterIds = ['nh3', 'no2', 'no3', 'ph', 'kh', 'gh', 'temp'];
        var hasParameter = parameterIds.some(function (name) {
            return document.getElementById('task-completion-' + name).value !== '';
        });
        product.setCustomValidity(currentReminder.eventType === 'co2' && !product.value.trim()
            ? i18n.task.requiredField : '');
        value.setCustomValidity(currentReminder.eventType === 'co2' && !value.value.trim()
            ? i18n.task.requiredField : '');
        photoInput.setCustomValidity(currentReminder.eventType === 'photo' && !photoInput.files.length
            ? i18n.task.requiredField : '');
        var parameterInput = document.getElementById('task-completion-nh3');
        parameterInput.setCustomValidity(currentReminder.eventType === 'parameters' && !hasParameter
            ? i18n.task.parameterRequired : '');
        if (!taskCompletionForm.reportValidity()) return;

        var completion = {
            date: toIsoTaskDate(dateInput.value),
            infoType: document.getElementById('task-completion-info-type').value,
            status: document.getElementById('task-completion-status').value.trim() || null,
            observation: document.getElementById('task-completion-observation').value.trim() || null,
            product: product.value.trim() || null,
            value: value.value.trim() || null,
            water: completionNumber('task-completion-water'),
            primeDose: completionNumber('task-completion-prime'),
            glass: document.getElementById('task-completion-glass').checked,
            pruning: document.getElementById('task-completion-pruning').value.trim() || null,
            maintenanceObservations:
                document.getElementById('task-completion-maintenance-observations').value.trim() || null,
            f1Sponge: document.getElementById('task-completion-f1-sponge').checked,
            f1Perlon: document.getElementById('task-completion-f1-perlon').checked,
            f1Mech: document.getElementById('task-completion-f1-mech').checked,
            f1Bio: document.getElementById('task-completion-f1-bio').checked,
            f1BioMech: document.getElementById('task-completion-f1-bio-mech').checked,
            f2Sponge: document.getElementById('task-completion-f2-sponge').checked,
            f2Perlon: document.getElementById('task-completion-f2-perlon').checked,
            f2Mech: document.getElementById('task-completion-f2-mech').checked,
            f2Bio: document.getElementById('task-completion-f2-bio').checked,
            f2BioMech: document.getElementById('task-completion-f2-bio-mech').checked
        };
        parameterIds.forEach(function (name) {
            completion[name] = completionNumber('task-completion-' + name);
        });
        var formData = new FormData();
        formData.append('completion', new Blob([JSON.stringify(completion)], {type: 'application/json'}));
        if (photoInput.files.length) formData.append('photo', photoInput.files[0]);

        var submitButton = document.getElementById('task-completion-submit');
        submitButton.disabled = true;
        completeReminder(currentReminderId, formData)
            .catch(showReminderError)
            .finally(function () { submitButton.disabled = false; });
    });

    document.getElementById('task-details-delete').addEventListener('click', function () {
        if (!currentReminderId || !confirm(i18n.task.confirmDelete)) return;
        this.disabled = true;
        fetch(taskUrl('/' + encodeURIComponent(currentReminderId)), {method: 'DELETE'})
            .then(function (response) {
                if (!response.ok) throw new Error(i18n.task.deleteError + ' (' + response.status + ')');
                taskDetailsModal.hide();
                calendar.refetchEvents();
                return loadUpcomingDashboard();
            })
            .catch(showReminderError)
            .finally(() => { this.disabled = false; });
    });

    document.getElementById('task-details-edit').addEventListener('click', function () {
        if (!currentReminder) return;
        taskDetailsView.classList.add('d-none');
        taskEditForm.classList.remove('d-none');
        document.getElementById('task-details-actions').classList.add('d-none');
        document.getElementById('task-details-actions').classList.remove('d-flex');
        document.getElementById('task-edit-actions').classList.remove('d-none');
        document.getElementById('task-edit-actions').classList.add('d-flex');
    });

    document.getElementById('task-edit-cancel').addEventListener('click', function () {
        taskEditForm.classList.add('d-none');
        taskDetailsView.classList.remove('d-none');
        document.getElementById('task-details-actions').classList.remove('d-none');
        document.getElementById('task-details-actions').classList.add('d-flex');
        document.getElementById('task-edit-actions').classList.add('d-none');
        document.getElementById('task-edit-actions').classList.remove('d-flex');
    });

    taskEditForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var actionDateInput = document.getElementById('task-edit-date');
        validateTaskDate(actionDateInput);
        if (!taskEditForm.reportValidity() || !currentReminderId) return;
        var actionDate = toIsoTaskDate(actionDateInput.value);
        var submitButton = document.getElementById('task-edit-submit');
        submitButton.disabled = true;
        fetch(taskUrl('/' + encodeURIComponent(currentReminderId)), {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                title: document.getElementById('task-edit-title').value.trim(),
                actionDate: actionDate,
                description: document.getElementById('task-edit-description').value.trim() || null,
                eventType: document.getElementById('task-edit-event-type').value
            })
        })
            .then(function (response) {
                if (!response.ok) throw new Error(i18n.task.updateError + ' (' + response.status + ')');
                taskDetailsModal.hide();
                calendar.refetchEvents();
                return loadUpcomingDashboard();
            })
            .catch(showReminderError)
            .finally(function () { submitButton.disabled = false; });
    });

    function loadAquariumTasks() {
        return fetch(taskUrl(''))
            .then(function (response) {
                if (!response.ok) throw new Error(i18n.task.loadError + ' (' + response.status + ')');
                return response.json();
            })
            .then(function (tasks) {
                aquariumTasksData = tasks || [];
                if (!tasks.length) {
                    taskList.innerHTML = '<p class="text-muted small mb-0">'
                        + escapeHtml(i18n.task.empty) + '</p>';
                } else {
                    taskList.innerHTML = '<ul class="list-group list-group-flush">'
                        + aquariumTasksData.map(function (task) {
                            return renderReminderRow(task);
                        }).join('') + '</ul>';
                }
                return aquariumTasksData;
            })
            .catch(function (error) {
                taskList.innerHTML = '<p class="text-danger small mb-0">'
                    + escapeHtml(error.message) + '</p>';
                aquariumTasksData = [];
                console.error(i18n.task.loadError, error);
                throw error;
            });
    }

    upcomingModal.addEventListener('show.bs.modal', function () {
        loadUpcomingDashboard()
            .catch(function (error) {
                upcomingFullList.innerHTML = '<p class="text-danger small mb-0">'
                    + escapeHtml(error.message) + '</p>';
            });
    });

    // ── Generic delete function ───────────────────────────────
    function deleteEvent(eventId, onSuccess) {
        if (!confirm(i18n.confirm.deleteEvent)) return;

        fetch(apiUrl('/api/events/' + eventId), {method: 'DELETE'})
            .then(function (res) {
                if (!res.ok) throw new Error(i18n.error.delete + ': ' + res.status);
                calendar.refetchEvents();
                if (onSuccess) onSuccess();
            })
            .catch(function (err) {
                alert(i18n.error.delete + ': ' + err.message);
            });
    }

    // ── Handler borrar co2 (delegado en la lista) ─────────────────
    document.getElementById('modal-items-list').addEventListener('click', function (e) {
        var btn = e.target.closest('.btn-borrar-co2');
        if (!btn) return;

        deleteEvent(btn.dataset.id, function () {
            btn.closest('li').remove();
            var lista = document.getElementById('modal-items-list');
            if (lista.children.length === 0) {
                lista.innerHTML = '<li class="list-group-item text-muted text-center">' + i18n.label.noData + '</li>';
                setTimeout(function () {
                    bootstrap.Modal.getInstance(document.getElementById('eventoModal')).hide();
                }, 800);
            }
        });
    });

    // ── Parameter delete handler ─────────────────────────────────
    document.getElementById('btn-borrar-parametro').addEventListener('click', function () {
        deleteEvent(this.dataset.id, function () {
            bootstrap.Modal.getInstance(document.getElementById('parametrosViewModal')).hide();
        });
    });

    // ── Generic event delete handler (maintenance, info, etc.) ──
    var btnBorrarEvento = document.getElementById('btn-borrar-evento-modal');
    if (btnBorrarEvento) {
        btnBorrarEvento.addEventListener('click', function () {
            deleteEvent(this.dataset.id, function () {
                bootstrap.Modal.getInstance(document.getElementById('eventoModal')).hide();
            });
        });
    }

    // ── Context menu ───────────────────────────────────────────
    var ctxMenu = document.getElementById('cal-context-menu');
    var selectedDate = null;
    var longPressTimer = null;

    function showCtxMenu(x, y, dateStr) {
        selectedDate = dateStr;
        ctxMenu.style.display = 'block';
        var menuW = ctxMenu.offsetWidth;
        var menuH = ctxMenu.offsetHeight;
        var left = Math.min(x, window.innerWidth - menuW - 8);
        var top = Math.min(y, window.innerHeight - menuH - 8);
        ctxMenu.style.left = left + 'px';
        ctxMenu.style.top = top + 'px';
    }

    function hideCtxMenu() {
        ctxMenu.style.display = 'none';
    }

    calendarEl.addEventListener('contextmenu', function (e) {
        var cell = e.target.closest('.fc-daygrid-day');
        if (!cell) return;
        e.preventDefault();
        showCtxMenu(e.clientX, e.clientY, cell.dataset.date);
    });

    calendarEl.addEventListener('touchstart', function (e) {
        var cell = e.target.closest('.fc-daygrid-day');
        if (!cell) return;
        longPressTimer = setTimeout(function () {
            var touch = e.touches[0];
            showCtxMenu(touch.clientX, touch.clientY, cell.dataset.date);
        }, 600);
    }, {passive: true});

    calendarEl.addEventListener('touchend', function () {
        clearTimeout(longPressTimer);
    }, {passive: true});
    calendarEl.addEventListener('touchmove', function () {
        clearTimeout(longPressTimer);
    }, {passive: true});

    document.addEventListener('click', hideCtxMenu);
    document.addEventListener('touchstart', function (e) {
        if (!ctxMenu.contains(e.target)) hideCtxMenu();
    }, {passive: true});

    document.getElementById('ctx-add-reminder').addEventListener('click', function () {
        hideCtxMenu();
        taskDateFromCalendar = selectedDate;
        taskModal.show();
    });

    document.getElementById('ctx-add-life').addEventListener('click', function () {
        hideCtxMenu();
        openLifeRegistration(selectedDate);
    });

    // ── Treatment modal ─────────────────────────────────────────
    var tratamientoModal = new bootstrap.Modal(document.getElementById('tratamientoModal'));

    document.getElementById('ctx-add-tratamiento').addEventListener('click', function () {
        hideCtxMenu();
        document.getElementById('tratamiento-nombre').value = '';
        document.getElementById('tratamiento-valor').value = '';
        var fb = document.getElementById('tratamiento-feedback');
        fb.className = 'alert d-none';
        fb.innerText = '';
        document.getElementById('tratamiento-fecha-label').innerText =
            selectedDate ? i18n.label.datePrefix + ' ' + new Date(selectedDate + 'T00:00:00').toLocaleDateString(window.appLocale) : '';
        tratamientoModal.show();
    });

    document.getElementById('btn-guardar-tratamiento').addEventListener('click', function () {
        var nombre = document.getElementById('tratamiento-nombre').value.trim();
        var valor = document.getElementById('tratamiento-valor').value.trim();
        var fb = document.getElementById('tratamiento-feedback');

        if (!nombre || !valor) {
            fb.className = 'alert alert-warning';
            fb.innerText = i18n.validation.nameValueRequired;
            return;
        }

        var btnTxt = document.getElementById('btn-guardar-txt');
        var btnSpinner = document.getElementById('btn-guardar-spinner');
        btnTxt.innerText = i18n.button.saving;
        btnSpinner.classList.remove('d-none');
        document.getElementById('btn-guardar-tratamiento').disabled = true;

        fetch(apiUrl('/api/treatments'), {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({nombre: nombre, valor: valor, fecha: selectedDate})
        })
            .then(function (res) {
                if (!res.ok) throw new Error(i18n.error.save + ': ' + res.status);
                fb.className = 'alert alert-success';
                fb.innerText = i18n.success.treatmentSaved;
                calendar.refetchEvents();
                setTimeout(function () {
                    tratamientoModal.hide();
                }, 1200);
            })
            .catch(function (err) {
                fb.className = 'alert alert-danger';
                fb.innerText = i18n.error.save + ': ' + err.message;
            })
            .finally(function () {
                btnTxt.innerText = i18n.button.save;
                btnSpinner.classList.add('d-none');
                document.getElementById('btn-guardar-tratamiento').disabled = false;
            });
    });

    // ── Parameters modal ──────────────────────────────────────────
    var parametrosAddModal = new bootstrap.Modal(document.getElementById('parametrosAddModal'));

    document.getElementById('ctx-add-parameters').addEventListener('click', function () {
        hideCtxMenu();
        ['nh3', 'no2', 'no3', 'ph', 'kh', 'gh'].forEach(function (id) {
            document.getElementById('param-' + id).value = '';
        });
        var fb = document.getElementById('parametros-feedback');
        fb.className = 'alert d-none';
        fb.innerText = '';
        document.getElementById('parametros-fecha-label').innerText =
            selectedDate ? i18n.label.datePrefix + ' ' + new Date(selectedDate + 'T00:00:00').toLocaleDateString(window.appLocale) : '';
        parametrosAddModal.show();
    });

    document.getElementById('btn-guardar-parametros').addEventListener('click', function () {
        var fb = document.getElementById('parametros-feedback');
        var payload = {
            fecha: selectedDate,
            nh3: document.getElementById('param-nh3').value || null,
            no2: document.getElementById('param-no2').value || null,
            no3: document.getElementById('param-no3').value || null,
            ph: document.getElementById('param-ph').value || null,
            kh: document.getElementById('param-kh').value || null,
            gh: document.getElementById('param-gh').value || null,
            temp: document.getElementById('param-temp').value || null
        };

        var btnTxt = document.getElementById('btn-param-txt');
        var btnSpinner = document.getElementById('btn-param-spinner');
        btnTxt.innerText = i18n.button.saving;
        btnSpinner.classList.remove('d-none');
        document.getElementById('btn-guardar-parametros').disabled = true;

        fetch(apiUrl('/api/parameters'), {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        })
            .then(function (res) {
                if (!res.ok) throw new Error('Error ' + res.status);
                fb.className = 'alert alert-success';
                fb.innerText = i18n.success.parametersSaved;
                calendar.refetchEvents();
                setTimeout(function () {
                    parametrosAddModal.hide();
                }, 1200);
            })
            .catch(function (err) {
                fb.className = 'alert alert-danger';
                fb.innerText = i18n.error.save + ': ' + err.message;
            })
            .finally(function () {
                btnTxt.innerText = i18n.button.save;
                btnSpinner.classList.add('d-none');
                document.getElementById('btn-guardar-parametros').disabled = false;
            });
    });

    // ── View maintenance modal ───────────────────────────────────
    document.getElementById('btn-delete-maintenance').addEventListener('click', function () {
        deleteEvent(this.dataset.id, function () {
            bootstrap.Modal.getInstance(document.getElementById('maintenanceViewModal')).hide();
        });
    });

    // ── Add maintenance modal ──────────────────────────────
    var maintenanceAddModal = new bootstrap.Modal(document.getElementById('maintenanceAddModal'));

    document.getElementById('ctx-add-maintenance').addEventListener('click', function () {
        hideCtxMenu();
        document.getElementById('mant-water').value = '';
        document.getElementById('mant-prime').value = '';
        document.getElementById('mant-glass').checked = false;
        document.getElementById('mant-pruning').value = '';
        document.getElementById('mant-observations').value = '';
        var fb = document.getElementById('maintenance-feedback');
        fb.className = 'alert d-none';
        fb.innerText = '';
        document.getElementById('maintenance-date-label').innerText =
            selectedDate ? i18n.label.datePrefix + ' ' + new Date(selectedDate + 'T00:00:00').toLocaleDateString(window.appLocale) : '';

        writeFilterBooleans('f1', null);
        writeFilterBooleans('f2', null);

        maintenanceAddModal.show();
    });

    document.getElementById('btn-save-maintenance').addEventListener('click', function () {
        var fb = document.getElementById('maintenance-feedback');

        // ── Filters (read BEFORE fetch, not in .finally) ──
        var f1 = readFilterBooleans('f1');
        var f2 = readFilterBooleans('f2');

        var payload = {
            fecha: selectedDate,
            water: document.getElementById('mant-water').value || null,
            primeDose: document.getElementById('mant-prime').value || null,
            glass: document.getElementById('mant-glass').checked,
            pruning: document.getElementById('mant-pruning').value || null,
            observations: document.getElementById('mant-observations').value || '',
            f1Perlon: f1.Perlon,
            f1Sponge: f1.Sponge,
            f1Bio: f1.Bio,
            f1Mech: f1.Mech,
            f1BioMech: f1.BioMech,
            f2Perlon: f2.Perlon,
            f2Sponge: f2.Sponge,
            f2Bio: f2.Bio,
            f2Mech: f2.Mech,
            f2BioMech: f2.BioMech
        };

        var btnTxt = document.getElementById('btn-mant-txt');
        var btnSpinner = document.getElementById('btn-mant-spinner');
        btnTxt.innerText = i18n.button.saving;
        btnSpinner.classList.remove('d-none');
        document.getElementById('btn-save-maintenance').disabled = true;

        fetch(apiUrl('/api/maintenances'), {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        })
            .then(function (res) {
                if (!res.ok) throw new Error('Error ' + res.status);
                fb.className = 'alert alert-success';
                fb.innerText = i18n.success.maintenanceSaved;
                calendar.refetchEvents();
                loadLatestMaintenance();
                setTimeout(function () {
                    maintenanceAddModal.hide();
                }, 1200);
            })
            .catch(function (err) {
                fb.className = 'alert alert-danger';
                fb.innerText = i18n.error.save + ': ' + err.message;
            })
            .finally(function () {
                btnTxt.innerText = i18n.button.save;
                btnSpinner.classList.add('d-none');
                document.getElementById('btn-save-maintenance').disabled = false;
            });
    });

    // ── Info delete handler ───────────────────────────────────────
    document.getElementById('info-view-content').addEventListener('click', function (e) {
        var btn = e.target.closest('.btn-borrar-info');
        if (!btn) return;

        deleteEvent(btn.dataset.id, function () {
            var li = btn.closest('li');
            var list = li.closest('ul');
            li.remove();
            if (list.children.length === 0) {
                list.closest('.mb-3').classList.add('d-none');
            }
            // Close the modal when no visible groups remain
            var hayGrupos = ['info', 'fish', 'plants'].some(function (t) {
                return !document.getElementById('info-group-' + t).classList.contains('d-none');
            });
            if (!hayGrupos) {
                setTimeout(function () {
                    bootstrap.Modal.getInstance(document.getElementById('infoViewModal')).hide();
                }, 600);
            }
        });
    });

    // ── Photo delete handler ───────────────────────────────────────
    document.getElementById('btn-borrar-foto').addEventListener('click', function () {
        var photoId = this.dataset.id;
        if (!photoId) return;
        if (!confirm(i18n.confirm.deletePhoto)) return;

        fetch(apiUrl('/api/photos/' + photoId), {method: 'DELETE'})
            .then(function (res) {
                if (!res.ok) throw new Error(i18n.error.delete + res.status);
                // Remove the slide from the carousel
                var item = document.querySelector('.carousel-item[data-photo-id="' + photoId + '"]');
                var carousel = bootstrap.Carousel.getInstance(document.getElementById('photoCarousel'));
                var items = document.querySelectorAll('.carousel-item');

                if (items.length === 1) {
                    // This was the last photo
                    bootstrap.Modal.getInstance(document.getElementById('photoViewModal')).hide();
                    calendar.refetchEvents();
                    return;
                }

                // Mover al siguiente antes de borrar
                if (item.classList.contains('active')) {
                    carousel.next();
                    setTimeout(function () {
                        item.remove();
                        var remaining = document.querySelectorAll('.carousel-item');
                        document.getElementById('photo-counter').innerText = '1 / ' + remaining.length;
                    }, 350);
                } else {
                    item.remove();
                    var remaining = document.querySelectorAll('.carousel-item');
                    document.getElementById('photo-counter').innerText =
                        (Array.from(remaining).indexOf(document.querySelector('.carousel-item.active')) + 1) +
                        ' / ' + remaining.length;
                }
                calendar.refetchEvents();
            })
            .catch(function (err) {
                alert(i18n.error.delete + err.message);
            });
    });

    // ── Upload photo modal ──────────────────────────────────────────
    var photoAddModal = new bootstrap.Modal(document.getElementById('photoAddModal'));

    document.getElementById('ctx-add-photo').addEventListener('click', function () {
        hideCtxMenu();
        document.getElementById('photo-file').value = '';
        document.getElementById('photo-preview').classList.add('d-none');
        document.getElementById('photo-preview').src = '';
        var fb = document.getElementById('photo-feedback');
        fb.className = 'alert d-none';
        fb.innerText = '';
        document.getElementById('photo-fecha-label').innerText =
            selectedDate ? i18n.label.datePrefix + ' ' + new Date(selectedDate + 'T00:00:00').toLocaleDateString(window.appLocale) : '';
        photoAddModal.show();
    });

    // Preview when selecting a file
    document.getElementById('photo-file').addEventListener('change', function () {
        var file = this.files[0];
        if (!file) return;
        var reader = new FileReader();
        reader.onload = function (e) {
            var preview = document.getElementById('photo-preview');
            preview.src = e.target.result;
            preview.classList.remove('d-none');
        };
        reader.readAsDataURL(file);
    });

    document.getElementById('btn-guardar-photo').addEventListener('click', function () {
        var fb = document.getElementById('photo-feedback');
        var file = document.getElementById('photo-file').files[0];

        if (!file) {
            fb.className = 'alert alert-warning';
            fb.innerText = i18n.label.selectPhoto;
            return;
        }

        var btnTxt = document.getElementById('btn-photo-txt');
        var btnSpinner = document.getElementById('btn-photo-spinner');
        btnTxt.innerText = i18n.button.uploading;
        btnSpinner.classList.remove('d-none');
        document.getElementById('btn-guardar-photo').disabled = true;

        var formData = new FormData();
        formData.append('fecha', selectedDate);
        formData.append('foto', file);

        fetch(apiUrl('/api/photos'), {
            method: 'POST',
            body: formData
        })
            .then(function (res) {
                if (!res.ok) throw new Error(i18n.error.upload);
                fb.className = 'alert alert-success';
                fb.innerText = i18n.success.photoSaved;
                calendar.refetchEvents();
                setTimeout(function () {
                    photoAddModal.hide();
                }, 1200);
            })
            .catch(function (err) {
                fb.className = 'alert alert-danger';
                fb.innerText = i18n.error.upload + ': ' + err.message;
            })
            .finally(function () {
                btnTxt.innerText = i18n.button.save;
                btnSpinner.classList.add('d-none');
                document.getElementById('btn-guardar-photo').disabled = false;
            });
    });

    // ── Modal Insertar Info ───────────────────────────────────────
    var infoAddModal = new bootstrap.Modal(document.getElementById('infoAddModal'));

    document.getElementById('ctx-add-info').addEventListener('click', function () {
        hideCtxMenu();
        document.getElementById('info-type').value = 'info';
        document.getElementById('info-status').value = '';
        document.getElementById('info-observation').value = '';
        var fb = document.getElementById('info-feedback');
        fb.className = 'alert d-none';
        fb.innerText = '';
        document.getElementById('info-fecha-label').innerText =
            selectedDate ? i18n.label.datePrefix + ' ' + new Date(selectedDate + 'T00:00:00').toLocaleDateString(window.appLocale) : '';
        infoAddModal.show();
    });

    document.getElementById('btn-guardar-info').addEventListener('click', function () {
        var fb = document.getElementById('info-feedback');
        var tipoInfo = document.getElementById('info-type').value;
        var tipoEvento = tipoInfo === 'INFO' ? 'info' : 'live';

        var payload = {
            fecha: selectedDate,
            type: tipoInfo,
            tipoEvento: tipoEvento,
            status: document.getElementById('info-status').value || null,
            observation: document.getElementById('info-observation').value || null
        };

        var btnTxt = document.getElementById('btn-info-txt');
        var btnSpinner = document.getElementById('btn-info-spinner');
        btnTxt.innerText = i18n.button.saving;
        btnSpinner.classList.remove('d-none');
        document.getElementById('btn-guardar-info').disabled = true;

        fetch(apiUrl('/api/events/information'), {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        })
            .then(function (res) {
                if (!res.ok) throw new Error('Error ' + res.status);
                fb.className = 'alert alert-success';
                fb.innerText = i18n.success.infoSaved;
                calendar.refetchEvents();
                setTimeout(function () {
                    infoAddModal.hide();
                }, 1200);
            })
            .catch(function (err) {
                fb.className = 'alert alert-danger';
                fb.innerText = i18n.error.saveInfo + ': ' + err.message;
            })
            .finally(function () {
                btnTxt.innerText = i18n.button.save;
                btnSpinner.classList.add('d-none');
                document.getElementById('btn-guardar-info').disabled = false;
            });
    });

    function iconForType(type) {
        switch (type) {
            case 'life-fish':
                return '<svg aria-hidden="true" viewBox="0 0 24 24" width="1em" height="1em" '
                    + 'style="color:#3788d8" fill="currentColor">'
                    + '<path d="M3 12c2.2-3.1 5.2-4.7 9-4.7 2.4 0 4.5.8 6.4 2.3L22 7v10l-3.6-2.6'
                    + 'c-1.9 1.5-4 2.3-6.4 2.3-3.8 0-6.8-1.6-9-4.7Zm7.1-1.3a1 1 0 1 0 0 2 1 1 0 0 0 0-2Z"/>'
                    + '</svg>';
            case 'life-plant':
                return '<i class="bi bi-flower1" style="color:#3788d8"></i>';
            case 'reminder':
                return '<i class="bi bi-clock"></i>';
            case 'co2Tratamiento':
                return '<i class="bi bi-wind"></i>';
            case 'info':
                return '<i class="bi bi-info-circle"></i>';
            case 'manteiment':
                return '<i class="bi bi-tools"></i>';
            case 'parameters':
                return '<i class="bi bi-sliders"></i>';
            case 'live':
                return '<i class="bi bi-camera-video"></i>';
            case 'photo':
                return '<i class="bi bi-camera"></i>';
            default:
                return '<i class="bi bi-calendar-event"></i>';
        }
    }

    function formatLocalizedDate(iso) {
        if (!iso) return '-';
        return new Intl.DateTimeFormat(window.appLocale, {timeZone: 'UTC'})
            .format(new Date(iso + 'T00:00:00Z'));
    }

    function badgesForEvento(ev) {
        var html = '';
        if (ev.diasRestantes < 0 && ev.diasRestantes !== -1) html += '<span class="badge bg-danger">' + window.i18n.label.vencido + '</span>';
        if (ev.diasRestantes === 0) html += '<span class="badge bg-warning text-dark">' + window.i18n.label.hoy + '</span>';
        if (ev.diasRestantes === 1) html += '<span class="badge bg-warning text-dark">' + window.i18n.label.manana + '</span>';
        if (ev.diasRestantes > 1) html += '<span class="badge bg-success">' + ev.diasRestantes + window.i18n.label.diaAbrev + '</span>';
        if (ev.diasRestantes === -1) html += '<span class="badge bg-secondary">' + window.i18n.label.noData + '</span>';
        return html;
    }

    function loadUpcomingEvents() {
        return fetch(apiUrl('/api/events/upcoming'))
            .then(function (res) {
                if (!res.ok) throw new Error('Error ' + res.status);
                return res.json();
            })
            .then(function (data) {
                var events = data || [];
                upcomingEventsData = events;
                upcomingFullList.innerHTML = events.length
                    ? renderUpcomingRows(events)
                    : '<p class="text-muted small mb-0">' + escapeHtml(i18n.label.noEvent) + '</p>';
                return events;
            })
            .catch(function (err) {
                var message = escapeHtml(i18n.upcoming.loadError + ': ' + err.message);
                upcomingFullList.innerHTML = '<p class="text-danger small mb-0">' + message + '</p>';
                throw err;
            });
    }

    function loadUpcomingDashboard() {
        return Promise.all([loadUpcomingEvents(), loadAquariumTasks()])
            .then(function () {
                renderUpcomingPreview();
            });
    }

    function renderUpcomingPreview() {
        var dueSoonEvents = upcomingEventsData.filter(function (item) {
            return item.proximaFecha && item.diasRestantes !== -1 && item.diasRestantes < 7;
        }).map(function (item) {
            return {kind: 'event', date: item.proximaFecha, remaining: item.diasRestantes, data: item};
        });
        var dueSoonTasks = aquariumTasksData.filter(function (task) {
            return taskDaysRemaining(task.actionDate) < 7;
        }).map(function (task) {
            return {kind: 'reminder', date: task.actionDate, remaining: taskDaysRemaining(task.actionDate), data: task};
        });
        var dueSoon = dueSoonEvents.concat(dueSoonTasks).sort(function (first, second) {
            return first.date.localeCompare(second.date);
        });
        document.getElementById('proximos-eventos-list').innerHTML = dueSoon.length
            ? dueSoon.map(function (item) {
                return item.kind === 'reminder'
                    ? renderReminderRow(item.data, item.remaining)
                    : renderUpcomingRows([item.data]);
            }).join('')
            : '<p class="text-muted small mb-0">' + escapeHtml(i18n.upcoming.noSoon) + '</p>';
    }

    function taskDaysRemaining(isoDate) {
        var parts = isoDate.split('-').map(Number);
        var dueDate = Date.UTC(parts[0], parts[1] - 1, parts[2]);
        var today = new Date();
        var todayDate = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
        return Math.floor((dueDate - todayDate) / 86400000);
    }

    function renderReminderRow(task, daysRemaining) {
        var remaining = daysRemaining === undefined ? taskDaysRemaining(task.actionDate) : daysRemaining;
        return '<div class="d-flex align-items-center justify-content-between py-1 border-bottom">'
            + '<button type="button" class="btn btn-link p-0 text-reset text-decoration-none text-start task-complete-button"'
            + ' data-task-id="' + encodeURIComponent(task.id) + '"'
            + ' aria-label="' + escapeHtml(i18n.task.openReminder + ': ' + task.title) + '">'
            + '<div class="d-flex align-items-center gap-2">'
            + '<span style="font-size:1.2rem;"><i class="bi bi-clock text-warning" aria-hidden="true"></i></span>'
            + '<span><span class="small fw-semibold">' + escapeHtml(task.title) + '</span>'
            + (task.description ? '<span class="small text-muted d-block">'
                + escapeHtml(task.description) + '</span>' : '')
            + '</span></div></button>'
            + '<div class="d-flex align-items-center gap-2">'
            + '<span class="small text-muted">' + escapeHtml(formatLocalizedDate(task.actionDate)) + '</span>'
            + '<span class="badge ' + (remaining < 0 ? 'bg-danger' : remaining < 2 ? 'bg-warning text-dark' : 'bg-success') + '">'
            + escapeHtml(remaining < 0 ? window.i18n.label.vencido
                : remaining === 0 ? window.i18n.label.hoy
                    : remaining === 1 ? window.i18n.label.manana
                        : remaining + window.i18n.label.diaAbrev) + '</span>'
            + '</div></div>';
    }

    function renderUpcomingRows(events) {
        return events.map(function (event) {
            var dateLabel = event.proximaFecha
                ? formatLocalizedDate(event.proximaFecha)
                : i18n.upcoming.noDate;
            return '<div class="d-flex align-items-center justify-content-between py-2 border-bottom">'
                + '<div class="d-flex align-items-center gap-2">'
                + '<span style="font-size:1.2rem;">' + iconForType(event.type) + '</span>'
                + '<span class="small fw-semibold text-capitalize">'
                + escapeHtml(event.type || '-') + '</span>'
                + '</div>'
                + '<div class="d-flex align-items-center gap-2">'
                + '<span class="small text-muted">' + escapeHtml(dateLabel) + '</span>'
                + badgesForEvento(event)
                + '</div>'
                + '</div>';
        }).join('');
    }

    function loadLatestParameters() {
        return fetch(apiUrl('/api/parameters/summary'))
            .then(function (res) {
                if (!res.ok) throw new Error('Error ' + res.status);
                return res.json();
            })
            .then(function (data) {
                document.getElementById('pl-nh3').innerText = data.nh3 ?? '-';
                document.getElementById('pl-no2').innerText = data.no2 ?? '-';
                document.getElementById('pl-no3').innerText = data.no3 ?? '-';
                document.getElementById('pl-ph').innerText = data.ph ?? '-';
                document.getElementById('pl-kh').innerText = data.kh ?? '-';
                document.getElementById('pl-gh').innerText = data.gh ?? '-';

                var estadoEl = document.getElementById('parametros-estado');
                var isHealthy = data.estado === i18n.label.healthy;
                estadoEl.innerText = data.estado;
                estadoEl.style.color = isHealthy ? 'green' : 'red';
            })
            .catch(function (err) {
                console.error(window.i18n.error.loadParameters, err);
            });
    }

    function loadLatestMaintenance() {
        var fechaEl = document.getElementById('maintenance-last-date');
        var cont = document.getElementById('maintenance-last-content');

        return fetch(apiUrl('/api/maintenances/last'))
            .then(function (res) {
                if (res.status === 204) return null;
                if (!res.ok) throw new Error('Error ' + res.status);
                return res.json();
            })
            .then(function (data) {
                if (!data) return; // Keep the empty message already provided by the HTML

                fechaEl.innerText = formatLocalizedDate(data.fecha);

                var items = '';
                console.log(data);
                if (data.water != null) {
                    items += '<li class="list-group-item d-flex justify-content-between px-0">'
                        + '<span class="text-muted small"><i class="bi bi-droplet"></i> ' + window.i18n.label.aguaCambiada + '</span>'
                        + '<strong class="small">' + data.water + ' L</strong></li>';
                }
                if (data.primeDose != null) {
                    items += '<li class="list-group-item d-flex justify-content-between px-0">'
                        + '<span class="text-muted small"><i class="bi bi-flask"></i> ' + window.i18n.label.prime + '</span>'
                        + '<strong class="small">' + data.primeDose + ' ml</strong></li>';
                }
                if (data.observations != null) {
                    items += '<li class="list-group-item d-flex justify-content-between px-0">'
                        + '<span class="text-muted small"><i class="bi bi-flask"></i> ' + window.i18n.label.observaciones + '</span>'
                        + '<strong class="small">' + escapeHtml(data.observations) + '</strong></li>';
                }
                if (data.pruning != null) {
                    items += '<li class="list-group-item d-flex justify-content-between px-0">'
                        + '<span class="text-muted small"><i class="bi bi-flask"></i> ' + window.i18n.label.podas + '</span>'
                        + '<strong class="small">' + escapeHtml(data.pruning) + '</strong></li>';
                }

                if (data.glass != null) {
                    items += '<li class="list-group-item d-flex justify-content-between px-0">'
                        + '<span class="text-muted small"><i class="bi bi-flask"></i> ' + window.i18n.label.vidrio + '</span>'
                        + '<strong class="small">' + (data.glass === true ? window.i18n.label.yes : window.i18n.label.no) + '</strong></li>';
                }

                var filters = data.filters || [];
                var filtro1 = filters[0] || null;
                var filtro2 = filters[1] || null;
                items += '<li class="list-group-item d-flex justify-content-between px-0">'
                    + '<span class="text-muted small"><i class="bi bi-funnel"></i> ' + window.i18n.label.filtro1 + '</span>'
                    + '<strong class="small">' + describeFilter(filtro1) + '</strong></li>';
                items += '<li class="list-group-item d-flex justify-content-between px-0">'
                    + '<span class="text-muted small"><i class="bi bi-funnel"></i> ' + window.i18n.label.filtro2 + '</span>'
                    + '<strong class="small">' + describeFilter(filtro2) + '</strong></li>';


                cont.innerHTML = '<ul class="list-group list-group-flush">' + items + '</ul>';
            })
            .catch(function (err) {
                cont.innerHTML = '<p class="text-danger small mb-0">' + window.i18n.error.load + '</p>';
            });
    }

    // ── Filter helpers (perlon, sponge, biological, mechanical, biomechanical) ──
    function readFilterBooleans(prefix) {
        return {
            Perlon: document.getElementById(prefix + '-perlon').checked,
            Sponge: document.getElementById(prefix + '-sponge').checked,
            Bio: document.getElementById(prefix + '-bio').checked,
            Mech: document.getElementById(prefix + '-mech').checked,
            BioMech: document.getElementById(prefix + '-biomech').checked
        };
    }

    function writeFilterBooleans(prefix, mask) {
        document.getElementById(prefix + '-perlon').checked = !!(mask && mask.perlon);
        document.getElementById(prefix + '-sponge').checked = !!(mask && mask.sponge);
        document.getElementById(prefix + '-bio').checked = !!(mask && mask.bio);
        document.getElementById(prefix + '-mech').checked = !!(mask && mask.mech);
        document.getElementById(prefix + '-biomech').checked = !!(mask && mask.bioMech);
    }

    function describeFilter(f) {
        if (!f) return i18n.label.noClean;
        var labels = [];
        if (f.perlon) labels.push(i18n.label.perlon);
        if (f.sponge) labels.push(i18n.label.esponja);
        if (f.bio) labels.push(i18n.label.biologico);
        if (f.mech) labels.push(i18n.label.mecanico);
        if (f.bioMech) labels.push(i18n.label.bioMeca);
        return labels.length ? labels.join(', ') : i18n.label.noClean;
    }

    initializeHistoryExport(aquariumId, i18n);

    Promise.all([
        loadUpcomingDashboard(),
        loadLatestParameters(),
        loadLatestMaintenance(),
        loadDashboardData()
    ]).catch(function (error) {
        console.error('Error loading initial dashboard data', error);
    });

});
