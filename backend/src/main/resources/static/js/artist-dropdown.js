document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("artistForm");
    const select = document.getElementById("artistSelect");

    form.addEventListener("submit", function (e) {
        e.preventDefault(); // prevent default form submission

        const artistId = select.value;
        if (!artistId) return; // do nothing if no artist selected

        // Redirect to artists/{id}/artworks
        window.location.href = `/artists/${artistId}/artworks`;
    });
});
