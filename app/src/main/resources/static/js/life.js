export function initializeLifeManagement(apiUrl, labels) {
    var listModalElement = document.getElementById('life-list-modal');
    var createModalElement = document.getElementById('life-create-modal');
    var listModal = bootstrap.Modal.getOrCreateInstance(listModalElement);
    var createModal = bootstrap.Modal.getOrCreateInstance(createModalElement);
    var lifeList = document.getElementById('life-list');
    var listError = document.getElementById('life-list-error');
    var createForm = document.getElementById('life-create-form');
    var createError = document.getElementById('life-create-error');
    var dateInput = document.getElementById('life-date');

    function localDateString(date) {
        return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0')
            + '-' + String(date.getDate()).padStart(2, '0');
    }

    function showError(element, error, fallback) {
        element.textContent = error.message || fallback;
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

    function addText(parent, className, value) {
        var text = document.createElement('p');
        text.className = className;
        text.textContent = value;
        parent.appendChild(text);
    }

    function renderLife(entries) {
        lifeList.replaceChildren();
        if (entries.length === 0) {
            var empty = document.createElement('p');
            empty.className = 'text-muted mb-0';
            empty.textContent = labels.empty;
            lifeList.appendChild(empty);
            return;
        }

        entries.forEach(function (life) {
            var column = document.createElement('div');
            column.className = 'col-12 col-md-6';
            var card = document.createElement('article');
            card.className = 'card h-100 shadow-sm';
            var body = document.createElement('div');
            body.className = 'card-body d-flex flex-column';

            var heading = document.createElement('h3');
            heading.className = 'h5 card-title';
            if (life.category === 'fish') {
                var fishIcon = document.createElement('span');
                fishIcon.className = 'me-2 text-info';
                fishIcon.setAttribute('aria-hidden', 'true');
                fishIcon.textContent = '🐟';
                heading.appendChild(fishIcon);
            } else {
                var plantIcon = document.createElement('i');
                plantIcon.className = 'bi bi-flower1 me-2 text-success';
                plantIcon.setAttribute('aria-hidden', 'true');
                heading.appendChild(plantIcon);
            }
            heading.appendChild(document.createTextNode(life.commonName));
            body.appendChild(heading);

            addText(body, 'small text-muted mb-2', labels[life.category] || life.category);
            if (life.scientificName) {
                addText(body, 'mb-2', labels.scientificName + ': ' + life.scientificName);
            }
            if (life.recommendedParameters) {
                addText(body, 'small mb-2', labels.recommendedParameters + ': ' + life.recommendedParameters);
            }
            if (life.notes) {
                addText(body, 'small mb-2', labels.notes + ': ' + life.notes);
            }
            if (life.technicalSheetUrl) {
                var link = document.createElement('a');
                link.href = life.technicalSheetUrl;
                link.target = '_blank';
                link.rel = 'noopener noreferrer';
                link.textContent = labels.technicalSheetUrl;
                link.className = 'small mb-3';
                body.appendChild(link);
            }

            var quantityRow = document.createElement('div');
            quantityRow.className = 'd-flex align-items-center justify-content-between gap-2 mt-auto pt-3';
            var quantity = document.createElement('strong');
            quantity.className = 'life-quantity';
            quantity.textContent = labels.quantity + ': ' + life.quantity;
            var controls = document.createElement('div');
            controls.className = 'btn-group';
            controls.setAttribute('role', 'group');
            controls.setAttribute('aria-label', labels.quantityLabel);
            var removeButton = quantityButton(life.id, -1, labels.removeOne, 'btn-outline-danger');
            removeButton.disabled = life.quantity === 0;
            controls.appendChild(removeButton);
            controls.appendChild(quantityButton(life.id, 1, labels.addOne, 'btn-outline-success'));
            quantityRow.append(quantity, controls);
            body.appendChild(quantityRow);
            card.appendChild(body);
            column.appendChild(card);
            lifeList.appendChild(column);
        });
    }

    function quantityButton(id, change, label, style) {
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'btn ' + style;
        button.dataset.lifeId = id;
        button.dataset.change = change;
        button.setAttribute('aria-label', label);
        button.title = label;
        button.textContent = change > 0 ? '+' : '−';
        return button;
    }

    function reloadLife() {
        listError.classList.add('d-none');
        lifeList.replaceChildren();
        var loading = document.createElement('p');
        loading.className = 'text-muted mb-0';
        loading.textContent = labels.loading;
        lifeList.appendChild(loading);
        return request(apiUrl('/api/life'), 'GET')
            .then(renderLife)
            .catch(function (error) {
                lifeList.replaceChildren();
                showError(listError, error, labels.loadError);
            });
    }

    function openCreate(date) {
        createForm.reset();
        dateInput.value = date || localDateString(new Date());
        createError.classList.add('d-none');
        createModal.show();
    }

    document.getElementById('life-open-button').addEventListener('click', function () {
        listModal.show();
        reloadLife();
    });

    document.getElementById('life-open-create').addEventListener('click', function () {
        listModalElement.addEventListener('hidden.bs.modal', function () {
            openCreate();
        }, {once: true});
        listModal.hide();
    });

    createForm.addEventListener('submit', function (event) {
        event.preventDefault();
        createError.classList.add('d-none');
        var values = new FormData(createForm);
        var data = {
            category: values.get('category'),
            commonName: values.get('commonName'),
            scientificName: values.get('scientificName') || null,
            quantity: Number(values.get('quantity')),
            recommendedParameters: values.get('recommendedParameters') || null,
            technicalSheetUrl: values.get('technicalSheetUrl') || null,
            notes: values.get('notes') || null,
            date: values.get('date')
        };
        request(apiUrl('/api/life'), 'POST', data)
            .then(function () {
                createModalElement.addEventListener('hidden.bs.modal', function () {
                    listModal.show();
                    reloadLife();
                }, {once: true});
                createModal.hide();
            })
            .catch(function (error) {
                showError(createError, error, labels.saveError);
            });
    });

    lifeList.addEventListener('click', function (event) {
        var button = event.target.closest('button[data-life-id]');
        if (!button) return;
        button.disabled = true;
        request(apiUrl('/api/life/' + encodeURIComponent(button.dataset.lifeId) + '/quantity'),
            'PATCH', {change: Number(button.dataset.change)})
            .then(reloadLife)
            .catch(function (error) {
                showError(listError, error, labels.quantityError);
                button.disabled = false;
            });
    });

    return openCreate;
}
