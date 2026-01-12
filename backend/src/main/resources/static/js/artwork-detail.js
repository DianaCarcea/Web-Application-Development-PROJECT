document.addEventListener("DOMContentLoaded", function() {
    initWrapperState('rec-wrapper-artist');
    initWrapperState('rec-wrapper-museum');
    initWrapperState('rec-wrapper-category');
});

function initWrapperState(wrapperId) {
    const wrapper = document.getElementById(wrapperId);
    if (!wrapper) return;

    const limit = parseInt(wrapper.getAttribute('data-limit'));
    const nextBtn = wrapper.querySelector('.next-btn');
    const grid = wrapper.querySelector('.rec-grid');

    if (grid) {
        const itemsCount = grid.querySelectorAll('.rec-card').length;
        if (nextBtn && itemsCount < limit) {
            nextBtn.disabled = true;
        }
    }
}

function changeRecommendationPage(direction, wrapperId, type) {
    const wrapper = document.getElementById(wrapperId);
    if (!wrapper) return;

    const uri = wrapper.getAttribute('data-uri');
    const domain = wrapper.getAttribute('data-domain');
    const limit = parseInt(wrapper.getAttribute('data-limit'));
    let currentOffset = parseInt(wrapper.getAttribute('data-offset'));

    let newOffset = currentOffset + (direction * limit);
    if (newOffset < 0) newOffset = 0;

    const url = `/artworks/recommendations-fragment?uri=${encodeURIComponent(uri)}&offset=${newOffset}&limit=${limit}&domain=${domain}`;

    fetch(url)
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.text();
        })
        .then(html => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, "text/html");

            let newContent = "";

            if (type === 'artist') {
                const source = doc.getElementById('ajax-source-artist');
                if (source) newContent = source.innerHTML;
            } else if (type === 'museum') {
                const source = doc.getElementById('ajax-source-museum');
                if (source) newContent = source.innerHTML;
            } else if (type === 'category') {
                const source = doc.getElementById('ajax-source-category');
                if (source) newContent = source.innerHTML;
            }

            const oldGrid = wrapper.querySelector('.rec-grid');
            if (oldGrid && newContent) {
                oldGrid.outerHTML = newContent;
            }

            wrapper.setAttribute('data-offset', newOffset);

            const prevBtn = wrapper.querySelector('.prev-btn');
            const nextBtn = wrapper.querySelector('.next-btn');
            const newGrid = wrapper.querySelector('.rec-grid');

            if (prevBtn) prevBtn.disabled = (newOffset === 0);

            if (newGrid && nextBtn) {
                const itemsCount = newGrid.querySelectorAll('.rec-card').length;
                nextBtn.disabled = (itemsCount < limit);
            }
        })
        .catch(error => console.error('Error:', error));
}
