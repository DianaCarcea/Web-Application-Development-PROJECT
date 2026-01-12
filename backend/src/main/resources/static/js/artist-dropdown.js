document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("artistForm");
    const select = document.getElementById("artistSelect");

    const domainInput = document.getElementById("domainInput");

    form.addEventListener("submit", function (e) {
        e.preventDefault();

        const artistId = select.value;
        if (!artistId) return;

        const domainValue = domainInput ? domainInput.value : 'ro';

        window.location.href = `/artists/${artistId}/artworks?domain=${domainValue}`;
    });
});