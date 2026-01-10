// arp-query.js

async function loadPredefinedQueries() {
    const response = await fetch("/predefined-queries");
    const queries = await response.json();
    const container = document.getElementById("query-buttons");

    const order = [
        "artworks",
        "artists",
        "artworks_all_details",
        "artworks_count_by_material",
        "artworks_by_getty_material",
        "artworks_count_by_category",
        "artworks_by_getty_category"
    ];

    const labelsMain = {
        artworks:"List all artworks",
        artists: "List all artists",
        artworks_all_details:"All details for artworks",
        artworks_count_by_material:"Count Artworks by Material",
        artworks_by_getty_material:"Artworks by Material (Getty AAT)",
        artworks_count_by_category:"Count Artworks by Categories",
        artworks_by_getty_category:"Artworks by Category (Getty AAT)",
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

    // Arată loader-ul și golește rezultatul anterior
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
        // Ascunde loader-ul după ce rezultatul a venit
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