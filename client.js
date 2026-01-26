
// ===== State =====
let clients = [];     // loaded from server
let editingId = null; // entity id
let deletingId = null;
let dt = null;

// ---- Optional: read context path from Thymeleaf (uncomment if you added the meta in layout) ----
// In your layout or page (BEFORE loading this file), you can add:
// <script th:inline="javascript">window.APP_BASE = /*[[${#httpServletRequest.contextPath}]]*/ '';</script>
// Then set BASE like this:
// const BASE = (typeof window.APP_BASE === 'string') ? window.APP_BASE : '';
// For RELATIVE URLs we don't need BASE. We'll use relative paths: 'api', `api/${id}`.

// ---- CSRF (optional, if Spring Security enabled) ----
const CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;
function withCsrf(headers = {}) {
  if (CSRF_TOKEN && CSRF_HEADER) headers[CSRF_HEADER] = CSRF_TOKEN;
  return headers;
}

// ---------- Fetch helpers ----------
async function apiGet(url) {
  // Using RELATIVE path: 'api' will resolve to '/<context>/client/api' automatically
  const res = await fetch(url, { headers: { 'Accept': 'application/json' } });
  if (!res.ok) throw new Error(`GET ${url} -> ${res.status}`);
  return await res.json();
}
async function apiPost(url, body) {
  const res = await fetch(url, {
    method: 'POST',
    headers: withCsrf({
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    }),
    body: JSON.stringify(body)
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`POST ${url} -> ${res.status} ${text}`);
  }
  return await res.json();
}
async function apiPut(url, body) {
  const res = await fetch(url, {
    method: 'PUT',
    headers: withCsrf({
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    }),
    body: JSON.stringify(body)
  });
  if (!res.ok) throw new Error(`PUT ${url} -> ${res.status}`);
  return await res.json();
}
async function apiDelete(url) {
  const res = await fetch(url, {
    method: 'DELETE',
    headers: withCsrf({ 'Accept': 'application/json' })
  });
  if (!res.ok && res.status !== 204) throw new Error(`DELETE ${url} -> ${res.status}`);
}

// ---------- Utils ----------
function toSafe(s) { return s ?? ''; }
function updateTotal() {
  // Count rows after DataTables' current search/filter
  const total = dt ? dt.rows({ search: 'applied' }).count() : 0;
  const el = document.getElementById("totalRows");
  if (el) el.textContent = String(total);
}

// ---------- DataTable ----------
function initDataTable() {
  dt = new DataTable('#clientTable', {
    data: clients,
    responsive: true,
    autoWidth: false,
    searching: true,      // use DataTables built-in search
    lengthChange: true,
    pageLength: 10,
    order: [[0, 'asc']],
    columns: [
      { data: 'code',   className: 'fw-semibold' },
      { data: 'service' },
      { data: 'nameTh' },
      { data: 'nameEn' },
      { data: 'createdBy' },
      { data: 'createdAt' },
      { data: 'updatedBy' },
      { data: 'updatedAt' },
      {
        data: null,
        orderable: false,
        className: 'text-center',
        render: (data, type, row) => `
          <button class="btn btn-outline-secondary btn-icon me-2 btn-edit"
                  type="button" title="Edit" data-id="${row.id}">
            <i class="fa-solid fa-pen"></i>
          </button>
          <button class="btn btn-danger btn-icon btn-delete"
                  type="button" title="Delete" data-id="${row.id}">
            <i class="fa-solid fa-trash"></i>
          </button>`
      }
    ]
  });

  // Update "Total Rows" whenever table is drawn (paging/search changes)
  $('#clientTable').on('draw.dt', updateTotal);

  // Delegated click handlers
  $('#clientTable tbody').on('click', 'button.btn-edit', function () {
    const id = Number(this.getAttribute('data-id'));
    const row = clients.find(x => x.id === id);
    if (!row) return;

    editingId = id;
    document.getElementById("editCode").value = row.code ?? '';
    document.getElementById("editService").value = row.service ?? '';
    document.getElementById("editNameTh").value = row.nameTh ?? '';
    document.getElementById("editNameEn").value = row.nameEn ?? '';
    hideError("editError");

    coreui.Modal.getOrCreateInstance(document.getElementById("modalEdit")).show();
  });

  $('#clientTable tbody').on('click', 'button.btn-delete', function () {
    const id = Number(this.getAttribute('data-id'));
    const row = clients.find(x => x.id === id);
    if (!row) return;

    deletingId = id;
    document.getElementById("delCode").textContent = row.code ?? '';
    document.getElementById("delName").textContent = `${row.nameTh ?? ''} / ${row.nameEn ?? ''}`;
    coreui.Modal.getOrCreateInstance(document.getElementById("modalDelete")).show();
  });

  updateTotal(); // initial count
}

function redrawTable() {
  if (!dt) return;
  dt.clear();
  dt.rows.add(clients);
  dt.draw(false);
}

// ---------- Error helpers ----------
function showError(id, msg) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = msg;
  el.classList.remove("d-none");
}
function hideError(id) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = "";
  el.classList.add("d-none");
}

// ---------- DOM Ready ----------
document.addEventListener("DOMContentLoaded", async () => {
  try {
    // RELATIVE path: resolves to /<context>/client/api (e.g., /fdu/client/api)
    const data = await apiGet('api');

    // Normalize for DataTable
    clients = data.map(c => ({
      id: c.id,
      code: toSafe(c.code),
      service: toSafe(c.service),
      nameTh: toSafe(c.nameTh),
      nameEn: toSafe(c.nameEn),
      createdBy: c.createdBy ?? '',
      createdAt: c.createdAt ?? '',
      updatedBy: c.updatedBy ?? '',
      updatedAt: c.updatedAt ?? ''
    }));

    initDataTable();
  } catch (e) {
    console.error('Failed to load clients:', e);
  }

  // Add
  document.getElementById("btnAddConfirm").addEventListener("click", async () => {
    const code = (document.getElementById("addCode").value || "").trim();
    const service = (document.getElementById("addService").value || "").trim();
    const nameTh = (document.getElementById("addNameTh").value || "").trim();
    const nameEn = (document.getElementById("addNameEn").value || "").trim();

    if (!code || !service || !nameTh || !nameEn) {
      showError("addError", "Please enter Code, Service, Name (Thai) and Name (Eng).");
      return;
    }

    try {
      // RELATIVE path
      const created = await apiPost('api', {
        code, service, nameTh, nameEn,
        createdBy: 1, // TODO: replace with logged-in user id
        updatedBy: 1
      });

      clients.unshift(created);
      redrawTable();

      // reset + close
      document.getElementById("addCode").value = "";
      document.getElementById("addService").value = "";
      document.getElementById("addNameTh").value = "";
      document.getElementById("addNameEn").value = "";
      hideError("addError");
      coreui.Modal.getInstance(document.getElementById("modalAdd"))?.hide();

    } catch (err) {
      console.error(err);
      const msg = String(err?.message || '').includes('409')
        ? 'Duplicate (Code + Service). Please use another.'
        : 'Create failed. Please try again.';
      showError("addError", msg);
    }
  });

  // Edit
  document.getElementById("btnEditConfirm").addEventListener("click", async () => {
    if (editingId == null) return;

    const service = (document.getElementById("editService").value || "").trim();
    const nameTh  = (document.getElementById("editNameTh").value || "").trim();
    const nameEn  = (document.getElementById("editNameEn").value || "").trim();

    if (!service || !nameTh || !nameEn) {
      showError("editError", "Service, Name (Thai) and Name (Eng) are required.");
      return;
    }

    try {
      // RELATIVE path
      const updated = await apiPut(`api/${editingId}`, {
        service, nameTh, nameEn,
        updatedBy: 1 // TODO: logged-in user id
      });

      const idx = clients.findIndex(x => x.id === editingId);
      if (idx >= 0) clients[idx] = updated;

      hideError("editError");
      redrawTable();
      coreui.Modal.getInstance(document.getElementById("modalEdit"))?.hide();

    } catch (err) {
      console.error(err);
      showError("editError", "Update failed. Please try again.");
    }
  });

  // Delete
  document.getElementById("btnDeleteConfirm").addEventListener("click", async () => {
    if (deletingId == null) return;
    try {
      // RELATIVE path
      await apiDelete(`api/${deletingId}`);
      const idx = clients.findIndex(x => x.id === deletingId);
      if (idx >= 0) clients.splice(idx, 1);
      deletingId = null;
      redrawTable();
      coreui.Modal.getInstance(document.getElementById("modalDelete"))?.hide();
    } catch (err) {
      console.error(err);
      // optionally show toast
    }
  });
});
