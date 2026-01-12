
function downloadTTL() {
    window.location.href = "/download/ttl";
}

document.addEventListener("DOMContentLoaded", () => {
    loadTTLFiles();

    const select = document.getElementById("ttlSelect");
    const previewBtn = document.getElementById("previewTTLBtn");

    select.addEventListener("change", () => {
        previewBtn.disabled = !select.value;
    });

    previewBtn.addEventListener("click", () => {
        const file = select.value;
        if (file) {
            window.open(`/ttl/preview/${file}`, "_blank");
        }
    });
});

function loadTTLFiles() {
    fetch("/ttl/list")
        .then(res => res.json())
        .then(files => {
            const select = document.getElementById("ttlSelect");

            files.forEach(file => {
                const opt = document.createElement("option");
                opt.value = file;
                opt.textContent = file;
                select.appendChild(opt);
            });
        })
        .catch(err => console.error("Error loading TTL files", err));
}



async function loadPredefinedQueries() {
    const response = await fetch("/predefined-queries");
    const queries = await response.json();
    const container = document.getElementById("query-buttons");

    const order = [
        "artworks",
        "artists",
        "museums",
        "artworks_all_details",
        "artworks_count_by_material",
        "artworks_by_getty_material",
        "artworks_count_by_category",
        "artworks_by_getty_category",
        "artworks_count_by_museum",
        "artworks_by_getty_museum",
        "artworks_count_by_artist",
        "artworks_by_getty_artist"


    ];

    const labelsMain = {
        artworks:"List all artworks",
        artists: "List all artists",
        museums:"List all museums",
        artworks_all_details:"All details for artworks",
        artworks_count_by_material:"Count Artworks by Material",
        artworks_by_getty_material:"Artworks by Material (Getty AAT)",
        artworks_count_by_category:"Count Artworks by Categories",
        artworks_by_getty_category:"Artworks by Category (Getty AAT)",
        artworks_count_by_museum:"Count Artworks by Museum",
        artworks_by_getty_museum:"Artworks by Museum (Getty AAT)",
        artworks_count_by_artist:"Count Artworks by Artist",
        artworks_by_getty_artist:"Artworks by Artist (Getty AAT)",
    };

    order.forEach(key => {
        if (queries[key]) {
            const btn = document.createElement("button");
            btn.textContent = labelsMain[key] || key;
            btn.onclick = () => {
                clearResult();
                document.getElementById("query").value = queries[key];
            };
            container.appendChild(btn);
        }
    });
}

loadPredefinedQueries();

async function runQuery() {
    const query = document.getElementById("query").value;
    const loader = document.getElementById("loader");
    const resultContainer = document.getElementById("result");

    loader.style.display = "block";
    resultContainer.innerHTML = "";

    try {
        const response = await fetch("/sparql", {
            method: "POST",
            headers: {
                "Content-Type": "text/plain",
                "Accept": "application/sparql-results+json"
            },
            body: query
        });

        const json = await response.json();
        renderTable(json);
    } catch (err) {
        resultContainer.innerHTML = "<p style='color:red'>Error: " + err.message + "</p>";
    } finally {
        loader.style.display = "none";
    }
}

function renderTable(data) {
    const vars = data.head.vars;
    const bindings = data.results.bindings;

    const container = document.getElementById("result");
    if (bindings.length === 0) {
        container.innerHTML = "<p>No results.</p>";
        return;
    }

    let html = "<table><thead><tr>";
    vars.forEach(v => html += `<th>${v}</th>`);
    html += "</tr></thead><tbody>";

    bindings.forEach(row => {
        html += "<tr>";
        vars.forEach(v => {
            if (row[v]) {
                const cell = row[v];
                if (cell.type === "uri") {
                    html += `<td class="uri"><a href="${cell.value}" target="_blank">${cell.value}</a></td>`;
                } else {
                    html += `<td>${cell.value}</td>`;
                }
            } else {
                html += "<td></td>";
            }
        });
        html += "</tr>";
    });

    html += "</tbody></table>";
    container.innerHTML = html;
}
function clearResult() {
    const resultContainer = document.getElementById("result");
    const loader = document.getElementById("loader");

    resultContainer.innerHTML = "";
    if (loader) {
        loader.style.display = "none";
    }
}