# ArP – Artwork Provenance

**Team:** ArtSynergy  
**Members:** Carcea Diana, Carcea Răzvan  
**Course:** WADe (Web Application Development)

## 📖 Project Overview
**ArP (Artwork Provenance)** is a semantic web platform designed for modeling, managing, and visualizing the provenance of artistic works. It transitions national heritage data from legacy XML formats into a dynamic **Knowledge Graph**.

The platform integrates Linked Open Data sources—primarily **Wikidata** and **Getty Vocabularies (AAT, ULAN, TGN)**—to enrich local records (CIMEC) with global context. It exposes a dual interface: a human-friendly web catalog and a machine-readable Semantic API.

Demo for application : https://www.youtube.com/watch?v=LJwkH9yU0rY

## 🚀 Key Features
* **Semantic Data Integration:** ETL pipeline transforming LIDO XML to RDF (PROV-O compliant).
* **Hybrid Architecture:** Serves both HTML (Thymeleaf) and JSON/Linked Data.
* **SPARQL Endpoint:** Full support for executing `SELECT` queries against the dataset.
* **Knowledge Reconciliation:** Automatic linking of local artists/museums to Wikidata entities.
* **Visual Discovery:** Filtering by domain (`ro` vs `int`), category, and museum.

## 🛠️ Technologies
- **Backend Framework:** Java 21, Spring Boot 3.x
- **Semantic Engine:** Apache Jena TDB (Embedded Triple Store)
- **Frontend:** HTML5, Thymeleaf, JavaScript (Vanilla), RDFa, schema.org  
- **API Documentation:** OpenAPI 3.0 (Swagger UI)
- **Build Tool:** Maven

## 📚 Documentation & Wiki
All project documentation, including the **System Architecture (C4 Models)**, **Technical Report**, and **User Guide**, is available in the **[Project Wiki](https://github.com/DianaCarcea/Web-Application-Development-PROJECT/wiki)**.

## 💻 Getting Started

### Prerequisites
* Java 21 SDK
* Maven 3.8+

### Installation & Execution
1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/DianaCarcea/Web-Application-Development-PROJECT.git](https://github.com/DianaCarcea/Web-Application-Development-PROJECT.git)
    ```
2.  **Navigate to the project directory:**
    ```bash
    cd Web-Application-Development-PROJECT
    ```
3.  **Run the application:**
    You can run the project directly using the main class from your IDE (IntelliJ/Eclipse) or via command line:
    * **Main Class:** `src/main/java/com/example/backend/BackendApplication.java`
    * **Maven Command:** `mvn spring-boot:run`

4.  **Access the application:**
    * **Web Interface:** `http://localhost:8080`
    * **Swagger UI (API Docs):** `http://localhost:8080/swagger-ui.html`

## 📄 License

### Source Code
The source code of this project is released under the **MIT License** ([Open Source Initiative](https://opensource.org/licenses)), allowing anyone to use, modify, and distribute it while giving proper credit to the authors.

### Data and Content
All data, documentation, and multimedia content provided in this project are shared under the **Creative Commons Attribution 4.0 International (CC BY 4.0)** ([Creative Commons](https://creativecommons.org/share-your-work/cclicenses/)) license.

### External Data Sources
Some data is retrieved from external Linked Open Data sources, which are subject to their own licenses:
- **[Wikidata](https://www.wikidata.org/)** – CC0 1.0
- **[Getty Vocabularies](https://www.getty.edu/research/tools/vocabularies/)** – ODC-BY 1.0
- **[Europeana](https://www.europeana.eu/)** – CC0 / CC BY-SA (depending on dataset)
