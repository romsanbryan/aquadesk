export function createLastPhotosLoader(apiUrl, i18n) {
    function loadLastPhotos() {
        var container = document.getElementById('last-photos-inner');
        var prevBtn = document.getElementById('last-photos-prev');
        var nextBtn = document.getElementById('last-photos-next');
        var counterEl = document.getElementById('last-photos-counter');
        if (!container || !prevBtn || !nextBtn || !counterEl) return;

        function updateControls(total) {
            counterEl.innerText = '1 / ' + total;
        }

        fetch(apiUrl('/api/photos/last?limit=10'))
            .then(function (res) {
                if (!res.ok) throw new Error(i18n.error.loadPhotos + ': ' + res.status);
                return res.json();
            })
            .then(function (data) {
                if (!data || data.length === 0) {
                    container.innerHTML = '<div class="text-muted small p-2">' + i18n.label.noPhotos + '</div>';
                    updateControls(0);
                    prevBtn.classList.add('disabled');
                    nextBtn.classList.add('disabled');
                    return;
                }

                container.innerHTML = data.map(function (item, idx) {
                    return '<div class="carousel-item ' + (idx === 0 ? 'active' : '') + '" data-photo-id="' + item.id + '">'
                        + '<img src="' + apiUrl('/api/photos/' + item.id + '/image') + '" class="d-block mx-auto img-fluid rounded" style="max-height:200px; object-fit:cover;" alt="Foto ' + (idx + 1) + '">'
                        + '</div>';
                }).join('');

                var carouselEl = document.getElementById('lastPhotosCarousel');
                bootstrap.Carousel.getOrCreateInstance(carouselEl, {interval: false});

                function actualizarContador() {
                    var items = container.querySelectorAll('.carousel-item');
                    var active = container.querySelector('.carousel-item.active');
                    var idx = Array.from(items).indexOf(active);
                    counterEl.innerText = (idx + 1) + ' / ' + items.length;
                    if (idx === items.length - 1) nextBtn.classList.add('disabled'); else nextBtn.classList.remove('disabled');
                    if (idx === 0) prevBtn.classList.add('disabled'); else prevBtn.classList.remove('disabled');
                }

                if (carouselEl._updatePhotoCounter) {
                    carouselEl.removeEventListener('slid.bs.carousel', carouselEl._updatePhotoCounter);
                }
                carouselEl._updatePhotoCounter = actualizarContador;
                carouselEl.addEventListener('slid.bs.carousel', carouselEl._updatePhotoCounter);
                actualizarContador();

                updateControls(data.length);
            })
            .catch(function (err) {
                container.innerHTML = '<div class="text-danger small p-2">' + i18n.error.loadPhotos + ': ' + err.message + '</div>';
                updateControls(0);
            });
    }

    return loadLastPhotos;

}
