// 1. 通用函数在apiService.js中定义

// --- 2. 配置信息 ---
const API_BASE_URL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
const API_ENDPOINT_DENSITY_ANALYSIS = `${API_BASE_URL}/densityAnalysis/analyze`;

// --- 3. DOM 元素引用 ---
const ui = {
  startTime: null, endTime: null, minLongitude: null, maxLatitude: null,
  maxLongitude: null, minLatitude: null, gridSize: null, timeSlotMinutes: null,
  analyzeBtn: null, resultDiv: null, currentTimeSlotDisplay: null,
  prevTimeSlotBtn: null, nextTimeSlotBtn: null, mapContainerParent: null,
};

// --- 4. 应用状态管理 ---
const appState = {
  mapInstance: null,
  densityResult: null,
  currentQueryInputParams: null,
  currentTimeIndex: 0,
  densityOverlays: [],
  gridCellPolygons: new Map(),
  globalEffectiveMaxDensity: 0,
};

// --- 通用信息显示与加载提示 ---
function displayResultsInfo(message, resultDivId = "f4_result") {
  const resultDiv = ui.resultDiv || document.getElementById(resultDivId);
  if (!resultDiv) return;
  resultDiv.innerHTML = `<p>${escapeHtml(message)}</p>`;
}

function showLoading(isLoading, message = "正在处理...", resultDivId = "f4_result") {
  const resultDiv = ui.resultDiv || document.getElementById(resultDivId);
  if (!resultDiv) return;
  resultDiv.innerHTML = isLoading ? `<p>${escapeHtml(message)} <span class="loading-spinner"></span></p>` : "";
}

// 简化版 displayFetchError
function displayFetchError(error, resultDivId, featureName = "操作") {
  const resultDiv = document.getElementById(resultDivId); // 直接获取，以防ui.resultDiv未初始化
  let displayHtml = `<p class="error-message">${escapeHtml(featureName)}出错：`;

  if (error && error.message) {
    displayHtml += `<br/>${escapeHtml(error.message)}`;
  } else {
    displayHtml += `。发生未知错误。`;
  }
  displayHtml += "</p>";

  if (resultDiv) {
    resultDiv.innerHTML = displayHtml;
  }
  console.error(`${featureName} 查询出错详情:`, error);
}


// --- 5. API 服务 (简化版，无复杂错误处理和封装) ---
async function fetchDensityData(params) {
  const featureName = "F4区域车流密度分析"; // 保留用于日志
  const options = {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(params),
  };

  console.log(`${featureName}: 发起请求到 POST ${API_ENDPOINT_DENSITY_ANALYSIS}`);

  const response = await fetch(API_ENDPOINT_DENSITY_ANALYSIS, options);

  console.log(`${featureName} 响应状态:`, response.status, "OK状态:", response.ok);

  if (!response.ok) {
    // 后端负责校验和详细错误信息，前端仅抛出通用网络错误
    throw new Error(`网络响应异常，状态码: ${response.status}`);
  }
  // 如果响应体不是有效的JSON，response.json() 会抛出错误
  return response.json();
}

// --- 6. 地图可视化模块 ---
const mapVisualizer = {
  initialize(mapInstanceFromGlobal) {
    appState.mapInstance = mapInstanceFromGlobal;
    return true;
  },

  clearAllOverlays() {
    if (!appState.mapInstance) return;
    appState.densityOverlays.forEach((overlay) => {
      if (overlay instanceof HTMLElement && overlay.id === "density-legend" && overlay.parentNode) {
        overlay.parentNode.removeChild(overlay);
      } else if (typeof appState.mapInstance.removeOverlay === "function") {
        // 移除了 try-catch
        appState.mapInstance.removeOverlay(overlay);
      }
    });
    appState.densityOverlays = [];
    appState.gridCellPolygons.clear();
  },

  drawGrid(gridResultData, queryInputParams) {
    if (!appState.mapInstance || !gridResultData || !queryInputParams) {
      console.error("密度分析 (F4): 绘制网格所需数据不完整。");
      return false; // 保持返回false，让调用者知道操作未完成
    }
    const { rows, cols } = gridResultData;
    const { minLongitude, minLatitude, maxLongitude, maxLatitude } = queryInputParams;

    if (Number.isFinite(minLongitude) && Number.isFinite(minLatitude) && Number.isFinite(maxLongitude) && Number.isFinite(maxLatitude)) {
      const borderPoints = [
        new BMapGL.Point(minLongitude, maxLatitude), new BMapGL.Point(maxLongitude, maxLatitude),
        new BMapGL.Point(maxLongitude, minLatitude), new BMapGL.Point(minLongitude, minLatitude),
        new BMapGL.Point(minLongitude, maxLatitude),
      ];
      const borderPolygon = new BMapGL.Polygon(borderPoints, {
        strokeColor: "#1E90FF", strokeWeight: 2, strokeOpacity: 0.9, fillOpacity: 0,
      });
      appState.mapInstance.addOverlay(borderPolygon); // 无 try-catch
      appState.densityOverlays.push(borderPolygon);
    } else {
      console.warn("密度分析 (F4): 查询边界坐标无效，无法绘制网格边界。");
      // displayResultsInfo("查询边界坐标无效，无法绘制网格边界。", "f4_result"); // 可选，或由调用者处理
      return false;
    }

    if (rows === 0 || cols === 0) {
      console.warn("密度分析 (F4): 网格行列数为0，仅绘制查询边界。");
      return true; // 边界已画，操作部分成功
    }
    const cellWidth = (maxLongitude - minLongitude) / cols;
    const cellHeight = (maxLatitude - minLatitude) / rows;

    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const cellMinLon = minLongitude + c * cellWidth;
        const cellMaxLat = maxLatitude - r * cellHeight;
        const cellPoints = [
          new BMapGL.Point(cellMinLon, cellMaxLat), new BMapGL.Point(cellMinLon + cellWidth, cellMaxLat),
          new BMapGL.Point(cellMinLon + cellWidth, cellMaxLat - cellHeight), new BMapGL.Point(cellMinLon, cellMaxLat - cellHeight),
          new BMapGL.Point(cellMinLon, cellMaxLat),
        ];
        const cellPolygon = new BMapGL.Polygon(cellPoints, {
          strokeColor: "#DDDDDD", strokeWeight: 0.5, strokeOpacity: 0.4,
          fillColor: "#FFFFFF", fillOpacity: 0.1,
        });
        appState.mapInstance.addOverlay(cellPolygon); // 无 try-catch
        appState.densityOverlays.push(cellPolygon);
        appState.gridCellPolygons.set(`${r},${c}`, cellPolygon);
      }
    }
    // console.log(`密度分析 (F4): ${rows}x${cols} 网格绘制完成。`); // 可选日志
    return true;
  },

  updateHeatmapForTimeSlot(timeIndex) {
    if (!appState.mapInstance || !appState.densityResult || !appState.densityResult.timeSlots || appState.densityResult.timeSlots.length === 0) {
      // console.warn("密度分析 (F4): 更新热力图所需数据或地图实例缺失。"); // 可选日志
      return;
    }
    appState.currentTimeIndex = timeIndex;
    const timeSlotKey = appState.densityResult.timeSlots[timeIndex];
    let slotData = appState.densityResult.densityMap[timeSlotKey];

    if (slotData === undefined && timeSlotKey && timeSlotKey.endsWith(":00")) {
      const altKey = timeSlotKey.substring(0, timeSlotKey.length - 3);
      slotData = appState.densityResult.densityMap[altKey];
    }
    const slotDensityData = slotData || {};

    appState.gridCellPolygons.forEach((polygon, cellId) => {
      const rawDensity = slotDensityData[cellId];
      const density = (rawDensity !== undefined && !isNaN(Number(rawDensity))) ? Number(rawDensity) : 0;
      let normalizedDensity = 0;
      if (appState.globalEffectiveMaxDensity > 0) {
        normalizedDensity = Math.min(density, appState.globalEffectiveMaxDensity) / appState.globalEffectiveMaxDensity;
      }
      polygon.setFillColor(this.getColorForDensity(normalizedDensity, density)); // 无 try-catch
      polygon.setFillOpacity(0.75); // 无 try-catch
    });
    updateTimeDisplay();
  },

  getColorForDensity(normalizedDensity, actualDensity) {
    if (actualDensity === 0) return "#EFF3FF";
    if (normalizedDensity <= 0) return "rgb(173,216,230)";
    if (normalizedDensity < 0.02) return "rgb(173,216,230)";
    if (normalizedDensity < 0.1) return "rgb(100,149,237)";
    if (normalizedDensity < 0.25) return "rgb(0,191,255)";
    if (normalizedDensity < 0.4) return "rgb(60,179,113)";
    if (normalizedDensity < 0.6) return "rgb(255,255,0)";
    if (normalizedDensity < 0.8) return "rgb(255,165,0)";
    return "rgb(255,69,0)";
  },

  addLegend() {
    if (!appState.mapInstance || !ui.mapContainerParent) {
      // console.warn("密度分析 (F4): 无法添加图例 - 地图实例或图例父容器未找到。"); // 可选日志
      return;
    }
    this.removeLegend();
    const legendDiv = document.createElement("div");
    legendDiv.id = "density-legend";
    Object.assign(legendDiv.style, { /* ... 样式同前 ... */
      position: "absolute", bottom: "20px", right: "20px",
      backgroundColor: "rgba(255, 255, 255, 0.9)", padding: "10px",
      border: "1px solid #ccc", borderRadius: "5px", boxShadow: "0 2px 6px rgba(0,0,0,0.3)",
      zIndex: "1000", fontFamily: "Arial, sans-serif", fontSize: "12px",
    });
    legendDiv.innerHTML = `<div style="font-weight: bold; margin-bottom: 8px; text-align: center;">车流密度</div>
      <div style="display: flex; align-items: center; margin-bottom: 4px;">
          <div style="height: 15px; width: 15px; background-color: #EFF3FF; margin-right: 8px; border: 1px solid #999;"></div>
          <span>零密度 / 无数据</span>
      </div>
      <div style="margin-bottom: 4px;">密度渐变 (示意):</div>
      <div style="height: 20px; width: 180px; background: linear-gradient(to right,
          rgb(173,216,230), rgb(100,149,237), rgb(0,191,255),
          rgb(60,179,113), rgb(255,255,0), rgb(255,165,0), rgb(255,69,0)
          ); border: 1px solid #ccc; margin-bottom: 2px;"></div>
      <div style="display: flex; justify-content: space-between; width: 180px; font-size: 12px;">
          <span>低</span><span>高</span>
      </div>`;
    ui.mapContainerParent.appendChild(legendDiv);
    appState.densityOverlays.push(legendDiv);
  },

  removeLegend() {
    const legend = document.getElementById("density-legend");
    if (legend && legend.parentNode) {
      legend.parentNode.removeChild(legend);
      appState.densityOverlays = appState.densityOverlays.filter(o => o !== legend);
    }
  },

  fitMapToArea(queryInputParams) {
    if (!appState.mapInstance || !queryInputParams) return;
    const { minLongitude, minLatitude, maxLongitude, maxLatitude } = queryInputParams;

    if ([minLongitude, minLatitude, maxLongitude, maxLatitude].some(coord => !Number.isFinite(coord))) {
      // console.warn("密度分析 (F4): 无法适应地图区域 - 无效的坐标。", queryInputParams); // 可选日志
      return;
    }
    if (minLongitude === maxLongitude || minLatitude === maxLatitude) {
      // console.warn("密度分析 (F4): 无法适应地图到零面积区域，将地图中心设置到该点。"); // 可选日志
      appState.mapInstance.setCenter(new BMapGL.Point(minLongitude, minLatitude)); // 无 try-catch
      return;
    }
    const sw = new BMapGL.Point(minLongitude, minLatitude);
    const ne = new BMapGL.Point(maxLongitude, maxLatitude);
    appState.mapInstance.setViewport([sw, ne], { // 无 try-catch
      margins: [50, 20, 20, 20], enableAnimation: true, zoomFactor: -1,
    });
  },
};

// --- 7. UI 更新函数 ---
function updateTimeDisplay() {
  if (!ui.currentTimeSlotDisplay) return;
  if (!appState.densityResult || !appState.densityResult.timeSlots || appState.densityResult.timeSlots.length === 0) {
    ui.currentTimeSlotDisplay.value = "无数据";
    return;
  }
  const currentDateTimeISO = appState.densityResult.timeSlots[appState.currentTimeIndex];
  ui.currentTimeSlotDisplay.value = currentDateTimeISO ? currentDateTimeISO.replace("T", " ") : "无数据";
}

// --- 8. 主要分析处理函数 ---
async function handleAnalyzeDensity() {
  const featureName = "F4区域车流密度分析";
  if (!appState.mapInstance) {
    displayFetchError(new Error("地图尚未初始化或不可用。请确保地图已加载。"), "f4_result", featureName);
    return;
  }
  showLoading(true, "正在准备分析...");

  // 直接从UI元素获取值并进行基本类型转换，无前端校验
  const params = {
    startTime: ui.startTime.value,
    endTime: ui.endTime.value,
    minLongitude: Number.parseFloat(ui.minLongitude.value),
    maxLatitude: Number.parseFloat(ui.maxLatitude.value),
    maxLongitude: Number.parseFloat(ui.maxLongitude.value),
    minLatitude: Number.parseFloat(ui.minLatitude.value),
    gridSize: Number.parseFloat(ui.gridSize.value),
    timeSlotMinutes: Number.parseInt(ui.timeSlotMinutes.value, 10),
  };

  appState.currentQueryInputParams = params;
  // console.log(`${featureName}: 发送到后端的请求参数:`, params); // 可选日志
  showLoading(true, "正在进行密度分析...");


  mapVisualizer.clearAllOverlays();
  appState.densityResult = null; appState.currentTimeIndex = 0; appState.globalEffectiveMaxDensity = 0;
  updateTimeDisplay();

  const resultData = await fetchDensityData(params); // 错误会直接抛出

  // 保留对响应数据结构的基本检查，因为这是成功路径上的预期
  if (!resultData || typeof resultData.densityMap !== "object" || !Array.isArray(resultData.timeSlots) || resultData.rows == null || resultData.cols == null) {
    // 如果结构不符，显示错误并停止。这不算是“校验”，而是对成功响应的期望。
    displayFetchError(new Error("后端返回的数据格式不正确或缺少必要字段。"), "f4_result", featureName);
    showLoading(false); // 尝试清除加载状态
    mapVisualizer.removeLegend();
    return;
  }
  appState.densityResult = resultData;

  const allNonZeroDensities = [];
  Object.values(appState.densityResult.densityMap).forEach(slotData => {
    if (typeof slotData === "object" && slotData !== null) {
      Object.values(slotData).forEach(rawValue => {
        const density = Number(rawValue);
        if (!isNaN(density) && density > 0) allNonZeroDensities.push(density);
      });
    }
  });

  if (allNonZeroDensities.length > 0) {
    allNonZeroDensities.sort((a, b) => a - b);
    const percentileIndex = Math.min(Math.floor(allNonZeroDensities.length * 0.98), allNonZeroDensities.length - 1);
    appState.globalEffectiveMaxDensity = allNonZeroDensities[Math.max(0, percentileIndex)];
    if (appState.globalEffectiveMaxDensity === 0 && allNonZeroDensities.length > 0) {
      appState.globalEffectiveMaxDensity = allNonZeroDensities[allNonZeroDensities.length - 1];
    }
  } else {
    appState.globalEffectiveMaxDensity = 0;
  }

  if (!mapVisualizer.drawGrid(resultData, params)) {
    // drawGrid 内部可能会显示错误，或者这里可以补充一个通用提示
    displayResultsInfo("绘制基础网格失败。", "f4_result");
    showLoading(false);
    mapVisualizer.removeLegend();
    return;
  }
  if (Number.isFinite(params.minLongitude)) { // 确保 params.minLongitude 是有效数字
    mapVisualizer.fitMapToArea(params);
  }


  const noDensityDataFound = Object.keys(resultData.densityMap).every(
      (key) => Object.keys(resultData.densityMap[key]).length === 0
  );

  if (resultData.timeSlots.length === 0 || (appState.globalEffectiveMaxDensity === 0 && noDensityDataFound)) {
    displayResultsInfo("分析完成。在指定条件下未找到有效的密度数据。", "f4_result");
  } else {
    mapVisualizer.updateHeatmapForTimeSlot(0);
    displayResultsInfo(`密度分析成功，共 ${resultData.timeSlots.length} 个时间点。`, "f4_result");
  }
  mapVisualizer.addLegend();
  showLoading(false); // 成功路径的最后清除加载状态

  // 由于没有了 catch, 如果在 await fetchDensityData() 之后发生错误，
  // 上面的 showLoading(false) 可能不会执行。这是“牺牲健壮性”的体现。
}

// --- 9. 事件监听器设置 ---
function setupEventListeners() {
  if (ui.analyzeBtn) {
    ui.analyzeBtn.addEventListener("click", handleAnalyzeDensity);
  } else {
    console.error("密度分析 (F4): 关键UI元素（分析按钮 'densityAnalysisBtn'）未在DOM中找到。");
    if(ui.resultDiv) displayFetchError(new Error("关键UI元素（分析按钮）缺失，功能无法启动。"), "f4_result", "F4页面初始化");
  }

  if (ui.prevTimeSlotBtn) {
    ui.prevTimeSlotBtn.addEventListener("click", () => {
      if (appState.densityResult && appState.densityResult.timeSlots && appState.densityResult.timeSlots.length > 0 && appState.currentTimeIndex > 0) {
        mapVisualizer.updateHeatmapForTimeSlot(appState.currentTimeIndex - 1);
      }
    });
  } // 移除了对按钮未找到的 console.warn，以求更简单

  if (ui.nextTimeSlotBtn) {
    ui.nextTimeSlotBtn.addEventListener("click", () => {
      if (appState.densityResult && appState.densityResult.timeSlots && appState.densityResult.timeSlots.length > 0 && appState.currentTimeIndex < appState.densityResult.timeSlots.length - 1) {
        mapVisualizer.updateHeatmapForTimeSlot(appState.currentTimeIndex + 1);
      }
    });
  } // 移除了对按钮未找到的 console.warn
  // console.log("密度分析 (F4): 事件监听器已设置。"); // 可选日志
}

// --- 10. 初始化 (包含轮询功能) ---
document.addEventListener("DOMContentLoaded", () => {
  // console.log("密度分析 (F4): DOM 内容已加载完毕 (DOMContentLoaded)。"); // 可选日志

  ui.startTime = document.getElementById("f4_startTime");
  ui.endTime = document.getElementById("f4_endTime");
  ui.minLongitude = document.getElementById("f4_topLeftLng");
  ui.maxLatitude = document.getElementById("f4_topLeftLat");
  ui.maxLongitude = document.getElementById("f4_bottomRightLng");
  ui.minLatitude = document.getElementById("f4_bottomRightLat");
  ui.gridSize = document.getElementById("gridRadius");
  ui.timeSlotMinutes = document.getElementById("timeInterval");
  ui.analyzeBtn = document.getElementById("densityAnalysisBtn");
  ui.resultDiv = document.getElementById("f4_result");
  ui.currentTimeSlotDisplay = document.getElementById("currentTimeSlot");
  ui.prevTimeSlotBtn = document.getElementById("prevTimeSlot");
  ui.nextTimeSlotBtn = document.getElementById("nextTimeSlot");
  ui.mapContainerParent = document.getElementById("container");

  if (!ui.resultDiv) { // 尝试动态创建 resultDiv，保持一定的界面容错
    const f4Container = document.getElementById("f4");
    if (f4Container) {
      const newResultDiv = document.createElement("div");
      newResultDiv.id = "f4_result";
      newResultDiv.className = "function-result";
      f4Container.appendChild(newResultDiv);
      ui.resultDiv = newResultDiv;
    } else {
      console.error("密度分析 (F4): 未能找到父容器 #f4 来创建 #f4_result DIV。");
    }
  }

  if (!ui.analyzeBtn) {
    const initErrorMsg = "页面初始化不完整（分析按钮 'densityAnalysisBtn' 未找到），功能无法使用。";
    console.error(`密度分析 (F4): ${initErrorMsg}`);
    if (ui.resultDiv) displayFetchError(new Error(initErrorMsg), "f4_result", "F4初始化");
    return;
  }
  // console.log("密度分析 (F4): UI 元素引用已填充。开始轮询等待地图实例..."); // 可选日志

  let mapCheckInterval;
  const MAX_MAP_WAIT_TIME = 10000;
  const MAP_CHECK_INTERVAL_MS = 200;
  let initializeAttempted = false;

  function initializeMapDependentComponents(mapInstance) {
    if (initializeAttempted) return;
    initializeAttempted = true;

    if (!mapVisualizer.initialize(mapInstance)) {
      // 地图可视化组件初始化失败通常是内部逻辑问题，而非可捕获的运行时错误
      // 但如果 initialize 内部有错误，会直接抛出
      if(ui.resultDiv) displayFetchError(new Error("地图可视化组件初始化失败。"), "f4_result", "F4密度分析");
      return;
    }
    setupEventListeners();
    if(ui.resultDiv) displayResultsInfo('请设置参数并点击"密度分析"按钮。', "f4_result");
    // console.log("密度分析 (F4): 地图实例已获取，依赖地图的组件和功能已设置完毕。"); // 可选日志
  }

  function tryInitializeMapLogic() {
    if (typeof window.map !== "undefined" && window.map instanceof BMapGL.Map && typeof window.map.getCenter === "function" && window.map.getCenter()) {
      clearInterval(mapCheckInterval);
      initializeMapDependentComponents(window.map);
    }
  }

  mapCheckInterval = setInterval(tryInitializeMapLogic, MAP_CHECK_INTERVAL_MS);

  setTimeout(() => {
    clearInterval(mapCheckInterval);
    if (!appState.mapInstance && !initializeAttempted) {
      if (typeof window.map !== "undefined" && window.map instanceof BMapGL.Map && typeof window.map.getCenter === "function" && window.map.getCenter()) {
        initializeMapDependentComponents(window.map);
      } else {
        const mapErrorMsg = `百度地图服务在 ${MAX_MAP_WAIT_TIME / 1000} 秒后仍不可用或未正确实例化。`;
        if(ui.resultDiv) displayFetchError(new Error(mapErrorMsg), "f4_result", "F4密度分析初始化");
        console.error("密度分析 (F4): " + mapErrorMsg + " (轮询超时)");
        if(ui.analyzeBtn) {
          ui.analyzeBtn.disabled = true; // 禁用按钮以提示用户
        }
      }
    }
  }, MAX_MAP_WAIT_TIME);
});
