// 文件: static/js/features/f4-density.js
/**
 * Taxi Trajectory Density Analysis Frontend Logic (F4)
 *
 * TARGET STATE:
 * - Integrates with a global overlay management system (window.allFeatureOverlays).
 * - Retains the working density color display logic (including time key fallback).
 * - Calls global clearOverlays() at the beginning of analysis.
 * - Relies on global clearOverlays() to clear its overlays via window.allFeatureOverlays.
 */
;((BMapGL, window, document) => {
  // --- 1. 配置信息 ---
  const API_BASE_URL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
  const API_ENDPOINT = `${API_BASE_URL}/densityAnalysis/analyze`;

  // --- F4模块初始化：在全局覆盖物管理器中注册专属条目 ---
  if (typeof window.allFeatureOverlays === 'undefined') {
    window.allFeatureOverlays = {};
  }
  // 确保这些键与 map-utils.js 中 clearOverlays 函数处理 F4 的逻辑一致（如果它有特定逻辑的话）
  // 或者，如果 map-utils.js 的 clearOverlays 通用地遍历所有 allFeatureOverlays 的属性，则键名具有一定灵活性。
  // 我们之前约定的是 map-utils.js 会通用地遍历。
  window.allFeatureOverlays["F4_查询边界"] = null;
  window.allFeatureOverlays["F4_密度单元格"] = [];
  window.allFeatureOverlays["F4_图例"] = null;
  // console.log("F4-DENSITY: 已在 window.allFeatureOverlays 中初始化 F4 的专属条目。");

  // --- 2. DOM 元素引用 ---
  const ui = {
    startTime: null, endTime: null, minLongitude: null, maxLatitude: null,
    maxLongitude: null, minLatitude: null, gridSize: null, timeSlotMinutes: null,
    analyzeBtn: null, resultDiv: null, /* commonResultDiv: null, */ // 移除或确认其用途
    currentTimeSlotDisplay: null, prevTimeSlotBtn: null, nextTimeSlotBtn: null,
    mapContainerParent: null,
  };

  // --- 3. 应用状态管理 (F4模块内部) ---
  const appState = {
    mapInstance: null, // 由外部 map 初始化
    densityResult: null,
    currentQueryInputParams: null,
    currentTimeIndex: 0,
    // gridCellPolygons: new Map(), // 仍然用于高效更新颜色，但其成员也是allFeatureOverlays的一部分
    // 修改：为了简化，并且因为 drawGrid 每次都重新创建单元格，
    // 我们可以直接从 window.allFeatureOverlays["F4_密度单元格"] 中通过 _f4_cellId 查找来更新。
    // 但保留 appState.gridCellPolygons (Map: "r,c" -> Polygon) 依然是最高效的更新方式，
    // 只需要确保这个Map中的Polygon对象和allFeatureOverlays中的是同一个对象。
    gridCellPolygons: new Map(), // 用于按 cellId ("r,c") 快速查找对应的 Polygon 对象进行颜色更新
    globalEffectiveMaxDensity: 0,
  };

  // --- 4. API 服务 ---
  async function fetchDensityData(params) {
    // if (ui.commonResultDiv) ui.commonResultDiv.innerHTML = ""; // 确认是否需要
    showLoading(true, "正在向服务器请求分析数据...");
    try {
      // 使用全局的 fetchApi (假设它在 apiService.js 中定义并能处理错误显示)
      const responseData = await fetchApi(API_ENDPOINT, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(params),
      }, "F4密度分析");
      return responseData;
    } catch (error) {
      // 如果 fetchApi 本身不显示错误，则在这里显示
      // displayError(error.message || "从服务器获取密度数据失败。");
      throw error; // 重新抛出，让 handleAnalyzeDensity 处理
    } finally {
      showLoading(false);
    }
  }

  // --- 5. 地图可视化模块 ---
  const mapVisualizer = {
    initialize(mapInstanceFromGlobal) {
      if (!mapInstanceFromGlobal || !(mapInstanceFromGlobal instanceof BMapGL.Map)) {
        console.error("F4-DENSITY: 无效的地图实例提供给 visualizer。");
        return false;
      }
      appState.mapInstance = mapInstanceFromGlobal;
      // console.log("F4-DENSITY: MapVisualizer 初始化成功。");
      return true;
    },

    // mapVisualizer.clearAllOverlays() 方法不再需要。
    // 清除工作由 handleAnalyzeDensity -> global clearOverlays() -> map-utils.js 清理 allFeatureOverlays 来完成。
    // appState.gridCellPolygons.clear() 将在 handleAnalyzeDensity 开始时执行。

    drawGrid(gridResultData, queryInputParams) {
      if (!appState.mapInstance) { console.error("F4-DENSITY: 地图未初始化，无法绘制网格。"); return; }

      const { rows, cols } = gridResultData;
      const { minLongitude, minLatitude, maxLongitude, maxLatitude } = queryInputParams;

      // 绘制查询区域总边界
      const borderPoints = [
        new BMapGL.Point(minLongitude, maxLatitude), new BMapGL.Point(maxLongitude, maxLatitude),
        new BMapGL.Point(maxLongitude, minLatitude), new BMapGL.Point(minLongitude, minLatitude),
        new BMapGL.Point(minLongitude, maxLatitude),
      ];
      const borderPolygon = new BMapGL.Polygon(borderPoints, {
        strokeColor: "#1E90FF", strokeWeight: 2, strokeOpacity: 0.9, fillOpacity: 0,
      });
      appState.mapInstance.addOverlay(borderPolygon);
      // **修改**: 存储到全局管理器
      window.allFeatureOverlays["F4_查询边界"] = borderPolygon;

      // 清空F4内部的快速查找Map，因为我们要重新填充它
      appState.gridCellPolygons.clear();
      const newDensityCells = []; // 临时数组，用于本次绘制的单元格

      if (rows === 0 || cols === 0) {
        // displayResultsInfo("分析区域有效，但网格行列数为0。"); // 可选
        window.allFeatureOverlays["F4_密度单元格"] = []; // 确保是空数组
        return;
      }

      const cellWidth = (maxLongitude - minLongitude) / cols;
      const cellHeight = (maxLatitude - minLatitude) / rows;

      for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
          const cellMinLon = minLongitude + c * cellWidth;
          const cellMaxLat = maxLatitude - r * cellHeight;
          const cellMaxLon = (c === cols - 1) ? maxLongitude : cellMinLon + cellWidth;
          const cellMinLat = (r === rows - 1) ? minLatitude : cellMaxLat - cellHeight;
          const cellPoints = [
            new BMapGL.Point(cellMinLon, cellMaxLat), new BMapGL.Point(cellMaxLon, cellMaxLat),
            new BMapGL.Point(cellMaxLon, cellMinLat), new BMapGL.Point(cellMinLon, cellMinLat),
            new BMapGL.Point(cellMinLon, cellMaxLat),
          ];
          const cellPolygon = new BMapGL.Polygon(cellPoints, {
            strokeColor: "#DDDDDD", strokeWeight: 0.5, strokeOpacity: 0.4,
            fillColor: "#FFFFFF", fillOpacity: 0.1,
          });
          appState.mapInstance.addOverlay(cellPolygon);

          // **修改**: 同时存入F4内部的Map和全局管理器的数组
          const cellId = `${r},${c}`;
          appState.gridCellPolygons.set(cellId, cellPolygon);
          newDensityCells.push(cellPolygon);
        }
      }
      window.allFeatureOverlays["F4_密度单元格"] = newDensityCells; // 更新全局列表
      // console.log(`F4-DENSITY: 绘制了 ${newDensityCells.length} 个密度单元格。`);
    },

    updateHeatmapForTimeSlot(timeIndex) {
      if (!appState.mapInstance || !appState.densityResult || !appState.densityResult.timeSlots || appState.densityResult.timeSlots.length === 0) {
        return;
      }
      appState.currentTimeIndex = timeIndex;
      const timeSlotKeyFromList = appState.densityResult.timeSlots[timeIndex];

      let slotDataFromMap = appState.densityResult.densityMap[timeSlotKeyFromList];
      // 关键的时间键兼容逻辑 (保持您工作版本中的这个重要逻辑)
      if (slotDataFromMap === undefined && typeof timeSlotKeyFromList === 'string' && timeSlotKeyFromList.endsWith(":00")) {
        const timeSlotKeyWithoutSeconds = timeSlotKeyFromList.substring(0, timeSlotKeyFromList.length - 3);
        slotDataFromMap = appState.densityResult.densityMap[timeSlotKeyWithoutSeconds];
      }
      const slotDensityData = slotDataFromMap || {};

      // 详细日志
      // console.log(`F4_KEY_DEBUG: 目标时间槽 (来自timeSlots数组): "${timeSlotKeyFromList}"`);
      // console.log(`F4_KEY_DEBUG: 最终使用的 slotDensityData 是否为空对象: ${Object.keys(slotDensityData).length === 0}`);
      // console.log(`F4_KEY_DEBUG: 全局有效最大密度: ${appState.globalEffectiveMaxDensity}`);

      appState.gridCellPolygons.forEach((polygon, cellId) => { // cellId 是 "r,c"
        const rawDensityValue = slotDensityData[cellId];
        const density = rawDensityValue !== undefined && !isNaN(Number(rawDensityValue)) ? Number(rawDensityValue) : 0;
        let normalizedDensity = 0;
        if (appState.globalEffectiveMaxDensity > 0) {
          normalizedDensity = Math.min(density, appState.globalEffectiveMaxDensity) / appState.globalEffectiveMaxDensity;
        } else if (density > 0) {
          normalizedDensity = 1;
        }
        const color = this.getColorForDensity(normalizedDensity, density);

        polygon.setFillColor(color);
        polygon.setFillOpacity(0.75);
      });
      updateTimeDisplay();
    },

    getColorForDensity(normalizedDensity, actualDensity) { /* ... 保持您工作版本中的颜色逻辑 ... */
      if (actualDensity === 0) return "#EFF3FF";
      let r, g, b;
      if (normalizedDensity <= 0) { r = 100; g = 149; b = 237; }
      else if (normalizedDensity < 0.02) { r = 173; g = 216; b = 230; }
      else if (normalizedDensity < 0.1) { r = 100; g = 149; b = 237; }
      else if (normalizedDensity < 0.25) { r = 0; g = 191; b = 255; }
      else if (normalizedDensity < 0.4) { r = 60; g = 179; b = 113; }
      else if (normalizedDensity < 0.6) { r = 255; g = 255; b = 0; }
      else if (normalizedDensity < 0.8) { r = 255; g = 165; b = 0; }
      else { r = 255; g = 69; b = 0; }
      return `rgb(${r},${g},${b})`;
    },

    addLegend() {
      if (!appState.mapInstance || !ui.mapContainerParent) return;

      // 移除旧图例（如果存在于全局管理器中）
      const existingLegend = window.allFeatureOverlays["F4_图例"];
      if (existingLegend && existingLegend.parentNode) {
        existingLegend.parentNode.removeChild(existingLegend);
        window.allFeatureOverlays["F4_图例"] = null;
      }

      const legendDiv = document.createElement("div");
      legendDiv.id = "density-legend"; // ID 可以保留
      Object.assign(legendDiv.style, {
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
      // 存储到全局管理器
      window.allFeatureOverlays["F4_图例"] = legendDiv;
      // console.log("F4-DENSITY: Legend added and stored globally.");
    },

    // removeLegend() 不再需要由外部直接调用，全局 clearOverlays 会处理 window.allFeatureOverlays["F4_图例"]

    fitMapToArea(queryInputParams) { /* ... 保持您工作版本中的实现 ... */
      if (!appState.mapInstance || !queryInputParams) return;
      const { minLongitude, minLatitude, maxLongitude, maxLatitude } = queryInputParams;
      const sw = new BMapGL.Point(minLongitude, minLatitude);
      const ne = new BMapGL.Point(maxLongitude, maxLatitude);
      if ( isNaN(sw.lng) || isNaN(sw.lat) || isNaN(ne.lng) || isNaN(ne.lat) || minLongitude === maxLongitude || minLatitude === maxLatitude ) {
        if (!isNaN(sw.lng) && !isNaN(sw.lat)) appState.mapInstance.setCenter(sw);
        return;
      }
      appState.mapInstance.setViewport([sw, ne], {
        margins: [50, 20, 20, 20], enableAnimation: true, zoomFactor: -1,
      });
    },
  };

  // --- 6. UI 更新与消息显示函数 ---
  function showLoading(isLoading, message = "正在处理...") { /* ...保持您工作版本中的实现... */
    if (!ui.resultDiv) return;
    ui.resultDiv.innerHTML = isLoading ? `<p>${escapeHtml(message)} <span class="loading-spinner"></span></p>` : "";
  }
  function displayResultsInfo(message) { /* ...保持您工作版本中的实现... */
    if (!ui.resultDiv) return;
    ui.resultDiv.innerHTML = `<p>${escapeHtml(message)}</p>`;
  }
  function displayError(errorMessage) { /* ...保持您工作版本中的实现... */
    if (!ui.resultDiv) { console.error("F4-DENSITY: Error display failed, ui.resultDiv not found. Error: " + escapeHtml(errorMessage)); return; }
    ui.resultDiv.innerHTML = `<p style="color: red; font-weight: bold;">错误：${escapeHtml(errorMessage)}</p>`;
  }
  function updateTimeDisplay() {
    if (!ui.currentTimeSlotDisplay) return;
    if (!appState.densityResult || !appState.densityResult.timeSlots || appState.densityResult.timeSlots.length === 0) {
      ui.currentTimeSlotDisplay.value = "无数据"; return;
    }
    const currentDateTimeISO = appState.densityResult.timeSlots[appState.currentTimeIndex];
    ui.currentTimeSlotDisplay.value = currentDateTimeISO ? currentDateTimeISO.replace("T", " ") : "无数据";
  }

  // --- 7. 主要分析处理函数 ---
  async function handleAnalyzeDensity() {
    if (!appState.mapInstance) {
      displayError("地图尚未初始化。");
      return;
    }

    // --- 关键修改：在执行任何F4特定操作前，调用全局清除函数 ---
    if (typeof clearOverlays === "function") { // clearOverlays 来自 map-utils.js
      console.log("F4-DENSITY (handleAnalyzeDensity): 调用全局 clearOverlays()。");
      clearOverlays(); // 这会清除所有模块的覆盖物，包括F4通过allFeatureOverlays注册的
    } else {
      console.warn("F4-DENSITY: 全局 clearOverlays() 函数未定义!");
    }

    // 重置F4内部状态
    appState.densityResult = null;
    appState.currentTimeIndex = 0;
    appState.globalEffectiveMaxDensity = 0;
    appState.gridCellPolygons.clear(); // 清空F4用于快速查找单元格的Map
    updateTimeDisplay();

    // 前端输入参数获取和校验 (保持您工作版本中的详细校验)
    const rawInputs = { /* ... */ }; /* 同您工作版本 */
    rawInputs.gridSize= ui.gridSize.value; rawInputs.startTime= ui.startTime.value;
    rawInputs.endTime= ui.endTime.value; rawInputs.timeSlotMinutes= ui.timeSlotMinutes.value;
    rawInputs.minLongitude= ui.minLongitude.value; rawInputs.maxLatitude= ui.maxLatitude.value;
    rawInputs.maxLongitude= ui.maxLongitude.value; rawInputs.minLatitude= ui.minLatitude.value;
    const currentParams = {}; const errors = []; /* 同您工作版本 */
    currentParams.gridSize = Number.parseFloat(rawInputs.gridSize);
    if (isNaN(currentParams.gridSize) || currentParams.gridSize <= 0) errors.push("网格大小必须为有效的正数。");
    currentParams.timeSlotMinutes = Number.parseInt(rawInputs.timeSlotMinutes, 10);
    if (isNaN(currentParams.timeSlotMinutes) || currentParams.timeSlotMinutes <= 0) errors.push("时间间隔必须为有效的正整数。");
    if (!rawInputs.startTime) errors.push("开始时间不能为空。"); else currentParams.startTime = rawInputs.startTime;
    if (!rawInputs.endTime) errors.push("结束时间不能为空。"); else currentParams.endTime = rawInputs.endTime;
    if (currentParams.startTime && currentParams.endTime && currentParams.startTime >= currentParams.endTime) errors.push("结束时间必须晚于开始时间。");
    const geoFields = { minLongitude: "最小经度", maxLatitude: "最大纬度", maxLongitude: "最大经度", minLatitude: "最小纬度",};
    for (const key in geoFields) {
      if (!rawInputs[key]) { errors.push(`${geoFields[key]}不能为空。`); currentParams[key] = Number.NaN;}
      else { currentParams[key] = Number.parseFloat(rawInputs[key]); if (isNaN(currentParams[key])) errors.push(`${geoFields[key]}必须为有效的数字。`);}
    }
    if (!isNaN(currentParams.minLongitude) && !isNaN(currentParams.maxLongitude) && currentParams.minLongitude >= currentParams.maxLongitude) errors.push("最小经度必须小于最大经度。");
    if (!isNaN(currentParams.minLatitude) && !isNaN(currentParams.maxLatitude) && currentParams.minLatitude >= currentParams.maxLatitude) errors.push("最小纬度必须小于最大纬度。");
    const lonLatRangeCheck = (val, min, max, name) => { if (!isNaN(val) && (val < min || val > max)) errors.push(`${name}必须在 ${min} 和 ${max} 之间。`);};
    lonLatRangeCheck(currentParams.minLongitude, -180, 180, "最小经度"); lonLatRangeCheck(currentParams.maxLongitude, -180, 180, "最大经度");
    lonLatRangeCheck(currentParams.minLatitude, -90, 90, "最小纬度"); lonLatRangeCheck(currentParams.maxLatitude, -90, 90, "最大纬度");
    if (errors.length > 0) {
      alert("输入参数错误：\n" + errors.join("\n"));
      displayError("输入参数校验失败，请检查。<br>" + errors.map(escapeHtml).join("<br>"));
      return;
    }
    appState.currentQueryInputParams = currentParams;
    // console.log("F4-DENSITY: 发送到后端的请求参数:", currentParams);

    try {
      showLoading(true, "正在分析密度数据...");
      const resultData = await fetchDensityData(currentParams);
      // console.log("F4-DENSITY: 从后端接收到的数据:", JSON.parse(JSON.stringify(resultData)));

      if (!resultData || typeof resultData.densityMap !== "object" || !Array.isArray(resultData.timeSlots) || resultData.rows == null || resultData.cols == null ) {
        throw new Error("返回的数据格式不正确或缺少必要字段。");
      }
      appState.densityResult = resultData;

      // 计算全局有效最大密度 (保持您工作版本中的逻辑)
      const allNonZeroDensities = []; /* ... */ // 同您工作版本
      if (appState.densityResult.densityMap) {
        Object.values(appState.densityResult.densityMap).forEach((slotData) => {
          if (typeof slotData === "object" && slotData !== null) {
            Object.values(slotData).forEach((rawValue) => {
              const density = Number(rawValue);
              if (!isNaN(density) && density > 0) allNonZeroDensities.push(density);
            });}});
      }
      if (allNonZeroDensities.length > 0) {
        allNonZeroDensities.sort((a, b) => a - b); let index = Math.floor(allNonZeroDensities.length * 0.98);
        index = Math.min(index, allNonZeroDensities.length - 1); index = Math.max(0, index);
        appState.globalEffectiveMaxDensity = allNonZeroDensities[index];
        if (appState.globalEffectiveMaxDensity === 0 && allNonZeroDensities.length > 0 && allNonZeroDensities[allNonZeroDensities.length - 1] > 0) {
          appState.globalEffectiveMaxDensity = allNonZeroDensities[allNonZeroDensities.length - 1];
        }
      } else { appState.globalEffectiveMaxDensity = 0; }
      // console.log(`F4-DENSITY: Global Effective Max Density: ${appState.globalEffectiveMaxDensity}`);

      // 绘制网格（覆盖物会被添加到 window.allFeatureOverlays）
      mapVisualizer.drawGrid(resultData, currentParams);
      if (currentParams.minLongitude != null) { mapVisualizer.fitMapToArea(currentParams); }

      const timeSlotsExist = resultData.timeSlots && resultData.timeSlots.length > 0;
      const densityDataExistsInMap = Object.values(resultData.densityMap || {}).some(slot => Object.keys(slot || {}).length > 0);

      if (timeSlotsExist && densityDataExistsInMap) {
        mapVisualizer.updateHeatmapForTimeSlot(0);
        displayResultsInfo(`密度分析成功，共 ${resultData.timeSlots.length} 个时间点。`);
      } else {
        displayResultsInfo("分析完成。在指定条件下未找到有效的密度数据。");
      }
      mapVisualizer.addLegend(); // 添加图例（会存到 window.allFeatureOverlays）
      showLoading(false);

    } catch (error) {
      console.error("F4-DENSITY: 密度分析过程中发生错误:", error);
      if (typeof displayFetchError === 'function') { // 优先使用全局的错误显示
        displayFetchError(error, "f4_result", "F4区域车流密度分析");
      } else {
        displayError(error.message || "分析失败。");
      }
      showLoading(false);
      if (typeof clearOverlays === "function") { clearOverlays(); } // 确保错误时也清理
    }
  }

  // --- 8. 事件监听器设置 ---
  function setupEventListeners() { /* ...保持您工作版本中的实现... */
    if (!ui.analyzeBtn && !ui.prevTimeSlotBtn && !ui.nextTimeSlotBtn) { console.error("F4-DENSITY: 关键UI按钮未能获取。"); return; }
    if (ui.analyzeBtn) { ui.analyzeBtn.addEventListener("click", handleAnalyzeDensity); }
    if (ui.prevTimeSlotBtn) {
      ui.prevTimeSlotBtn.addEventListener("click", () => {
        if ( appState.densityResult && appState.densityResult.timeSlots && appState.densityResult.timeSlots.length > 0 && appState.currentTimeIndex > 0 ) {
          mapVisualizer.updateHeatmapForTimeSlot(appState.currentTimeIndex - 1);
        }
      });
    }
    if (ui.nextTimeSlotBtn) {
      ui.nextTimeSlotBtn.addEventListener("click", () => {
        if ( appState.densityResult && appState.densityResult.timeSlots && appState.densityResult.timeSlots.length > 0 && appState.currentTimeIndex < appState.densityResult.timeSlots.length - 1) {
          mapVisualizer.updateHeatmapForTimeSlot(appState.currentTimeIndex + 1);
        }
      });
    }
  }

  // --- 9. 初始化 ---
  document.addEventListener("DOMContentLoaded", () => {
    // 填充UI元素引用
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

    // 再次确保 F4 在 allFeatureOverlays 中的条目存在
    if (typeof window.allFeatureOverlays !== 'object' || window.allFeatureOverlays === null) {
      window.allFeatureOverlays = {};
    }
    window.allFeatureOverlays["F4_查询边界"] = null;
    window.allFeatureOverlays["F4_密度单元格"] = [];
    window.allFeatureOverlays["F4_图例"] = null;

    let mapCheckIntervalF4_local; // 使用局部变量避免与外部冲突
    const MAX_MAP_WAIT_TIME_F4_local = 10000;
    const MAP_CHECK_INTERVAL_MS_F4_local = 200;
    let initializeAttemptedF4_local = false;

    function initializeMapDependentComponentsF4_local_scoped(mapInstance) {
      if(initializeAttemptedF4_local) return;
      initializeAttemptedF4_local = true;
      if (!mapVisualizer.initialize(mapInstance)) {
        displayError("F4: 地图可视化组件初始化失败。"); return;
      }
      setupEventListeners();
      displayResultsInfo('请设置参数并点击"分析区域车流密度"按钮开始。');
    }
    function tryInitializeMapLogicF4_local_scoped() {
      if (typeof window.map !== "undefined" && window.map instanceof BMapGL.Map && typeof window.map.getCenter === "function" && window.map.getCenter()) {
        clearInterval(mapCheckIntervalF4_local);
        initializeMapDependentComponentsF4_local_scoped(window.map);
      }
    }
    mapCheckIntervalF4_local = setInterval(tryInitializeMapLogicF4_local_scoped, MAP_CHECK_INTERVAL_MS_F4_local);
    setTimeout(() => {
      clearInterval(mapCheckIntervalF4_local);
      if (!initializeAttemptedF4_local) {
        if (typeof window.map !== "undefined" && window.map instanceof BMapGL.Map && typeof window.map.getCenter === "function" && window.map.getCenter()) {
          initializeMapDependentComponentsF4_local_scoped(window.map);
        } else {
          console.error(`F4-DENSITY: 全局 'map' 实例在 ${MAX_MAP_WAIT_TIME_F4_local / 1000} 秒后仍未可用。`);
          displayError(`地图服务在 ${MAX_MAP_WAIT_TIME_F4_local / 1000} 秒后仍不可用。`);
          if(ui.analyzeBtn) ui.analyzeBtn.disabled = true;
        }
      }
    }, MAX_MAP_WAIT_TIME_F4_local);
  });

  // window.mapVisualizer = mapVisualizer; // 不再需要全局暴露整个 mapVisualizer
  // 如果 map-utils.js 需要调用 F4 的特定清除（现在不需要了，因为它会处理 allFeatureOverlays），
  // 我们可以考虑只暴露一个 window.clearF4Overlays 函数。
  // 但根据当前“全局clearOverlays处理allFeatureOverlays”的策略，F4模块不需要暴露自己的清除函数。

})(BMapGL, window, document);