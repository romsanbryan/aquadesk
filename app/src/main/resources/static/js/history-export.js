export function initializeHistoryExport(aquariumId, i18n) {
    var historyExportForm = document.getElementById('history-export-form');
    if (historyExportForm) {
        var allHistoryInput = document.getElementById('history-export-all');
        var startDateInput = document.getElementById('history-export-start');
        var endDateInput = document.getElementById('history-export-end');
        var exportFeedback = document.getElementById('history-export-feedback');
        var exportSubmit = document.getElementById('history-export-submit');
        var exportSpinner = document.getElementById('history-export-spinner');
        var exportDownloadLabel = document.getElementById('history-export-download-label');
        var defaultDownloadLabel = exportDownloadLabel.innerText;

        function syncHistoryDateInputs() {
            var allHistory = allHistoryInput.checked;
            startDateInput.disabled = allHistory;
            endDateInput.disabled = allHistory;
            startDateInput.required = !allHistory;
            endDateInput.required = !allHistory;
        }

        allHistoryInput.addEventListener('change', syncHistoryDateInputs);
        startDateInput.addEventListener('change', function () {
            endDateInput.min = startDateInput.value;
        });
        endDateInput.addEventListener('change', function () {
            startDateInput.max = endDateInput.value;
        });
        syncHistoryDateInputs();

        historyExportForm.addEventListener('submit', function (event) {
            event.preventDefault();
            exportFeedback.classList.add('d-none');

            var selectedSections = Array.from(
                historyExportForm.querySelectorAll('.history-export-section:checked')
            ).map(function (input) { return input.value; });
            if (selectedSections.length === 0) {
                exportFeedback.innerText = i18n.historyExport.selectSection;
                exportFeedback.classList.remove('d-none');
                return;
            }
            if (!allHistoryInput.checked && (!startDateInput.value || !endDateInput.value)) {
                exportFeedback.innerText = i18n.historyExport.dateRangeRequired;
                exportFeedback.classList.remove('d-none');
                return;
            }
            if (!allHistoryInput.checked && endDateInput.value < startDateInput.value) {
                exportFeedback.innerText = i18n.historyExport.invalidDateRange;
                exportFeedback.classList.remove('d-none');
                return;
            }
            if (!historyExportForm.reportValidity()) return;

            var format = historyExportForm.querySelector('input[name="format"]:checked').value;
            var query = new URLSearchParams();
            query.set('format', format);
            query.set('allHistory', String(allHistoryInput.checked));
            selectedSections.forEach(function (section) { query.append('sections', section); });
            if (!allHistoryInput.checked) {
                query.set('startDate', startDateInput.value);
                query.set('endDate', endDateInput.value);
            }

            exportSubmit.disabled = true;
            exportSpinner.classList.remove('d-none');
            exportDownloadLabel.innerText = i18n.historyExport.downloading;
            fetch('/api/aquariums/' + encodeURIComponent(aquariumId)
                + '/history/export?' + query.toString())
                .then(function (response) {
                    if (!response.ok) {
                        return response.json().then(function (error) {
                            throw new Error(error.message || i18n.historyExport.downloadError);
                        }).catch(function (error) {
                            if (error.message) throw error;
                            throw new Error(i18n.historyExport.downloadError);
                        });
                    }
                    return response.blob();
                })
                .then(function (blob) {
                    var link = document.createElement('a');
                    link.href = URL.createObjectURL(blob);
                    link.download = 'aquadesk-historial.' + format;
                    document.body.appendChild(link);
                    link.click();
                    link.remove();
                    window.setTimeout(function () { URL.revokeObjectURL(link.href); }, 1000);
                    bootstrap.Modal.getInstance(document.getElementById('history-export-modal')).hide();
                })
                .catch(function (error) {
                    exportFeedback.innerText = error.message || i18n.historyExport.downloadError;
                    exportFeedback.classList.remove('d-none');
                })
                .finally(function () {
                    exportSubmit.disabled = false;
                    exportSpinner.classList.add('d-none');
                    exportDownloadLabel.innerText = defaultDownloadLabel;
                });
        });
    }
}
