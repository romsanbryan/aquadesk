import {escapeHtml} from './utils.js';

export function initializeDashboardData(apiUrl, i18n) {
    function loadParameterTable(page = 0, size = 5) {
        return fetch(apiUrl('/api/parameters/table?page=' + page + '&size=' + size))
            .then(function (res) {
                if (!res.ok) throw new Error('Error ' + res.status);
                return res.json();
            })
            .then(function (data) {
                var tbody = document.getElementById('parametrosTableBody');
                tbody.innerHTML = data.map(function (param) {
                    return '<tr>' +
                        '<td>' + (param.id || '-') + '</td>' +
                        '<td>' + (param.idEvent || '-') + '</td>' +
                        '<td>' + (param.nh3 || '-') + '</td>' +
                        '<td>' + (param.no2 || '-') + '</td>' +
                        '<td>' + (param.no3 || '-') + '</td>' +
                        '<td>' + (param.ph || '-') + '</td>' +
                        '<td>' + (param.kh || '-') + '</td>' +
                        '<td>' + (param.gh || '-') + '</td>' +
                        '</tr>';
                }).join('');
            })
            .catch(function (err) {
                console.error(i18n.error.loadParameters, err);
            });
    }

    function loadCurrentParameterPage(page = 0, size = 5) {
        return fetch(apiUrl('/api/parameters/table/current-page?page=' + page + '&size=' + size))
            .then(function (res) {
                if (!res.ok) throw new Error('Error ' + res.status);
                return res.json();
            })
            .then(function (currentPage) {
                window.parametrosCurrentPage = currentPage;
                loadParameterPagination();
            })
            .catch(function (err) {
                console.error(i18n.error.load, err);
            });
    }

    function loadParameterTotalPages(page = 0, size = 5) {
        return fetch(apiUrl('/api/parameters/table/total-pages?page=' + page + '&size=' + size))
            .then(function (res) {
                if (!res.ok) throw new Error('Error ' + res.status);
                return res.json();
            })
            .then(function (totalPages) {
                window.parametrosTotalPages = totalPages;
                loadParameterPagination();
            })
            .catch(function (err) {
                console.error(i18n.error.load, err);
            });
    }

    function loadParameterTotalElements() {
        return fetch(apiUrl('/api/parameters/table/total-elements'))
            .then(function (res) {
                if (!res.ok) throw new Error('Error ' + res.status);
                return res.json();
            })
            .then(function (totalElements) {
                document.getElementById('parametrosTotalElements').innerText = totalElements;
            })
            .catch(function (err) {
                console.error(i18n.error.load, err);
            });
    }

    function loadParameterPagination() {
        if (window.parametrosCurrentPage === undefined || window.parametrosTotalPages === undefined) {
            return;
        }

        var pagination = document.getElementById('parametrosPagination');
        var paginationHtml = '';

        if (window.parametrosCurrentPage > 0) {
            paginationHtml += '<li class="page-item"><a class="page-link" href="#" onclick="loadPaginatedParameters(' + (window.parametrosCurrentPage - 1) + ', 5); return false;">' + i18n.label.anterior + '</a></li>';
        }

        for (var i = 0; i < window.parametrosTotalPages; i++) {
            var activeClass = (i === window.parametrosCurrentPage) ? ' active' : '';
            paginationHtml += '<li class="page-item' + activeClass + '"><a class="page-link" href="#" onclick="loadPaginatedParameters(' + i + ', 5); return false;">' + (i + 1) + '</a></li>';
        }

        if (window.parametrosCurrentPage < window.parametrosTotalPages - 1) {
            paginationHtml += '<li class="page-item"><a class="page-link" href="#" onclick="loadPaginatedParameters(' + (window.parametrosCurrentPage + 1) + ', 5); return false;">' + i18n.label.siguiente + '</a></li>';
        }

        pagination.innerHTML = paginationHtml;
    }

    function loadPaginatedParameters(page = 0, size = 5) {
        return Promise.all([
            loadParameterTable(page, size),
            loadCurrentParameterPage(page, size),
            loadParameterTotalPages(page, size)
        ]);
    }

    function loadParameterRanges() {
        return fetch(apiUrl('/api/parameter-ranges'))
            .then(function (res) {
                if (!res.ok) throw new Error('Error ' + res.status);
                return res.json();
            })
            .then(function (data) {
                console.log('Parameter ranges loaded:', data);
            })
            .catch(function (err) {
                console.error(i18n.error.loadRanges, err);
            });
    }

    function loadNotificationConfig() {
        return fetch(apiUrl('/api/notifications/active'))
            .then(function (res) {
                if (!res.ok) throw new Error('Error ' + res.status);
                return res.json();
            })
            .then(function (data) {
                console.log('Configuraciones de notificaciones cargadas:', data);
            })
            .catch(function (err) {
                console.error(i18n.error.load, err);
            });
    }

    function loadNotifications() {
        return fetch('/api/notifications')
            .then(function (res) {
                if (!res.ok) throw new Error('Error ' + res.status);
                return res.json();
            })
            .then(function (data) {
                var wrapper = document.getElementById('notif-bell-wrapper');
                var badge = document.getElementById('notif-bell-badge');
                var list = document.getElementById('notif-bell-list');

                var anomalias = data || [];

                if (anomalias.length === 0) {
                    wrapper.classList.add('d-none');
                    return;
                }

                badge.innerText = anomalias.length;
                list.innerHTML = anomalias.map(function (n) {
                    return '<li>' +
                        '<div class="dropdown-item d-flex justify-content-between py-2 border-bottom">' +
                        '<span class="fw-bold">' + escapeHtml(n.aquariumName || '-') + ':</span>' +
                        '<span class="text-muted small ms-2">' +
                        escapeHtml(n.parameter ?? '-') + ': ' +
                        escapeHtml(n.value ?? '-') + '</span>' +
                        '</div>' +
                        '</li>';
                }).join('');

                wrapper.classList.remove('d-none');
            })
            .catch(function (err) {
                console.error(window.i18n.error.load, err);
            });
    }

    window.loadPaginatedParameters = loadPaginatedParameters;

    function loadDashboardData() {
        return Promise.all([
            loadPaginatedParameters(),
            loadParameterTotalElements(),
            loadParameterRanges(),
            loadNotificationConfig(),
            loadNotifications()
        ]);
    }

    return {loadDashboardData: loadDashboardData, loadNotifications: loadNotifications};
}
