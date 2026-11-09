document.addEventListener('DOMContentLoaded', function () {
    var labels = document.getElementById('aquarium-ui-labels').dataset;
    var aquariumList = document.getElementById('aquarium-list');
    var count = document.getElementById('aquarium-count');
    var emptyRow = document.getElementById('aquarium-empty-row');
    var loadError = document.getElementById('aquarium-load-error');
    var detailsModal = bootstrap.Modal.getOrCreateInstance(
        document.getElementById('aquarium-details-modal'));
    var editModal = bootstrap.Modal.getOrCreateInstance(
        document.getElementById('aquarium-edit-modal'));
    var filterModal = bootstrap.Modal.getOrCreateInstance(
        document.getElementById('aquarium-filter-modal'));
    var editForm = document.getElementById('aquarium-edit-form');
    var filterForm = document.getElementById('aquarium-filter-form');
    var createForm = document.getElementById('aquarium-create-form');
    var createFilterContainer = document.getElementById('aquarium-create-filters');
    var createFilterTemplate = document.getElementById('aquarium-create-filter-template');
    var aquariums = [];
    var activeAquariumId = null;
    var reopenDetailsAfterFilterSave = false;

    function showError(element, error) {
        element.textContent = error.message || 'HTTP error';
        element.classList.remove('d-none');
    }

    function request(url, method, data) {
        var options = {method: method};
        if (data !== undefined) {
            options.headers = {'Content-Type': 'application/json'};
            options.body = JSON.stringify(data);
        }
        return fetch(url, options).then(function (response) {
            if (!response.ok) {
                return response.json().then(function (body) {
                    throw new Error(body.message || ('HTTP ' + response.status));
                }).catch(function (error) {
                    if (error.message) throw error;
                    throw new Error('HTTP ' + response.status);
                });
            }
            if (response.status === 204) return null;
            return response.text().then(function (body) {
                return body ? JSON.parse(body) : null;
            });
        });
    }

    function aquariumById(id) {
        return aquariums.find(function (aquarium) {
            return String(aquarium.id) === String(id);
        });
    }

    function textCell(value) {
        var cell = document.createElement('td');
        cell.textContent = value == null || value === '' ? '-' : value;
        return cell;
    }

    function makeButton(action, id, label, style) {
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'btn btn-sm ' + style;
        button.dataset.action = action;
        button.dataset.id = id;
        button.textContent = label;
        return button;
    }

    function renderAquariums() {
        aquariumList.replaceChildren();
        aquariumList.appendChild(emptyRow);
        count.textContent = String(aquariums.length);
        emptyRow.classList.toggle('d-none', aquariums.length > 0);

        aquariums.forEach(function (aquarium) {
            var row = document.createElement('tr');
            row.appendChild(textCell(aquarium.name));
            row.appendChild(textCell(aquarium.type));
            row.appendChild(textCell(String(aquarium.filters.length)));

            var actionsCell = document.createElement('td');
            var actions = document.createElement('div');
            actions.className = 'd-flex flex-wrap gap-2';

            var management = document.createElement('a');
            management.className = 'btn btn-primary btn-sm';
            management.href = '/gestion?aquariumId=' + encodeURIComponent(aquarium.id);
            management.textContent = labels.management;
            actions.appendChild(management);
            actions.appendChild(makeButton('edit', aquarium.id, labels.edit, 'btn-outline-primary'));
            actions.appendChild(makeButton('details', aquarium.id, labels.viewDetails, 'btn-secondary'));
            actions.appendChild(makeButton('delete', aquarium.id, labels.delete, 'btn-outline-danger'));
            actionsCell.appendChild(actions);
            row.appendChild(actionsCell);
            aquariumList.appendChild(row);
        });
    }

    function addDetailField(fields, label, value) {
        var heading = document.createElement('dt');
        heading.className = 'col-sm-4';
        heading.textContent = label;
        var content = document.createElement('dd');
        content.className = 'col-sm-8';
        content.textContent = value == null || value === '' ? '-' : value;
        fields.append(heading, content);
    }

    function showDetails(aquarium) {
        activeAquariumId = aquarium.id;
        document.getElementById('aquarium-details-title').textContent = aquarium.name;
        var fields = document.getElementById('aquarium-details-fields');
        fields.replaceChildren();
        addDetailField(fields, labels.id, aquarium.id);
        addDetailField(fields, labels.name, aquarium.name);
        addDetailField(fields, labels.type, aquarium.type);
        addDetailField(fields, labels.volume, aquarium.volume == null ? null : aquarium.volume + ' L');
        addDetailField(fields, labels.glassThickness, aquarium.glassThickness);
        addDetailField(fields, labels.size, aquarium.size);
        addDetailField(fields, labels.heater, aquarium.heater);
        addDetailField(fields, labels.light, aquarium.light);
        addDetailField(fields, labels.substrate, aquarium.substrate);
        addDetailField(fields, labels.createdAt, aquarium.createdAt);
        addDetailField(fields, labels.updatedAt, aquarium.updatedAt);

        var filters = document.getElementById('aquarium-details-filters');
        filters.replaceChildren();
        if (aquarium.filters.length === 0) {
            var emptyFilters = document.createElement('p');
            emptyFilters.className = 'text-muted';
            emptyFilters.textContent = labels.noFilters;
            filters.appendChild(emptyFilters);
        }
        aquarium.filters.forEach(function (filter) {
            var card = document.createElement('div');
            card.className = 'card mb-3';
            var body = document.createElement('div');
            body.className = 'card-body';
            var title = document.createElement('h4');
            title.className = 'h6 mb-3';
            title.textContent = filter.name || labels.filter;
            body.appendChild(title);

            var actions = document.createElement('div');
            actions.className = 'd-flex gap-2 mb-3';
            var editFilter = makeButton('filter-edit', filter.id, labels.edit, 'btn-outline-primary');
            editFilter.dataset.aquariumId = aquarium.id;
            var deleteFilter = makeButton('filter-delete', filter.id, labels.deleteFilter, 'btn-outline-danger');
            deleteFilter.dataset.aquariumId = aquarium.id;
            actions.append(editFilter, deleteFilter);
            body.appendChild(actions);

            var details = document.createElement('dl');
            details.className = 'row mb-0';
            addDetailField(details, labels.id, filter.id);
            addDetailField(details, labels.biological, filter.materialBiological);
            addDetailField(details, labels.mechanical, filter.materialMechanical);
            addDetailField(details, labels.bioMechanical, filter.materialBioMechanical);
            addDetailField(details, labels.sponge, filter.sponge ? labels.yes : labels.no);
            addDetailField(details, labels.carbonActive, filter.carbonActive ? labels.yes : labels.no);
            addDetailField(details, labels.perlon, filter.perlon ? labels.yes : labels.no);
            addDetailField(details, labels.createdAt, filter.createdAt);
            addDetailField(details, labels.updatedAt, filter.updatedAt);
            body.appendChild(details);
            card.appendChild(body);
            filters.appendChild(card);
        });
        detailsModal.show();
    }

    function openEdit(aquarium) {
        editForm.reset();
        editForm.dataset.aquariumId = aquarium.id;
        editForm.querySelector('.form-error').classList.add('d-none');
        ['name', 'type', 'volume', 'glassThickness', 'size', 'heater', 'light', 'substrate']
            .forEach(function (key) {
                editForm.elements[key].value = aquarium[key] == null ? '' : aquarium[key];
            });
        editModal.show();
    }

    function openFilterForm(aquariumId, filter) {
        filterForm.reset();
        filterForm.dataset.aquariumId = aquariumId;
        filterForm.elements.filterId.value = filter ? filter.id : '';
        filterForm.querySelector('.form-error').classList.add('d-none');
        if (filter) {
            ['name', 'materialBiological', 'materialMechanical', 'materialBioMechanical']
                .forEach(function (key) {
                    filterForm.elements[key].value = filter[key] || '';
                });
            filterForm.elements.sponge.checked = filter.sponge;
            filterForm.elements.carbonActive.checked = filter.carbonActive;
            filterForm.elements.perlon.checked = filter.perlon;
            document.getElementById('aquarium-filter-title').textContent = labels.editFilter;
        } else {
            document.getElementById('aquarium-filter-title').textContent = labels.addFilter;
        }
        filterModal.show();
    }

    function switchFromDetails(openNext) {
        var element = document.getElementById('aquarium-details-modal');
        element.addEventListener('hidden.bs.modal', openNext, {once: true});
        detailsModal.hide();
    }

    function reloadAquariums() {
        loadError.classList.add('d-none');
        return request('/api/aquariums', 'GET').then(function (data) {
            aquariums = data;
            renderAquariums();
        }).catch(function (error) {
            showError(loadError, error);
            throw error;
        });
    }

    function formData(form) {
        var data = {};
        ['name', 'type', 'volume', 'glassThickness', 'size', 'heater', 'light', 'substrate']
            .forEach(function (key) {
                var value = form.querySelector('[name="' + key + '"]').value;
                data[key] = value === '' ? null : value;
            });
        data.volume = data.volume ? Number(data.volume) : null;
        return data;
    }

    document.getElementById('aquarium-create-add-filter').addEventListener('click', function () {
        createFilterContainer.appendChild(createFilterTemplate.content.cloneNode(true));
    });

    createFilterContainer.addEventListener('click', function (event) {
        var removeButton = event.target.closest('.aquarium-create-remove-filter');
        if (removeButton) removeButton.closest('.aquarium-create-filter').remove();
    });

    createForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var errorElement = document.getElementById('aquarium-create-error');
        errorElement.classList.add('d-none');
        var data = formData(createForm);
        data.filters = Array.from(createFilterContainer.querySelectorAll('.aquarium-create-filter'))
            .map(function (filterElement) {
                return {
                    name: filterElement.querySelector('[name="name"]').value || null,
                    materialBiological: filterElement.querySelector('[name="materialBiological"]').value || null,
                    materialMechanical: filterElement.querySelector('[name="materialMechanical"]').value || null,
                    materialBioMechanical: filterElement.querySelector('[name="materialBioMechanical"]').value || null,
                    sponge: filterElement.querySelector('[name="sponge"]').checked,
                    carbonActive: filterElement.querySelector('[name="carbonActive"]').checked,
                    perlon: filterElement.querySelector('[name="perlon"]').checked
                };
            });
        request('/api/aquariums', 'POST', data).then(function () {
            bootstrap.Modal.getOrCreateInstance(document.getElementById('aquarium-create-modal')).hide();
            createForm.reset();
            createFilterContainer.replaceChildren();
            return reloadAquariums();
        }).catch(function (error) {
            showError(errorElement, error);
        });
    });

    aquariumList.addEventListener('click', function (event) {
        var button = event.target.closest('button[data-action]');
        if (!button) return;
        var aquarium = aquariumById(button.dataset.id);
        if (!aquarium) return;
        if (button.dataset.action === 'details') showDetails(aquarium);
        if (button.dataset.action === 'edit') openEdit(aquarium);
        if (button.dataset.action === 'delete'
                && window.confirm(labels.deleteConfirm + '\n\n' + aquarium.name)) {
            request('/api/aquariums/' + encodeURIComponent(aquarium.id), 'DELETE')
                .then(reloadAquariums)
                .catch(function (error) { window.alert(error.message); });
        }
    });

    document.getElementById('aquarium-details-edit').addEventListener('click', function () {
        var aquarium = aquariumById(activeAquariumId);
        if (!aquarium) return;
        switchFromDetails(function () { openEdit(aquarium); });
    });

    document.getElementById('aquarium-details-add-filter').addEventListener('click', function () {
        var id = activeAquariumId;
        reopenDetailsAfterFilterSave = true;
        switchFromDetails(function () { openFilterForm(id, null); });
    });

    document.getElementById('aquarium-details-filters').addEventListener('click', function (event) {
        var button = event.target.closest('button[data-action]');
        if (!button) return;
        if (button.dataset.action === 'filter-edit') {
            var aquarium = aquariumById(button.dataset.aquariumId);
            if (!aquarium) return;
            var filter = aquarium.filters.find(function (item) {
                return String(item.id) === button.dataset.id;
            });
            reopenDetailsAfterFilterSave = true;
            switchFromDetails(function () { openFilterForm(aquarium.id, filter); });
        } else if (button.dataset.action === 'filter-delete') {
            var aquariumId = button.dataset.aquariumId;
            if (!window.confirm('¿Eliminar este filtro?')) return;
            request('/api/aquariums/' + encodeURIComponent(aquariumId)
                + '/filters/' + encodeURIComponent(button.dataset.id), 'DELETE')
                .then(function () {
                    return reloadAquariums().then(function () {
                        var updated = aquariumById(aquariumId);
                        if (updated) showDetails(updated);
                    });
                }).catch(function (error) { window.alert(error.message); });
        }
    });

    editForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var errorElement = editForm.querySelector('.form-error');
        errorElement.classList.add('d-none');
        request('/api/aquariums/' + encodeURIComponent(editForm.dataset.aquariumId),
            'PUT', formData(editForm)).then(function () {
            editModal.hide();
            return reloadAquariums();
        }).catch(function (error) { showError(errorElement, error); });
    });

    filterForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var errorElement = filterForm.querySelector('.form-error');
        errorElement.classList.add('d-none');
        var values = Object.fromEntries(new FormData(filterForm).entries());
        var filterId = values.filterId;
        delete values.filterId;
        values.name = values.name || null;
        values.materialBiological = values.materialBiological || null;
        values.materialMechanical = values.materialMechanical || null;
        values.materialBioMechanical = values.materialBioMechanical || null;
        values.sponge = filterForm.elements.sponge.checked;
        values.carbonActive = filterForm.elements.carbonActive.checked;
        values.perlon = filterForm.elements.perlon.checked;
        var url = filterId
            ? '/api/aquariums/' + encodeURIComponent(filterForm.dataset.aquariumId)
                + '/filters/' + encodeURIComponent(filterId)
            : '/api/aquariums/' + encodeURIComponent(filterForm.dataset.aquariumId) + '/filters';
        request(url, filterId ? 'PUT' : 'POST', values).then(function () {
            return reloadAquariums().then(function () {
                if (reopenDetailsAfterFilterSave) {
                    var updated = aquariumById(filterForm.dataset.aquariumId);
                    if (updated) {
                        var element = document.getElementById('aquarium-filter-modal');
                        element.addEventListener('hidden.bs.modal', function () {
                            showDetails(updated);
                        }, {once: true});
                    }
                }
                reopenDetailsAfterFilterSave = false;
                filterModal.hide();
            });
        }).catch(function (error) { showError(errorElement, error); });
    });

    fetch('/api/notifications')
        .then(function (response) {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.json();
        })
        .then(function (notifications) {
            var items = notifications || [];
            var wrapper = document.getElementById('notif-bell-wrapper');
            var badge = document.getElementById('notif-bell-badge');
            var list = document.getElementById('notif-bell-list');
            if (!wrapper || !badge || !list || items.length === 0) return;
            badge.textContent = items.length;
            list.innerHTML = items.map(function (notification) {
                return '<li><div class="dropdown-item d-flex justify-content-between py-2 border-bottom">'
                    + '<span class="fw-bold">' + escapeHtml(notification.aquariumName || '-') + ':</span>'
                    + '<span class="text-muted small ms-2">'
                    + escapeHtml(notification.parameter || '-') + ': '
                    + escapeHtml(notification.value || '-') + '</span></div></li>';
            }).join('');
            wrapper.classList.remove('d-none');
        })
        .catch(function (error) {
            console.error('No se pudieron cargar las notificaciones', error);
        });

    function escapeHtml(value) {
        return String(value).replace(/[&<>"']/g, function (character) {
            return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[character];
        });
    }

    reloadAquariums().catch(function (error) {
        console.error('No se pudieron cargar los acuarios', error);
    });
});
