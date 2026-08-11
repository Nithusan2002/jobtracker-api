const API_URL = "/api/applications";

const statusLabels = {
	SENT: "Sendt",
	INTERVIEW: "Intervju",
	REJECTED: "Avslag",
	OFFER: "Tilbud",
};

let applications = [];

const elements = {
	table: document.querySelector("#applicationsTable"),
	emptyState: document.querySelector("#emptyState"),
	statusText: document.querySelector("#statusText"),
	totalCount: document.querySelector("#totalCount"),
	activeCount: document.querySelector("#activeCount"),
	searchInput: document.querySelector("#searchInput"),
	statusFilter: document.querySelector("#statusFilter"),
	form: document.querySelector("#applicationForm"),
	formTitle: document.querySelector("#formTitle"),
	formError: document.querySelector("#formError"),
	submitButton: document.querySelector("#submitButton"),
	resetButton: document.querySelector("#resetButton"),
	fields: {
		id: document.querySelector("#applicationId"),
		companyName: document.querySelector("#companyName"),
		jobTitle: document.querySelector("#jobTitle"),
		applicationDate: document.querySelector("#applicationDate"),
		status: document.querySelector("#status"),
		jobListingUrl: document.querySelector("#jobListingUrl"),
		notes: document.querySelector("#notes"),
	},
};

function escapeHtml(value) {
	return String(value ?? "")
		.replaceAll("&", "&amp;")
		.replaceAll("<", "&lt;")
		.replaceAll(">", "&gt;")
		.replaceAll('"', "&quot;")
		.replaceAll("'", "&#039;");
}

function formatDate(value) {
	if (!value) return "";
	return new Intl.DateTimeFormat("no-NO", {
		day: "2-digit",
		month: "short",
		year: "numeric",
	}).format(new Date(`${value}T00:00:00`));
}

function getFilteredApplications() {
	const query = elements.searchInput.value.trim().toLowerCase();
	const status = elements.statusFilter.value;

	return applications.filter((application) => {
		const matchesQuery =
			!query ||
			application.companyName.toLowerCase().includes(query) ||
			application.jobTitle.toLowerCase().includes(query);
		const matchesStatus = status === "ALL" || application.status === status;
		return matchesQuery && matchesStatus;
	});
}

function updateSummary() {
	const active = applications.filter((application) =>
		["SENT", "INTERVIEW"].includes(application.status),
	).length;

	elements.totalCount.textContent = `${applications.length} søknader`;
	elements.activeCount.textContent = `${active} aktive`;
}

function renderApplications() {
	const filtered = getFilteredApplications();
	elements.table.innerHTML = "";
	elements.emptyState.hidden = filtered.length > 0;

	filtered.forEach((application) => {
		const row = document.createElement("tr");
		const link = application.jobListingUrl
			? `<a class="link" href="${escapeHtml(application.jobListingUrl)}" target="_blank" rel="noreferrer">Åpne annonse</a>`
			: "";

		row.innerHTML = `
			<td>
				<div class="companyCell">${escapeHtml(application.companyName)}</div>
				${link}
			</td>
			<td>${escapeHtml(application.jobTitle)}</td>
			<td>${formatDate(application.applicationDate)}</td>
			<td><span class="statusPill status-${application.status}">${statusLabels[application.status]}</span></td>
			<td class="notesCell">${escapeHtml(application.notes || "")}</td>
			<td>
				<div class="rowActions">
					<button class="smallButton" type="button" data-action="edit" data-id="${application.id}">Rediger</button>
					<button class="smallButton danger" type="button" data-action="delete" data-id="${application.id}">Slett</button>
				</div>
			</td>
		`;

		elements.table.append(row);
	});

	elements.statusText.textContent =
		filtered.length === applications.length
			? "Viser alle registrerte søknader."
			: `Viser ${filtered.length} av ${applications.length} søknader.`;
	updateSummary();
}

function setFormError(message) {
	elements.formError.textContent = message;
	elements.formError.hidden = !message;
}

function resetForm() {
	elements.form.reset();
	elements.fields.id.value = "";
	elements.fields.applicationDate.value = new Date().toISOString().slice(0, 10);
	elements.fields.status.value = "SENT";
	elements.formTitle.textContent = "Ny søknad";
	elements.submitButton.textContent = "Lagre søknad";
	setFormError("");
}

function fillForm(application) {
	elements.fields.id.value = application.id;
	elements.fields.companyName.value = application.companyName;
	elements.fields.jobTitle.value = application.jobTitle;
	elements.fields.applicationDate.value = application.applicationDate;
	elements.fields.status.value = application.status;
	elements.fields.jobListingUrl.value = application.jobListingUrl || "";
	elements.fields.notes.value = application.notes || "";
	elements.formTitle.textContent = "Rediger søknad";
	elements.submitButton.textContent = "Oppdater søknad";
	setFormError("");
	elements.fields.companyName.focus();
}

function getPayload() {
	return {
		companyName: elements.fields.companyName.value.trim(),
		jobTitle: elements.fields.jobTitle.value.trim(),
		applicationDate: elements.fields.applicationDate.value,
		status: elements.fields.status.value,
		jobListingUrl: elements.fields.jobListingUrl.value.trim() || null,
		notes: elements.fields.notes.value.trim() || null,
	};
}

async function requestJson(url, options = {}) {
	const response = await fetch(url, {
		headers: { "Content-Type": "application/json" },
		...options,
	});

	if (!response.ok) {
		const error = await response.json().catch(() => ({}));
		throw new Error(error.message || "Noe gikk galt.");
	}

	if (response.status === 204) return null;
	return response.json();
}

async function loadApplications() {
	elements.statusText.textContent = "Laster inn...";
	applications = await requestJson(API_URL);
	renderApplications();
}

async function saveApplication(event) {
	event.preventDefault();
	setFormError("");
	elements.submitButton.disabled = true;

	const id = elements.fields.id.value;
	const payload = getPayload();
	const method = id ? "PUT" : "POST";
	const url = id ? `${API_URL}/${id}` : API_URL;

	try {
		await requestJson(url, {
			method,
			body: JSON.stringify(payload),
		});
		resetForm();
		await loadApplications();
	} catch (error) {
		setFormError(error.message);
	} finally {
		elements.submitButton.disabled = false;
	}
}

async function handleTableClick(event) {
	const button = event.target.closest("button[data-action]");
	if (!button) return;

	const id = Number(button.dataset.id);
	const application = applications.find((item) => item.id === id);
	if (!application) return;

	if (button.dataset.action === "edit") {
		fillForm(application);
		return;
	}

	if (button.dataset.action === "delete") {
		await requestJson(`${API_URL}/${id}`, { method: "DELETE" });
		await loadApplications();
		if (elements.fields.id.value === String(id)) resetForm();
	}
}

elements.form.addEventListener("submit", saveApplication);
elements.resetButton.addEventListener("click", resetForm);
elements.table.addEventListener("click", handleTableClick);
elements.searchInput.addEventListener("input", renderApplications);
elements.statusFilter.addEventListener("change", renderApplications);

resetForm();
loadApplications().catch((error) => {
	elements.statusText.textContent = "Kunne ikke laste søknader.";
	setFormError(error.message);
});
