/**
 * Taxi Trajectory Density Analysis Frontend Logic (F4)
 *
 * Optimizes visual effects by using a global effective max density (percentile-based)
 * and a refined multi-step color scale.
 * Assumes Baidu Maps API (BMapGL) is loaded and Heatmap library if HeatmapOverlay is used (not used in this version).
 * This script will poll for the global `map` instance initialized by another script (e.g., main.js).
 * Assumes specific HTML element IDs for inputs and controls.
 */
;((BMapGL, window, document) => {
  // --- 1. 配置信息 ---
  const API_BASE_URL = window.location.hostname === "localhost" ? "http://localhost:8080" : ""
  const API_ENDPOINT = `${API_BASE_URL}/densityAnalysis/analyze`

  // --- 2. DOM 元素引用 (在DOMContentLoaded中填充) ---
  const ui = {
    startTime: null,
    endTime: null,
    minLongitude: null,
    maxLatitude: null,
    maxLongitude: null,
    minLatitude: null,
    gridSize: null,
    timeSlotMinutes: null,
    analyzeBtn: null,
    resultDiv: null,
    commonResultDiv: null,
    currentTimeSlotDisplay: null,
    prevTimeSlotBtn: null,
    nextTimeSlotBtn: null,
    mapContainerParent: null,
  }

  // --- 3. 应用状态管理 ---
  const appState = {
    mapInstance: null,
    densityResult: null,
    currentQueryInputParams: null,
    currentTimeIndex: 0,
    densityOverlays: [],
    gridCellPolygons: new Map(),
    globalEffectiveMaxDensity: 0,
  }

  // --- 4. API 服务 ---
  async function fetchDensityData(params) {
    if (ui.commonResultDiv) ui.commonResultDiv.innerHTML = ""
    showLoading(true, "正在向服务器请求分析数据...")
    try {
      const response = await fetch(API_ENDPOINT, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(params),
      })

      const responseData = await response.json()

      if (!response.ok) {
        let errorMsg = `网络响应异常，状态码: ${response.status}`
        if (responseData && responseData.message) {
          errorMsg = responseData.message
        }
        if (responseData && responseData.errors && Array.isArray(responseData.errors)) {
          errorMsg += " (详情: " + responseData.errors.map((e) => e.defaultMessage || e.field).join(", ") + ")"
        }
        throw new Error(errorMsg)
      }
      return responseData
    } finally {
      showLoading(false)
    }
  }

  // --- 5. 地图可视化模块 ---
  const mapVisualizer = {
    initialize(mapInstanceFromGlobal) {
      if (!mapInstanceFromGlobal || !(mapInstanceFromGlobal instanceof BMapGL.Map)) {
        // 确保是BMapGL的实例
        console.error("Density Analysis (F4): Baidu Map instance is not available or invalid for visualizer.")
        return false
      }
      appState.mapInstance = mapInstanceFromGlobal
      console.log("Density Analysis (F4): MapVisualizer initialized with map instance.")
      return true
    },

    clearAllOverlays() {
      if (!appState.mapInstance) return
      appState.densityOverlays.forEach((overlay) => {
        if (overlay instanceof HTMLElement && overlay.id === "density-legend" && overlay.parentNode) {
          overlay.parentNode.removeChild(overlay)
        } else if (typeof appState.mapInstance.removeOverlay === "function") {
          appState.mapInstance.removeOverlay(overlay)
        }
      })
      appState.densityOverlays = []
      appState.gridCellPolygons.clear()
      console.log("Density Analysis (F4): All density overlays cleared.")
    },

    drawGrid(gridResultData, queryInputParams) {
      if (!appState.mapInstance || !gridResultData || !queryInputParams) {
        console.error("Density Analysis (F4): Missing map instance, grid data, or query params for drawGrid.")
        return
      }

      const { rows, cols } = gridResultData
      const { minLongitude, minLatitude, maxLongitude, maxLatitude } = queryInputParams

      if (
        !Number.isFinite(minLongitude) ||
        !Number.isFinite(minLatitude) ||
        !Number.isFinite(maxLongitude) ||
        !Number.isFinite(maxLatitude) ||
        rows == null ||
        cols == null
      ) {
        console.warn(
          "Density Analysis (F4): Grid dimensions or query input bounds are invalid for drawing.",
          gridResultData,
          queryInputParams,
        )
        displayError("返回的网格维度或查询边界无效，无法绘制。")
        return
      }

      // 绘制总边界 (即使行列数为0也绘制，以便用户看到查询范围)
      if (Number.isFinite(minLongitude)) {
        const borderPoints = [
          new BMapGL.Point(minLongitude, maxLatitude),
          new BMapGL.Point(maxLongitude, maxLatitude),
          new BMapGL.Point(maxLongitude, minLatitude),
          new BMapGL.Point(minLongitude, minLatitude),
          new BMapGL.Point(minLongitude, maxLatitude),
        ]
        const borderPolygon = new BMapGL.Polygon(borderPoints, {
          strokeColor: "#1E90FF",
          strokeWeight: 2,
          strokeOpacity: 0.9,
          fillOpacity: 0,
        })
        appState.mapInstance.addOverlay(borderPolygon)
        appState.densityOverlays.push(borderPolygon)
      }

      if (rows === 0 || cols === 0) {
        console.warn("Density Analysis (F4): Grid has zero rows or columns. Only drawing boundary.")
        displayResultsInfo("分析区域有效，但网格行列数为0，无法进一步细分显示单元格。")
        return // 不绘制小单元格
      }

      const cellWidth = (maxLongitude - minLongitude) / cols
      const cellHeight = (maxLatitude - minLatitude) / rows

      for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
          const cellMinLon = minLongitude + c * cellWidth
          const cellMaxLat = maxLatitude - r * cellHeight
          const cellMaxLon = cellMinLon + cellWidth
          const cellMinLat = cellMaxLat - cellHeight

          const cellPoints = [
            new BMapGL.Point(cellMinLon, cellMaxLat),
            new BMapGL.Point(cellMaxLon, cellMaxLat),
            new BMapGL.Point(cellMaxLon, cellMinLat),
            new BMapGL.Point(cellMinLon, cellMinLat),
            new BMapGL.Point(cellMinLon, cellMaxLat),
          ]
          const cellPolygon = new BMapGL.Polygon(cellPoints, {
            strokeColor: "#DDDDDD",
            strokeWeight: 0.5,
            strokeOpacity: 0.4,
            fillColor: "#FFFFFF",
            fillOpacity: 0.1,
          })
          appState.mapInstance.addOverlay(cellPolygon)
          appState.densityOverlays.push(cellPolygon)
          appState.gridCellPolygons.set(`${r},${c}`, cellPolygon)
        }
      }
      console.log("Density Analysis (F4): Grid drawn on map using input bounds and result rows/cols.")
    },

    updateHeatmapForTimeSlot(timeIndex) {
      if (
        !appState.mapInstance ||
        !appState.densityResult ||
        !appState.densityResult.timeSlots ||
        appState.densityResult.timeSlots.length === 0
      ) {
        console.warn("[F4 DEBUG] mapVisualizer.updateHeatmapForTimeSlot: 缺少数据或地图实例，提前返回。", appState)
        return
      }

      appState.currentTimeIndex = timeIndex
      const timeSlotKeyFromList = appState.densityResult.timeSlots[timeIndex]

      let slotDataFromMap = appState.densityResult.densityMap[timeSlotKeyFromList]
      if (slotDataFromMap === undefined && timeSlotKeyFromList.endsWith(":00")) {
        const timeSlotKeyWithoutSeconds = timeSlotKeyFromList.substring(0, timeSlotKeyFromList.length - 3)
        slotDataFromMap = appState.densityResult.densityMap[timeSlotKeyWithoutSeconds]
      }

      const slotDensityData = slotDataFromMap || {}

      // 调试信息
      const currentSlotDensities = Object.values(slotDensityData)
        .map(Number)
        .filter((d) => !isNaN(d) && d > 0)
      console.log(`[F4 DENSITY DEBUG] Time Slot: ${timeSlotKeyFromList}`)
      console.log(
        `[F4 DENSITY DEBUG] Non-zero densities in this slot: ${currentSlotDensities.length}, Max in this slot: ${currentSlotDensities.length > 0 ? Math.max(...currentSlotDensities) : 0}`,
      )
      console.log(
        `[F4 DENSITY DEBUG] Using globalEffectiveMaxDensity for normalization: ${appState.globalEffectiveMaxDensity}`,
      )

      appState.gridCellPolygons.forEach((polygon, cellId) => {
        const rawDensityValue = slotDensityData[cellId]
        const density = rawDensityValue !== undefined && !isNaN(Number(rawDensityValue)) ? Number(rawDensityValue) : 0

        let normalizedDensity = 0
        if (appState.globalEffectiveMaxDensity > 0) {
          normalizedDensity = Math.min(density, appState.globalEffectiveMaxDensity) / appState.globalEffectiveMaxDensity
        }

        const color = this.getColorForDensity(normalizedDensity, density)
        polygon.setFillColor(color)
        polygon.setFillOpacity(0.75)
      })
      updateTimeDisplay()
    },

    getColorForDensity(normalizedDensity, actualDensity) {
      if (actualDensity === 0) return "#EFF3FF" // 非常浅的蓝灰色，比 #ADD8E6 更中性

      let r, g, b
      if (normalizedDensity <= 0) {
        r = 100
        g = 149
        b = 237 // CornflowerBlue
      } else if (normalizedDensity < 0.02) {
        r = 173
        g = 216
        b = 230 // LightBlue
      } else if (normalizedDensity < 0.1) {
        r = 100
        g = 149
        b = 237 // CornflowerBlue
      } else if (normalizedDensity < 0.25) {
        r = 0
        g = 191
        b = 255 // DeepSkyBlue
      } else if (normalizedDensity < 0.4) {
        r = 60
        g = 179
        b = 113 // MediumSeaGreen
      } else if (normalizedDensity < 0.6) {
        r = 255
        g = 255
        b = 0 // Yellow
      } else if (normalizedDensity < 0.8) {
        r = 255
        g = 165
        b = 0 // Orange
      } else {
        r = 255
        g = 69
        b = 0 // OrangeRed
      }
      return `rgb(${r},${g},${b})`
    },

    addLegend() {
      if (!appState.mapInstance || !ui.mapContainerParent) {
        console.warn("Density Analysis (F4): Cannot add legend - Map instance or map container parent not found.")
        return
      }
      this.removeLegend()

      const legendDiv = document.createElement("div")
      legendDiv.id = "density-legend"
      Object.assign(legendDiv.style, {
        position: "absolute",
        bottom: "20px",
        right: "20px",
        backgroundColor: "rgba(255, 255, 255, 0.9)",
        padding: "10px",
        border: "1px solid #ccc",
        borderRadius: "5px",
        boxShadow: "0 2px 6px rgba(0,0,0,0.3)",
        zIndex: "1000",
        fontFamily: "Arial, sans-serif",
        fontSize: "12px",
      })
      legendDiv.innerHTML = `
                <div style="font-weight: bold; margin-bottom: 8px; text-align: center;">车流密度</div>
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
                </div>
            `
      ui.mapContainerParent.appendChild(legendDiv)
      appState.densityOverlays.push(legendDiv)
      console.log("Density Analysis (F4): Legend added/updated.")
    },

    removeLegend() {
      const legend = document.getElementById("density-legend")
      if (legend && legend.parentNode) {
        legend.parentNode.removeChild(legend)
        appState.densityOverlays = appState.densityOverlays.filter((o) => o !== legend)
      }
    },

    fitMapToArea(queryInputParams) {
      if (!appState.mapInstance || !queryInputParams) return
      const { minLongitude, minLatitude, maxLongitude, maxLatitude } = queryInputParams

      const sw = new BMapGL.Point(minLongitude, minLatitude)
      const ne = new BMapGL.Point(maxLongitude, maxLatitude)

      if (
        isNaN(sw.lng) ||
        isNaN(sw.lat) ||
        isNaN(ne.lng) ||
        isNaN(ne.lat) ||
        minLongitude === maxLongitude ||
        minLatitude === maxLatitude
      ) {
        console.warn(
          "Density Analysis (F4): Cannot fit map to area - Invalid or zero-area coordinates from input.",
          queryInputParams,
        )
        if (!isNaN(sw.lng) && !isNaN(sw.lat)) appState.mapInstance.setCenter(sw)
        return
      }
      appState.mapInstance.setViewport([sw, ne], {
        margins: [50, 20, 20, 20],
        enableAnimation: true,
        zoomFactor: -1,
      })
    },
  }

  // --- 6. UI 更新函数 ---
  function showLoading(isLoading, message = "正在处理...") {
    if (!ui.resultDiv) return
    ui.resultDiv.innerHTML = isLoading ? `<p>${message} <span class="loading-spinner"></span></p>` : ""
  }
  function displayResultsInfo(message) {
    if (!ui.resultDiv) return
    ui.resultDiv.innerHTML = `<p>${message}</p>`
  }
  function displayError(errorMessage) {
    if (!ui.resultDiv) {
      console.error("Density Analysis (F4): Error display failed, ui.resultDiv not found. Error: " + errorMessage)
      return
    }
    ui.resultDiv.innerHTML = `<p style="color: red; font-weight: bold;">错误：${errorMessage}</p>`
  }
  function updateTimeDisplay() {
    if (!ui.currentTimeSlotDisplay) return
    if (!appState.densityResult || !appState.densityResult.timeSlots || appState.densityResult.timeSlots.length === 0) {
      ui.currentTimeSlotDisplay.value = "无数据"
      return
    }
    const currentDateTimeISO = appState.densityResult.timeSlots[appState.currentTimeIndex]
    ui.currentTimeSlotDisplay.value = currentDateTimeISO.replace("T", " ")
  }

  // --- 7. 主要分析处理函数 ---
  async function handleAnalyzeDensity() {
    if (!appState.mapInstance) {
      displayError("地图尚未初始化或不可用。请刷新页面或检查地图配置。")
      console.error("Density Analysis (F4): handleAnalyzeDensity called but mapInstance is not ready.")
      return
    }

    // 7.1 获取并严格校验输入参数
    const rawInputs = {
      gridSize: ui.gridSize.value,
      startTime: ui.startTime.value,
      endTime: ui.endTime.value,
      timeSlotMinutes: ui.timeSlotMinutes.value,
      minLongitude: ui.minLongitude.value,
      maxLatitude: ui.maxLatitude.value,
      maxLongitude: ui.maxLongitude.value,
      minLatitude: ui.minLatitude.value,
    }
    const currentParams = {}
    const errors = []

    currentParams.gridSize = Number.parseFloat(rawInputs.gridSize)
    if (isNaN(currentParams.gridSize) || currentParams.gridSize <= 0) errors.push("网格大小必须为有效的正数。")

    currentParams.timeSlotMinutes = Number.parseInt(rawInputs.timeSlotMinutes, 10)
    if (isNaN(currentParams.timeSlotMinutes) || currentParams.timeSlotMinutes <= 0)
      errors.push("时间间隔必须为有效的正整数。")

    if (!rawInputs.startTime) errors.push("开始时间不能为空。")
    else currentParams.startTime = rawInputs.startTime
    if (!rawInputs.endTime) errors.push("结束时间不能为空。")
    else currentParams.endTime = rawInputs.endTime
    if (currentParams.startTime && currentParams.endTime && currentParams.startTime >= currentParams.endTime)
      errors.push("结束时间必须晚于开始时间。")

    const geoFields = {
      minLongitude: "最小经度",
      maxLatitude: "最大纬度",
      maxLongitude: "最大经度",
      minLatitude: "最小纬度",
    }
    for (const key in geoFields) {
      if (!rawInputs[key]) {
        errors.push(`${geoFields[key]}不能为空。`)
        currentParams[key] = Number.NaN
      } else {
        currentParams[key] = Number.parseFloat(rawInputs[key])
        if (isNaN(currentParams[key])) errors.push(`${geoFields[key]}必须为有效的数字。`)
      }
    }
    if (
      !isNaN(currentParams.minLongitude) &&
      !isNaN(currentParams.maxLongitude) &&
      currentParams.minLongitude >= currentParams.maxLongitude
    )
      errors.push("最小经度必须小于最大经度。")
    if (
      !isNaN(currentParams.minLatitude) &&
      !isNaN(currentParams.maxLatitude) &&
      currentParams.minLatitude >= currentParams.maxLatitude
    )
      errors.push("最小纬度必须小于最大纬度。")

    const lonLatRangeCheck = (val, min, max, name) => {
      if (!isNaN(val) && (val < min || val > max)) errors.push(`${name}必须在 ${min} 和 ${max} 之间。`)
    }
    lonLatRangeCheck(currentParams.minLongitude, -180, 180, "最小经度")
    lonLatRangeCheck(currentParams.maxLongitude, -180, 180, "最大经度")
    lonLatRangeCheck(currentParams.minLatitude, -90, 90, "最小纬度")
    lonLatRangeCheck(currentParams.maxLatitude, -90, 90, "最大纬度")

    if (errors.length > 0) {
      alert("输入参数错误：\n" + errors.join("\n"))
      displayError("输入参数校验失败，请检查。<br>" + errors.join("<br>"))
      console.log("[F4 VALIDATION DEBUG] Client-side validation failed. Errors:", errors, "Raw Inputs:", rawInputs)
      return
    }
    appState.currentQueryInputParams = currentParams
    console.log("Density Analysis (F4): 发送到后端的请求参数:", currentParams)

    try {
      mapVisualizer.clearAllOverlays()
      appState.densityResult = null
      appState.currentTimeIndex = 0
      appState.globalEffectiveMaxDensity = 0
      updateTimeDisplay()

      const resultData = await fetchDensityData(currentParams)
      console.log("Density Analysis (F4): 从后端接收到的简化数据:", JSON.parse(JSON.stringify(resultData)))

      if (
        !resultData ||
        typeof resultData.densityMap !== "object" ||
        !Array.isArray(resultData.timeSlots) ||
        resultData.rows == null ||
        resultData.cols == null
      ) {
        throw new Error("返回的数据格式不正确或缺少必要字段 (rows, cols, densityMap, timeSlots)。")
      }
      appState.densityResult = resultData

      // ---- 计算全局有效最大密度 ----
      const allNonZeroDensities = []
      if (appState.densityResult.densityMap) {
        Object.values(appState.densityResult.densityMap).forEach((slotData) => {
          if (typeof slotData === "object" && slotData !== null) {
            // 确保slotData是对象
            Object.values(slotData).forEach((rawValue) => {
              const density = Number(rawValue)
              if (!isNaN(density) && density > 0) {
                allNonZeroDensities.push(density)
              }
            })
          }
        })
      }

      if (allNonZeroDensities.length > 0) {
        allNonZeroDensities.sort((a, b) => a - b)
        const percentile = 0.98
        let index = Math.floor(allNonZeroDensities.length * percentile)
        index = Math.min(index, allNonZeroDensities.length - 1) // Cap at last element
        index = Math.max(0, index) // Ensure index is not negative
        appState.globalEffectiveMaxDensity = allNonZeroDensities[index]

        if (appState.globalEffectiveMaxDensity === 0 && allNonZeroDensities[allNonZeroDensities.length - 1] > 0) {
          appState.globalEffectiveMaxDensity = allNonZeroDensities[allNonZeroDensities.length - 1]
        }
      } else {
        appState.globalEffectiveMaxDensity = 0
      }
      console.log(`[F4 GLOBAL DEBUG] All non-zero densities collected count: ${allNonZeroDensities.length}`)
      console.log(
        `[F4 GLOBAL DEBUG] Global Effective Max Density (e.g., 98th percentile): ${appState.globalEffectiveMaxDensity}`,
      )
      // ---- 全局有效最大密度计算结束 ----

      mapVisualizer.drawGrid(resultData, currentParams)

      if (currentParams.minLongitude != null) {
        mapVisualizer.fitMapToArea(currentParams)
      }

      if (
        resultData.timeSlots.length === 0 ||
        (allNonZeroDensities.length === 0 &&
          Object.keys(resultData.densityMap).every((key) => Object.keys(resultData.densityMap[key]).length === 0))
      ) {
        displayResultsInfo("分析完成。在指定条件下未找到有效的密度数据。")
        mapVisualizer.addLegend()
        return
      }

      mapVisualizer.updateHeatmapForTimeSlot(0)
      mapVisualizer.addLegend()

      displayResultsInfo(`密度分析成功，共 ${resultData.timeSlots.length} 个时间点。使用时间切换按钮查看。`)
    } catch (error) {
      console.error("Density Analysis (F4): 密度分析过程中发生错误:", error)
      displayError(error.message || "分析失败，请检查网络或联系管理员。")
    }
  }

  // --- 8. 事件监听器设置 ---
  function setupEventListeners() {
    if (!ui.analyzeBtn && !ui.prevTimeSlotBtn && !ui.nextTimeSlotBtn) {
      console.error("Density Analysis (F4): 关键UI按钮未能获取，事件监听器无法设置。请检查HTML ID。")
      return
    }
    if (ui.analyzeBtn) {
      ui.analyzeBtn.addEventListener("click", handleAnalyzeDensity)
    } else {
      console.error("Density Analysis (F4): 分析按钮 'densityAnalysisBtn' 未在DOM中找到。")
    }
    if (ui.prevTimeSlotBtn) {
      ui.prevTimeSlotBtn.addEventListener("click", () => {
        if (
          appState.densityResult &&
          appState.densityResult.timeSlots &&
          appState.densityResult.timeSlots.length > 0 &&
          appState.currentTimeIndex > 0
        ) {
          mapVisualizer.updateHeatmapForTimeSlot(appState.currentTimeIndex - 1)
        } else {
          console.log("Density Analysis (F4): 无法切换到上一个时间槽。")
        }
      })
    } else {
      console.warn("Density Analysis (F4): \"上一时间槽\"按钮 'prevTimeSlot' 未找到。")
    }
    if (ui.nextTimeSlotBtn) {
      ui.nextTimeSlotBtn.addEventListener("click", () => {
        if (
          appState.densityResult &&
          appState.densityResult.timeSlots &&
          appState.densityResult.timeSlots.length > 0 &&
          appState.currentTimeIndex < appState.densityResult.timeSlots.length - 1
        ) {
          mapVisualizer.updateHeatmapForTimeSlot(appState.currentTimeIndex + 1)
        } else {
          console.log("Density Analysis (F4): 无法切换到下一个时间槽。")
        }
      })
    } else {
      console.warn("Density Analysis (F4): \"下一时间槽\"按钮 'nextTimeSlot' 未找到。")
    }
    console.log("Density Analysis (F4): Event listeners set up.")
  }

  // --- 9. 初始化 (轮询逻辑) ---
  document.addEventListener("DOMContentLoaded", () => {
    console.log("Density Analysis (F4): DOMContentLoaded 事件触发。")
    ui.startTime = document.getElementById("f4_startTime")
    ui.endTime = document.getElementById("f4_endTime")
    ui.minLongitude = document.getElementById("f4_topLeftLng")
    ui.maxLatitude = document.getElementById("f4_topLeftLat")
    ui.maxLongitude = document.getElementById("f4_bottomRightLng")
    ui.minLatitude = document.getElementById("f4_bottomRightLat")
    ui.gridSize = document.getElementById("gridRadius")
    ui.timeSlotMinutes = document.getElementById("timeInterval")
    ui.analyzeBtn = document.getElementById("densityAnalysisBtn")
    ui.resultDiv = document.getElementById("f4_result")
    ui.commonResultDiv = document.getElementById("result")
    ui.currentTimeSlotDisplay = document.getElementById("currentTimeSlot")
    ui.prevTimeSlotBtn = document.getElementById("prevTimeSlot")
    ui.nextTimeSlotBtn = document.getElementById("nextTimeSlot")
    ui.mapContainerParent = document.getElementById("container")

    if (!ui.resultDiv) {
      const f4Container = document.getElementById("f4")
      if (f4Container) {
        const newResultDiv = document.createElement("div")
        newResultDiv.id = "f4_result"
        newResultDiv.className = "function-result"
        f4Container.appendChild(newResultDiv)
        ui.resultDiv = newResultDiv
        console.log("Density Analysis (F4): #f4_result div 已动态创建。")
      } else {
        console.error("Density Analysis (F4): 无法找到父容器 #f4 来创建 #f4_result div。")
      }
    }
    if (!ui.analyzeBtn) {
      console.error("Density Analysis (F4): 分析按钮未找到，功能可能无法使用。")
      if (ui.resultDiv) displayError("页面初始化不完整（缺少分析按钮），功能可能无法使用。")
      return
    }
    console.log("Density Analysis (F4): UI 元素引用已填充。开始轮询地图实例...")

    let mapCheckInterval
    let mapCheckTimeout
    const MAX_MAP_WAIT_TIME = 10000
    const MAP_CHECK_INTERVAL_MS = 200

    function initializeMapDependentComponents(mapInstance) {
      if (!mapVisualizer.initialize(mapInstance)) {
        displayError("地图可视化组件初始化失败。")
        return false
      }
      setupEventListeners()
      displayResultsInfo('请设置参数并点击"分析区域车流密度"按钮开始。')
      console.log("Density Analysis (F4): 地图相关组件已成功初始化。")
      return true
    }
    function tryInitializeMapLogic() {
      if (
        typeof window.map !== "undefined" &&
        window.map instanceof BMapGL.Map &&
        typeof window.map.getCenter === "function" &&
        window.map.getCenter()
      ) {
        console.log("Density Analysis (F4): 全局 'map' 实例找到并已准备就绪。")
        clearInterval(mapCheckInterval)
        clearTimeout(mapCheckTimeout)
        initializeMapDependentComponents(window.map)
      }
    }
    mapCheckInterval = setInterval(tryInitializeMapLogic, MAP_CHECK_INTERVAL_MS)
    mapCheckTimeout = setTimeout(() => {
      clearInterval(mapCheckInterval)
      if (
        typeof window.map !== "undefined" &&
        window.map instanceof BMapGL.Map &&
        typeof window.map.getCenter === "function" &&
        window.map.getCenter()
      ) {
        if (!appState.mapInstance) {
          initializeMapDependentComponents(window.map)
        }
      } else {
        if (!appState.mapInstance) {
          console.error(`Density Analysis (F4): 全局 'map' 实例在 ${MAX_MAP_WAIT_TIME / 1000} 秒后仍未可用。`)
          displayError(`地图服务在 ${MAX_MAP_WAIT_TIME / 1000} 秒后仍不可用。请确保地图已正确加载或刷新页面。`)
        }
      }
    }, MAX_MAP_WAIT_TIME)
  })

  // 将 mapVisualizer 暴露到全局作用域，以便其他脚本可以访问
  window.mapVisualizer = mapVisualizer
})(BMapGL, window, document)
