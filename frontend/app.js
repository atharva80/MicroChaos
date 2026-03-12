const apiParam = new URLSearchParams(window.location.search).get("api");
const API_STORAGE_KEY = "microChaosApiBase";
if (apiParam) {
  window.localStorage.setItem(API_STORAGE_KEY, apiParam);
}
const API_BASE = apiParam || window.localStorage.getItem(API_STORAGE_KEY) || "http://localhost:8080/api";

const state = {
  services: [],
  dependencies: [],
  experiments: [],
  runs: [],
  monitoring: [],
  metricsByRun: new Map(),
  scorecardByRun: new Map(),
};

const serviceRows = document.getElementById("serviceRows");
const dependencyRows = document.getElementById("dependencyRows");
const experimentList = document.getElementById("experimentList");
const runList = document.getElementById("runList");
const runOutput = document.getElementById("runOutput");
const topologyGraph = document.getElementById("topologyGraph");
const monitoringRows = document.getElementById("monitoringRows");
const metricRows = document.getElementById("metricRows");
const rawPanel = document.getElementById("rawPanel");
const apiBaseLabel = document.getElementById("apiBaseLabel");

const sourceServiceId = document.getElementById("sourceServiceId");
const targetServiceId = document.getElementById("targetServiceId");
const experimentTargetId = document.getElementById("experimentTargetId");

const numberFormatter = new Intl.NumberFormat("en-US", {
  maximumFractionDigits: 2,
});

boot().catch(showError);

async function boot() {
  apiBaseLabel.textContent = API_BASE;
  bindEvents();
  await refreshAll();
  setInterval(() => {
    Promise.all([refreshOverview(), refreshMonitoring()])
      .then(() => renderTopology())
      .catch(showError);
  }, 3000);
}

function bindEvents() {
  document.getElementById("serviceForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = new FormData(event.target);
    await api(
      `/services?${qs({
        name: data.get("name"),
        baseUrl: data.get("baseUrl"),
        environment: data.get("environment"),
        projectId: 1,
      })}`,
      { method: "POST" }
    );
    event.target.reset();
    await refreshServicesAndTopology();
    await refreshOverview();
  });

  document.getElementById("dependencyForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    await api(
      `/topology/dependencies?${qs({
        sourceServiceId: sourceServiceId.value,
        targetServiceId: targetServiceId.value,
        dependencyType: document.getElementById("dependencyType").value,
        communicationMode: document.getElementById("communicationMode").value,
        protocol: "HTTP",
        criticality: "HIGH",
        fallbackAvailable: "true",
      })}`,
      { method: "POST" }
    );
    await refreshDependenciesAndTopology();
  });

  document.getElementById("experimentForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = new FormData(event.target);
    await api(
      `/experiments?${qs({
        projectId: 1,
        name: data.get("name"),
        targetServiceId: experimentTargetId.value,
        faultType: document.getElementById("faultType").value,
        stressType: document.getElementById("stressType").value,
        intensity: document.getElementById("intensity").value,
        durationSeconds: 60,
        blastRadiusLimit: 3,
        createdBy: 1,
      })}`,
      { method: "POST" }
    );
    event.target.reset();
    await refreshExperiments();
    await refreshOverview();
  });
}

async function refreshAll() {
  await Promise.all([
    refreshOverview(),
    refreshServices(),
    refreshDependencies(),
    refreshExperiments(),
    refreshRuns(),
    refreshMonitoring(),
  ]);
  renderTopology();
}

async function refreshOverview() {
  const overview = await api("/dashboard/overview");
  document.getElementById("totalServices").textContent = overview.totalServices ?? 0;
  document.getElementById("totalExperiments").textContent = overview.totalExperiments ?? 0;
  document.getElementById("activeRuns").textContent = overview.activeRuns ?? 0;
  document.getElementById("avgResilience").textContent = numberFormatter.format(overview.averageResilienceScore || 0);

  const monitoring = overview.monitoring || {};
  document.getElementById("healthyCount").textContent = monitoring.healthyCount ?? 0;
  document.getElementById("degradedCount").textContent = monitoring.degradedCount ?? 0;
  document.getElementById("downCount").textContent = monitoring.downCount ?? 0;
}

async function refreshServices() {
  const response = await api("/services");
  state.services = response.items || [];
  renderServices();
  refreshServiceSelects();
}

async function refreshDependencies() {
  const response = await api("/topology/dependencies");
  state.dependencies = response.items || [];
  renderDependencies();
}

async function refreshExperiments() {
  const response = await api("/experiments");
  state.experiments = response.items || [];
  renderExperiments();
}

async function refreshRuns() {
  const response = await api("/runs");
  state.runs = response.items || [];
  renderRuns();
}

async function refreshMonitoring() {
  const response = await api("/monitoring/services");
  state.monitoring = response.items || [];
  renderMonitoring();
}

async function refreshServicesAndTopology() {
  await refreshServices();
  await refreshDependencies();
  await refreshMonitoring();
  renderTopology();
}

async function refreshDependenciesAndTopology() {
  await refreshDependencies();
  renderTopology();
}

function renderServices() {
  serviceRows.innerHTML = "";
  for (const service of state.services) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${service.id}</td>
      <td>${escapeHtml(service.name)}</td>
      <td>${escapeHtml(service.baseUrl)}</td>
      <td>${escapeHtml(service.environment)}</td>
    `;
    serviceRows.appendChild(tr);
  }
}

function renderDependencies() {
  const byId = new Map(state.services.map((service) => [service.id, service]));
  dependencyRows.innerHTML = "";
  for (const dependency of state.dependencies) {
    const source = byId.get(dependency.sourceServiceId)?.name || dependency.sourceServiceId;
    const target = byId.get(dependency.targetServiceId)?.name || dependency.targetServiceId;
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${escapeHtml(String(source))}</td>
      <td>${escapeHtml(String(target))}</td>
      <td>${escapeHtml(dependency.dependencyType)}</td>
      <td>${escapeHtml(dependency.communicationMode)}</td>
      <td>${escapeHtml(dependency.protocol)}</td>
    `;
    dependencyRows.appendChild(tr);
  }
}

function renderExperiments() {
  experimentList.innerHTML = "";
  for (const experiment of state.experiments) {
    const serviceName = state.services.find((service) => service.id === experiment.targetServiceId)?.name || "unknown";
    const pill = document.createElement("div");
    pill.className = "pill";
    pill.innerHTML = `<span>#${experiment.id} ${escapeHtml(experiment.name)} -> ${escapeHtml(serviceName)} (${escapeHtml(
      experiment.faultType
    )})</span>`;

    const runButton = document.createElement("button");
    runButton.textContent = "Run";
    runButton.addEventListener("click", async () => {
      const result = await api(`/experiments/${experiment.id}/run`, { method: "POST" });
      showOutput(result, true);
      await refreshRuns();
      await refreshOverview();
      await refreshMonitoring();
    });
    pill.appendChild(runButton);

    experimentList.appendChild(pill);
  }
}

function renderRuns() {
  runList.innerHTML = "";
  for (const run of state.runs) {
    const pill = document.createElement("div");
    pill.className = "pill";
    pill.innerHTML = `<span>Run #${run.id} status=${run.status} score=${numberFormatter.format(run.resilienceScore || 0)}</span>`;

    const metricsButton = document.createElement("button");
    metricsButton.textContent = "Metrics";
    metricsButton.addEventListener("click", async () => {
      await loadMetrics(run.id);
    });
    pill.appendChild(metricsButton);

    const scorecardButton = document.createElement("button");
    scorecardButton.textContent = "Scorecard";
    scorecardButton.addEventListener("click", async () => {
      await loadScorecard(run.id);
    });
    pill.appendChild(scorecardButton);

    const rawButton = document.createElement("button");
    rawButton.textContent = "Raw";
    rawButton.addEventListener("click", async () => {
      const [runData, metricsData, scoreData] = await Promise.all([
        api(`/runs/${run.id}`),
        api(`/runs/${run.id}/metrics`),
        api(`/runs/${run.id}/scorecard`),
      ]);
      showOutput({ run: runData, metrics: metricsData, scorecard: scoreData }, true);
    });
    pill.appendChild(rawButton);

    runList.appendChild(pill);
  }
}

function renderMonitoring() {
  monitoringRows.innerHTML = "";
  for (const item of state.monitoring) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${escapeHtml(item.serviceName)}</td>
      <td><span class="state-pill ${stateClass(item.monitoringState)}">${escapeHtml(item.monitoringState)}</span></td>
      <td>${item.healthStatusCode}</td>
      <td>${item.healthResponseTimeMs}</td>
      <td>${item.activeFaultCount}</td>
      <td>${item.latencyMs}</td>
      <td>${item.timeoutMs}</td>
      <td>${numberFormatter.format(item.injectedErrorRate || 0)}</td>
      <td>${item.totalRequests ?? 0}</td>
      <td>${numberFormatter.format(computeFailurePercent(item))}</td>
      <td>
        <div class="action-group">
          <button class="btn-mini btn-bad" data-action="down" data-service="${item.serviceId}">Down</button>
          <button class="btn-mini btn-warn" data-action="latency" data-service="${item.serviceId}">Latency</button>
          <button class="btn-mini btn-good" data-action="recover" data-service="${item.serviceId}">Recover</button>
        </div>
      </td>
    `;
    tr.addEventListener("click", async (event) => {
      const actionButton = event.target.closest("button[data-action]");
      if (actionButton) {
        event.stopPropagation();
        await onServiceAction(actionButton.dataset.action, Number(actionButton.dataset.service));
        return;
      }
      const history = await api(`/monitoring/services/${item.serviceId}/history?limit=30`);
      showOutput({ service: item.serviceName, latest: item, history: history.items }, true);
    });
    monitoringRows.appendChild(tr);
  }
}

async function onServiceAction(action, serviceId) {
  if (action === "down") {
    await api(`/services/${serviceId}/faults/inject?type=DEPENDENCY_UNAVAILABLE&intensity=100&durationSeconds=300`, {
      method: "POST",
    });
  } else if (action === "latency") {
    await api(`/services/${serviceId}/faults/inject?type=LATENCY&intensity=60&durationSeconds=180`, { method: "POST" });
  } else if (action === "recover") {
    await api(`/services/${serviceId}/faults/reset`, { method: "POST" });
  }
  await refreshMonitoring();
  await refreshOverview();
  renderTopology();
}

async function loadMetrics(runId) {
  const response = await api(`/runs/${runId}/metrics`);
  const items = response.items || [];
  state.metricsByRun.set(runId, items);
  renderMetricTable(items);
  renderMetricSummary(items);
  showOutput({ runId, metricsCount: items.length }, false);
}

async function loadScorecard(runId) {
  const scorecard = await api(`/runs/${runId}/scorecard`);
  state.scorecardByRun.set(runId, scorecard);
  renderScorecard(scorecard);
  showOutput({ runId, scorecard }, false);
}

function renderMetricSummary(items) {
  document.getElementById("metricSamples").textContent = items.length;
  if (!items.length) {
    document.getElementById("metricAvgResponse").textContent = "-";
    document.getElementById("metricAvgP95").textContent = "-";
    document.getElementById("metricAvgError").textContent = "-";
    document.getElementById("metricAvgThroughput").textContent = "-";
    document.getElementById("metricAvgAvailability").textContent = "-";
    return;
  }

  const avgResponse = average(items.map((item) => item.responseTimeMs));
  const avgP95 = average(items.map((item) => item.p95LatencyMs));
  const avgError = average(items.map((item) => item.errorRate));
  const avgThroughput = average(items.map((item) => item.throughput));
  const avgAvailability = average(items.map((item) => item.availabilityPercent));

  document.getElementById("metricAvgResponse").textContent = numberFormatter.format(avgResponse);
  document.getElementById("metricAvgP95").textContent = numberFormatter.format(avgP95);
  document.getElementById("metricAvgError").textContent = numberFormatter.format(avgError);
  document.getElementById("metricAvgThroughput").textContent = numberFormatter.format(avgThroughput);
  document.getElementById("metricAvgAvailability").textContent = numberFormatter.format(avgAvailability);
}

function renderMetricTable(items) {
  metricRows.innerHTML = "";
  for (const item of items) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${escapeHtml(item.timestamp)}</td>
      <td>${numberFormatter.format(item.responseTimeMs)}</td>
      <td>${numberFormatter.format(item.p95LatencyMs)}</td>
      <td>${numberFormatter.format(item.errorRate)}</td>
      <td>${numberFormatter.format(item.throughput)}</td>
      <td>${numberFormatter.format(item.availabilityPercent)}</td>
    `;
    metricRows.appendChild(tr);
  }
}

function renderScorecard(score) {
  document.getElementById("scoreFault").textContent = numberFormatter.format(score.faultToleranceScore ?? 0);
  document.getElementById("scoreRecovery").textContent = numberFormatter.format(score.recoverySpeedScore ?? 0);
  document.getElementById("scoreObservability").textContent = numberFormatter.format(score.observabilityScore ?? 0);
  document.getElementById("scoreDependency").textContent = numberFormatter.format(score.dependencyStabilityScore ?? 0);
  document.getElementById("scoreOverall").textContent = numberFormatter.format(score.overallScore ?? 0);
}

function renderTopology() {
  const width = 980;
  const height = 520;
  topologyGraph.innerHTML = "";
  topologyGraph.setAttribute("viewBox", `0 0 ${width} ${height}`);
  if (!state.services.length) {
    return;
  }

  const monitoringByService = new Map(state.monitoring.map((item) => [item.serviceId, item]));

  const defs = svg("defs");
  const marker = svg("marker");
  marker.setAttribute("id", "arrow");
  marker.setAttribute("markerWidth", "11");
  marker.setAttribute("markerHeight", "11");
  marker.setAttribute("refX", "8");
  marker.setAttribute("refY", "3");
  marker.setAttribute("orient", "auto");
  const path = svg("path");
  path.setAttribute("d", "M0,0 L0,6 L9,3 z");
  path.setAttribute("fill", "#8da0ff");
  marker.appendChild(path);
  defs.appendChild(marker);
  topologyGraph.appendChild(defs);

  const cx = width / 2;
  const cy = height / 2;
  const radius = Math.min(width, height) * 0.34;
  const positions = new Map();

  state.services.forEach((service, index) => {
    const angle = (Math.PI * 2 * index) / state.services.length - Math.PI / 2;
    const x = cx + radius * Math.cos(angle);
    const y = cy + radius * Math.sin(angle);
    positions.set(service.id, { x, y, service });
  });

  for (const dependency of state.dependencies) {
    const source = positions.get(dependency.sourceServiceId);
    const target = positions.get(dependency.targetServiceId);
    if (!source || !target) {
      continue;
    }
    const color = edgeColor(dependency.dependencyType);
    const line = svg("line");
    line.setAttribute("x1", source.x);
    line.setAttribute("y1", source.y);
    line.setAttribute("x2", target.x);
    line.setAttribute("y2", target.y);
    line.setAttribute("stroke", color);
    line.setAttribute("stroke-width", "2.2");
    line.setAttribute("opacity", "0.9");
    if (dependency.communicationMode === "ASYNC") {
      line.setAttribute("stroke-dasharray", "9,8");
    }
    line.setAttribute("marker-end", "url(#arrow)");
    topologyGraph.appendChild(line);

    const label = svg("text");
    label.setAttribute("x", (source.x + target.x) / 2 + 8);
    label.setAttribute("y", (source.y + target.y) / 2 - 6);
    label.setAttribute("class", "edge-label");
    label.textContent = `${dependency.dependencyType}/${dependency.communicationMode}`;
    topologyGraph.appendChild(label);
  }

  positions.forEach((position) => {
    const stateItem = monitoringByService.get(position.service.id);
    const group = svg("g");
    const circle = svg("circle");
    circle.setAttribute("cx", position.x);
    circle.setAttribute("cy", position.y);
    circle.setAttribute("r", "46");
    circle.setAttribute("class", "node-circle " + nodeClass(stateItem?.monitoringState));
    circle.style.cursor = "pointer";
    circle.addEventListener("click", async () => {
      const upstream = await api(`/topology/services/${position.service.id}/upstream`);
      const downstream = await api(`/topology/services/${position.service.id}/downstream`);
      showOutput(
        {
          service: position.service.name,
          state: stateItem?.monitoringState || "UNKNOWN",
          upstream: upstream.items,
          downstream: downstream.items,
        },
        true
      );
    });
    group.appendChild(circle);

    const text = svg("text");
    text.setAttribute("x", position.x);
    text.setAttribute("y", position.y + 5);
    text.setAttribute("class", "node-text");
    text.textContent = position.service.name;
    group.appendChild(text);
    topologyGraph.appendChild(group);
  });
}

function refreshServiceSelects() {
  fillSelect(sourceServiceId, state.services);
  fillSelect(targetServiceId, state.services);
  fillSelect(experimentTargetId, state.services);
}

function fillSelect(selectElement, items) {
  selectElement.innerHTML = "";
  for (const item of items) {
    const option = document.createElement("option");
    option.value = item.id;
    option.textContent = `${item.id} - ${item.name}`;
    selectElement.appendChild(option);
  }
}

function edgeColor(type) {
  switch (type) {
    case "DB":
      return "#f59f0b";
    case "CACHE":
      return "#42d392";
    case "QUEUE":
      return "#c77dff";
    case "AUTH":
      return "#ff6b9c";
    default:
      return "#60a5fa";
  }
}

function nodeClass(stateLabel) {
  if (stateLabel === "HEALTHY") {
    return "node-healthy";
  }
  if (stateLabel === "DEGRADED") {
    return "node-degraded";
  }
  if (stateLabel === "DOWN") {
    return "node-down";
  }
  return "node-unknown";
}

function stateClass(stateLabel) {
  if (stateLabel === "HEALTHY") {
    return "state-healthy";
  }
  if (stateLabel === "DEGRADED") {
    return "state-degraded";
  }
  if (stateLabel === "DOWN") {
    return "state-down";
  }
  return "state-unknown";
}

function computeFailurePercent(item) {
  const total = item.totalRequests ?? 0;
  const failed = item.failedRequests ?? 0;
  if (!total) {
    return 0;
  }
  return (failed * 100.0) / total;
}

function average(values) {
  if (!values.length) {
    return 0;
  }
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function svg(tagName) {
  return document.createElementNS("http://www.w3.org/2000/svg", tagName);
}

async function api(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method || "GET",
  });
  const json = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(json.error || `Request failed with ${response.status}`);
  }
  return json;
}

function qs(obj) {
  return new URLSearchParams(obj).toString();
}

function showOutput(value, open = false) {
  runOutput.textContent = typeof value === "string" ? value : JSON.stringify(value, null, 2);
  if (open) {
    rawPanel.open = true;
  }
}

function showError(error) {
  runOutput.textContent = `Error: ${error.message}`;
  rawPanel.open = true;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
