
(function (BMapGL, window, document) {
    'use strict';

    // --- 1. 配置信息 ---
    const API_BASE_URL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
    const API_ENDPOINT = `${API_BASE_URL}/densityAnalysis/analyze`;

    // --- 2. DOM 元素引用 (在DOMContentLoaded中填充) ---
    const ui = {
        startTime: null, endTime: null, minLongitude: null, maxLatitude: null,
        maxLongitude: null, minLatitude: null, gridSize: null, timeSlotMinutes: null,
        analyzeBtn: null, resultDiv: null, commonResultDiv: null,
        currentTimeSlotDisplay: null, prevTimeSlotBtn: null, nextTimeSlotBtn: null,
        mapContainerParent: null // 地图的父容器，图例会添加到这里
    };

    // --- 3. 应用状态管理 ---
    let appState = {
        mapInstance: null,
        densityResult: null,
        currentQueryInputParams: null, // 存储当次查询的用户输入参数，用于绘图
        currentTimeIndex: 0,
        densityOverlays: [],
        gridCellPolygons: new Map()
    };

    // --- 4. API 服务 ---
    async function fetchDensityData(params) {
        if (ui.commonResultDiv) ui.commonResultDiv.innerHTML = "";
        showLoading(true, "正在向服务器请求分析数据...");
        try {
            const response = await fetch(API_ENDPOINT, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(params)
            });

            const responseData = await response.json();

            if (!response.ok) {
                let errorMsg = responseData.message || `网络响应异常，状态码: ${response.status}`;
                if (responseData.errors && Array.isArray(responseData.errors)) {
                    errorMsg += ": " + responseData.errors.map(e => e.defaultMessage || e.field).join(', ');
                }
                throw new Error(errorMsg);
            }
            return responseData;
        } finally {
            showLoading(false);
        }
    }

    // --- 5. 地图可视化模块 ---
    const mapVisualizer = {
        initialize(mapInstanceFromGlobal) {
            if (!mapInstanceFromGlobal) {
                console.error("Density Analysis (F4): Baidu Map instance is not available for visualizer.");
                return false;
            }
            appState.mapInstance = mapInstanceFromGlobal;
            console.log("Density Analysis (F4): MapVisualizer initialized with map instance.");
            return true;
        },

        clearAllOverlays() {
            if (!appState.mapInstance) return;
            appState.densityOverlays.forEach(overlay => {
                if (overlay instanceof HTMLElement && overlay.id === 'density-legend' && overlay.parentNode) {
                    overlay.parentNode.removeChild(overlay);
                } else if (typeof appState.mapInstance.removeOverlay === 'function') {
                    appState.mapInstance.removeOverlay(overlay);
                }
            });
            appState.densityOverlays = [];
            appState.gridCellPolygons.clear();
            console.log("Density Analysis (F4): All density overlays cleared.");
        },

        drawGrid(gridResultData, queryInputParams) {
            if (!appState.mapInstance || !gridResultData || !queryInputParams) {
                console.error("Density Analysis (F4): Missing map instance, grid data, or query params for drawGrid.");
                return;
            }

            const { rows, cols } = gridResultData; // 从后端结果获取行数和列数
            // 从用户原始输入获取地理边界 (这些是构建Grid的原始依据)
            const { minLongitude, minLatitude, maxLongitude, maxLatitude } = queryInputParams;

            if (!Number.isFinite(minLongitude) || !Number.isFinite(minLatitude) ||
                !Number.isFinite(maxLongitude) || !Number.isFinite(maxLatitude) ||
                rows == null || cols == null ) { // rows/cols 可以是0，但不能是null/undefined
                console.warn("Density Analysis (F4): Grid dimensions or query input bounds are invalid for drawing.", gridResultData, queryInputParams);
                displayError("返回的网格维度或查询边界无效，无法绘制。");
                return;
            }
            if (rows === 0 || cols === 0) {
                console.warn("Density Analysis (F4): Grid has zero rows or columns. Drawing boundary only.");
                // 即使行列为0，也尝试绘制查询的边界框
                if (Number.isFinite(minLongitude)) { // 确保边界值有效
                    const borderPoints = [
                        new BMapGL.Point(minLongitude, maxLatitude), new BMapGL.Point(maxLongitude, maxLatitude),
                        new BMapGL.Point(maxLongitude, minLatitude), new BMapGL.Point(minLongitude, minLatitude),
                        new BMapGL.Point(minLongitude, maxLatitude)
                    ];
                    const borderPolygon = new BMapGL.Polygon(borderPoints, {
                        strokeColor: "#1E90FF", strokeWeight: 2, strokeOpacity: 0.9, fillOpacity: 0
                    });
                    appState.mapInstance.addOverlay(borderPolygon);
                    appState.densityOverlays.push(borderPolygon);
                }
                displayResultsInfo("分析区域有效，但网格行列数为0，无法进一步细分显示单元格。");
                return;
            }

            const cellWidth = (maxLongitude - minLongitude) / cols;
            const cellHeight = (maxLatitude - minLatitude) / rows;

            const borderPoints = [
                new BMapGL.Point(minLongitude, maxLatitude), new BMapGL.Point(maxLongitude, maxLatitude),
                new BMapGL.Point(maxLongitude, minLatitude), new BMapGL.Point(minLongitude, minLatitude),
                new BMapGL.Point(minLongitude, maxLatitude)
            ];
            const borderPolygon = new BMapGL.Polygon(borderPoints, {
                strokeColor: "#1E90FF", strokeWeight: 2, strokeOpacity: 0.9, fillOpacity: 0
            });
            appState.mapInstance.addOverlay(borderPolygon);
            appState.densityOverlays.push(borderPolygon);

            for (let r = 0; r < rows; r++) {
                for (let c = 0; c < cols; c++) {
                    const cellMinLon = minLongitude + c * cellWidth;
                    const cellMaxLat = maxLatitude - r * cellHeight;
                    const cellMaxLon = cellMinLon + cellWidth;
                    const cellMinLat = cellMaxLat - cellHeight;

                    const cellPoints = [
                        new BMapGL.Point(cellMinLon, cellMaxLat), new BMapGL.Point(cellMaxLon, cellMaxLat),
                        new BMapGL.Point(cellMaxLon, cellMinLat), new BMapGL.Point(cellMinLon, cellMinLat),
                        new BMapGL.Point(cellMinLon, cellMaxLat)
                    ];
                    const cellPolygon = new BMapGL.Polygon(cellPoints, {
                        strokeColor: "#DDDDDD", strokeWeight: 0.5, strokeOpacity: 0.4,
                        fillColor: "#FFFFFF", fillOpacity: 0.1
                    });
                    appState.mapInstance.addOverlay(cellPolygon);
                    appState.densityOverlays.push(cellPolygon);
                    appState.gridCellPolygons.set(`${r},${c}`, cellPolygon);
                }
            }
            console.log("Density Analysis (F4): Grid drawn on map using input bounds and result rows/cols.");
        },

        // 在 mapVisualizer 对象内部
        updateHeatmapForTimeSlot(timeIndex) {
            console.log(`[F4 DEBUG] mapVisualizer.updateHeatmapForTimeSlot: 函数开始执行，timeIndex: ${timeIndex}`);

            if (!appState.mapInstance || !appState.densityResult || !appState.densityResult.timeSlots || appState.densityResult.timeSlots.length === 0) {
                console.warn("[F4 DEBUG] mapVisualizer.updateHeatmapForTimeSlot: 缺少数据或地图实例，提前返回。", appState);
                return;
            }

            appState.currentTimeIndex = timeIndex;
            // timeSlotKeyFromList 通常是带秒的, e.g., "2008-02-02T14:30:00"
            const timeSlotKeyFromList = appState.densityResult.timeSlots[timeIndex];

            console.log(`[F4 DETAIL DEBUG] Processing timeSlotKeyFromList: "${timeSlotKeyFromList}" (length: ${timeSlotKeyFromList.length}, type: ${typeof timeSlotKeyFromList})`);

            let slotDataFromMap = appState.densityResult.densityMap[timeSlotKeyFromList]; // 首先尝试用原始的、带秒的键

            // 如果用带秒的键找不到，并且它以 ":00" 结尾，则尝试去掉 ":00" 再找一次
            if (slotDataFromMap === undefined && timeSlotKeyFromList.endsWith(":00")) {
                const timeSlotKeyWithoutSeconds = timeSlotKeyFromList.substring(0, timeSlotKeyFromList.length - 3); // 去掉末尾的 ":00"
                console.log(`[F4 DETAIL DEBUG] Key "${timeSlotKeyFromList}" not found. Trying key without seconds: "${timeSlotKeyWithoutSeconds}"`);
                slotDataFromMap = appState.densityResult.densityMap[timeSlotKeyWithoutSeconds];

                if (slotDataFromMap === undefined) {
                    console.error(`[F4 CRITICAL DEBUG] Key "${timeSlotKeyFromList}" AND "${timeSlotKeyWithoutSeconds}" NOT FOUND in appState.densityResult.densityMap! Available keys:`, Object.keys(appState.densityResult.densityMap));
                } else {
                    console.log(`[F4 DETAIL DEBUG] Successfully found data using key without seconds: "${timeSlotKeyWithoutSeconds}"`);
                }
            } else if (slotDataFromMap !== undefined) {
                console.log(`[F4 DETAIL DEBUG] Successfully found data using key with seconds: "${timeSlotKeyFromList}"`);
            } else {
                // slotDataFromMap is undefined and timeSlotKeyFromList does not end with ":00"
                console.error(`[F4 CRITICAL DEBUG] Key "${timeSlotKeyFromList}" NOT FOUND and does not end with ':00'. Available keys:`, Object.keys(appState.densityResult.densityMap));
            }

            console.log(`[F4 DETAIL DEBUG] slotDataFromMap (after attempting key variations):`, slotDataFromMap ? JSON.parse(JSON.stringify(slotDataFromMap)) : undefined);
            if (slotDataFromMap) {
                console.log(`[F4 DETAIL DEBUG] typeof slotDataFromMap: ${typeof slotDataFromMap}`);
                console.log(`[F4 DETAIL DEBUG] Is slotDataFromMap an Array? ${Array.isArray(slotDataFromMap)}`);
                console.log(`[F4 DETAIL DEBUG] Keys in slotDataFromMap (using Object.keys):`, Object.keys(slotDataFromMap));
            }

            const slotDensityData = slotDataFromMap || {};

            let maxDensity = 0;
            const numericDensitiesInSlot = [];

            for (const cellKey in slotDensityData) {
                if (slotDensityData.hasOwnProperty(cellKey)) {
                    const rawValue = slotDensityData[cellKey];
                    const density = Number(rawValue);

                    if (!isNaN(density)) {
                        numericDensitiesInSlot.push(density);
                        if (density > maxDensity) {
                            maxDensity = density;
                        }
                    } else {
                        console.warn(`[F4 DENSITY WARNING] Invalid non-numeric density value found for cell ${cellKey}:`, rawValue);
                    }
                }
            }

            const nonZeroDensities = numericDensitiesInSlot.filter(d => d > 0).sort((a, b) => a - b);

            // 现在使用 timeSlotKeyFromList (带秒的) 作为日志和UI显示的时间，保持一致性
            console.log(`[F4 DENSITY DEBUG] Time Slot: ${timeSlotKeyFromList}`);
            console.log(`[F4 DENSITY DEBUG] Max Density in this slot (after Number() conversion): ${maxDensity}`);
            console.log(`[F4 DENSITY DEBUG] Non-zero densities count (after Number() conversion): ${nonZeroDensities.length}`);
            if (nonZeroDensities.length > 0) {
                console.log(`[F4 DENSITY DEBUG] Min non-zero density: ${nonZeroDensities[0]}`);
                console.log(`[F4 DENSITY DEBUG] Median non-zero density: ${nonZeroDensities[Math.floor(nonZeroDensities.length / 2)]}`);
                console.log(`[F4 DENSITY DEBUG] A few sample non-zero densities:`, nonZeroDensities.slice(0, Math.min(20, nonZeroDensities.length)));
            }

            appState.gridCellPolygons.forEach((polygon, cellId) => {
                const rawDensityValue = slotDensityData[cellId];
                const density = (rawDensityValue !== undefined && !isNaN(Number(rawDensityValue))) ? Number(rawDensityValue) : 0;

                let normalizedDensity = 0;
                if (maxDensity > 0) {
                    normalizedDensity = density / maxDensity;
                }

                const color = this.getColorForDensity(normalizedDensity, density);
                polygon.setFillColor(color);
                polygon.setFillOpacity(0.7);
            });
            updateTimeDisplay(); // updateTimeDisplay 内部也使用 appState.densityResult.timeSlots[appState.currentTimeIndex] 来获取时间
        },

        getColorForDensity(normalizedDensity, actualDensity) {
            if (actualDensity === 0) return "#ADD8E6";
            let r, g, b;
            if (normalizedDensity < 0.25) {
                const p = normalizedDensity / 0.25;
                r = 0; g = Math.round(p * 255); b = Math.round((1 - p) * 255);
            } else if (normalizedDensity < 0.5) {
                const p = (normalizedDensity - 0.25) / 0.25;
                r = Math.round(p * 255); g = 255; b = 0;
            } else if (normalizedDensity < 0.75) {
                const p = (normalizedDensity - 0.5) / 0.25;
                r = 255; g = Math.round(255 - p * (255 - 165)); b = 0;
            } else {
                const p = (normalizedDensity - 0.75) / 0.25;
                r = 255; g = Math.round(165 - p * 165); b = 0;
            }
            return `rgb(${r},${g},${b})`;
        },

        addLegend() {
            if (!appState.mapInstance || !ui.mapContainerParent) {
                console.warn("Density Analysis (F4): Cannot add legend - Map instance or map container parent not found.");
                return;
            }
            this.removeLegend();

            const legendDiv = document.createElement("div");
            legendDiv.id = "density-legend";
            Object.assign(legendDiv.style, {
                position: "absolute", bottom: "20px", right: "20px",
                backgroundColor: "rgba(255, 255, 255, 0.9)", padding: "10px", border: "1px solid #ccc",
                borderRadius: "5px", boxShadow: "0 2px 6px rgba(0,0,0,0.3)", zIndex: "1000",
                fontFamily: "Arial, sans-serif", fontSize: "12px"
            });
            legendDiv.innerHTML = `
                <div style="font-weight: bold; margin-bottom: 8px; text-align: center;">车流密度</div>
                <div style="display: flex; align-items: center; margin-bottom: 4px;">
                    <div style="height: 15px; width: 15px; background-color: #ADD8E6; margin-right: 8px; border: 1px solid #999;"></div>
                    <span>零密度</span>
                </div>
                <div style="margin-bottom: 4px;">密度渐变:</div>
                <div style="height: 20px; width: 180px; background: linear-gradient(to right, blue, limegreen, yellow, orange, red); border: 1px solid #ccc; margin-bottom: 2px;"></div>
                <div style="display: flex; justify-content: space-between; width: 180px;">
                    <span>低</span><span>高</span>
                </div>
            `;
            ui.mapContainerParent.appendChild(legendDiv);
            appState.densityOverlays.push(legendDiv);
            console.log("Density Analysis (F4): Legend added.");
        },

        removeLegend() {
            const legend = document.getElementById("density-legend");
            if (legend && legend.parentNode) {
                legend.parentNode.removeChild(legend);
                appState.densityOverlays = appState.densityOverlays.filter(o => o !== legend);
            }
        },

        fitMapToArea(queryInputParams) { // 使用用户输入的边界参数
            if (!appState.mapInstance || !queryInputParams) return;
            const { minLongitude, minLatitude, maxLongitude, maxLatitude } = queryInputParams;

            const sw = new BMapGL.Point(minLongitude, minLatitude);
            const ne = new BMapGL.Point(maxLongitude, maxLatitude);

            if (isNaN(sw.lng) || isNaN(sw.lat) || isNaN(ne.lng) || isNaN(ne.lat) ||
                minLongitude === maxLongitude || minLatitude === maxLatitude) {
                console.warn("Density Analysis (F4): Cannot fit map to area - Invalid or zero-area coordinates from input.", queryInputParams);
                if (!isNaN(sw.lng) && !isNaN(sw.lat)) appState.mapInstance.setCenter(sw);
                return;
            }
            appState.mapInstance.setViewport([sw, ne], {
                margins: [50, 20, 20, 20],
                enableAnimation: true,
                zoomFactor: -1
            });
        }
    };

    // --- 6. UI 更新函数 ---
    function showLoading(isLoading, message = "正在处理...") {
        if (!ui.resultDiv) return;
        ui.resultDiv.innerHTML = isLoading ? `<p>${message} <span class="loading-spinner"></span></p>` : "";
        // 您可能需要为 .loading-spinner 添加CSS动画
    }
    function displayResultsInfo(message) {
        if (!ui.resultDiv) return;
        ui.resultDiv.innerHTML = `<p>${message}</p>`;
    }
    function displayError(errorMessage) {
        if (!ui.resultDiv) {
            console.error("Density Analysis (F4): Error display failed, ui.resultDiv not found. Error: " + errorMessage);
            return;
        }
        ui.resultDiv.innerHTML = `<p style="color: red; font-weight: bold;">错误：${errorMessage}</p>`;
    }
    function updateTimeDisplay() {
        if (!ui.currentTimeSlotDisplay) return;
        if (!appState.densityResult || !appState.densityResult.timeSlots || appState.densityResult.timeSlots.length === 0) {
            ui.currentTimeSlotDisplay.value = "无数据";
            return;
        }
        const currentDateTimeISO = appState.densityResult.timeSlots[appState.currentTimeIndex];
        ui.currentTimeSlotDisplay.value = currentDateTimeISO.replace("T", " ");
    }

    // --- 7. 主要分析处理函数 ---
    async function handleAnalyzeDensity() {
        if (!appState.mapInstance) {
            displayError("地图尚未初始化或不可用。请刷新页面或检查地图配置。");
            console.error("Density Analysis (F4): handleAnalyzeDensity called but mapInstance is not ready.");
            return;
        }

        // 1. 获取并校验输入参数
        const currentParams = { // 将用户输入存储起来，供绘图使用
            gridSize: parseFloat(ui.gridSize.value),
            startTime: ui.startTime.value,
            endTime: ui.endTime.value,
            timeSlotMinutes: parseInt(ui.timeSlotMinutes.value, 10),
            minLongitude: parseFloat(ui.minLongitude.value),
            maxLatitude: parseFloat(ui.maxLatitude.value),
            maxLongitude: parseFloat(ui.maxLongitude.value),
            minLatitude: parseFloat(ui.minLatitude.value)
        };
        appState.currentQueryInputParams = currentParams; // 保存当次查询的输入参数

        let errors = [];
        if (isNaN(currentParams.gridSize) || currentParams.gridSize <= 0) errors.push("网格大小必须为正数。");
        if (!currentParams.startTime) errors.push("开始时间不能为空。");
        if (!currentParams.endTime) errors.push("结束时间不能为空。");
        if (currentParams.startTime && currentParams.endTime && currentParams.startTime >= currentParams.endTime) errors.push("结束时间必须晚于开始时间。");
        if (isNaN(currentParams.timeSlotMinutes) || currentParams.timeSlotMinutes <= 0) errors.push("时间间隔必须为正数。");

        if (isNaN(currentParams.minLongitude)) errors.push("最小经度不能为空且必须为数字。");
        if (isNaN(currentParams.minLatitude)) errors.push("最小纬度不能为空且必须为数字。");
        if (isNaN(currentParams.maxLongitude)) errors.push("最大经度不能为空且必须为数字。");
        if (isNaN(currentParams.maxLatitude)) errors.push("最大纬度不能为空且必须为数字。");

        if (!isNaN(currentParams.minLongitude) && !isNaN(currentParams.maxLongitude) && currentParams.minLongitude >= currentParams.maxLongitude) {
            errors.push("最小经度必须小于最大经度。");
        }
        if (!isNaN(currentParams.minLatitude) && !isNaN(currentParams.maxLatitude) && currentParams.minLatitude >= currentParams.maxLatitude) {
            errors.push("最小纬度必须小于最大纬度。");
        }
        const lonLatCheck = (val, min, max, name) => { if (!isNaN(val) && (val < min || val > max)) errors.push(`${name}必须在 ${min} 和 ${max} 之间。`);};
        lonLatCheck(currentParams.minLongitude, -180, 180, "最小经度"); lonLatCheck(currentParams.maxLongitude, -180, 180, "最大经度");
        lonLatCheck(currentParams.minLatitude, -90, 90, "最小纬度"); lonLatCheck(currentParams.maxLatitude, -90, 90, "最大纬度");

        if (errors.length > 0) {
            alert("输入参数错误：\n" + errors.join("\n"));
            displayError("输入参数校验失败，请检查。<br>" + errors.join("<br>"));
            return;
        }

        console.log('Density Analysis (F4): 发送到后端的请求参数:', currentParams);

        try {
            mapVisualizer.clearAllOverlays();
            appState.densityResult = null; // 清空旧结果
            appState.currentTimeIndex = 0;
            updateTimeDisplay();

            const resultData = await fetchDensityData(currentParams); // resultData 是简化结构 {rows, cols, timeSlots, densityMap}
            console.log("Density Analysis (F4): 从后端接收到的简化数据:", resultData);

            if (!resultData || typeof resultData.densityMap !== 'object' || !Array.isArray(resultData.timeSlots) ||
                resultData.rows == null || resultData.cols == null) { // 检查 rows/cols 是否存在 (可以是0)
                throw new Error("返回的数据格式不正确或缺少必要字段 (rows, cols, densityMap, timeSlots)。");
            }
            appState.densityResult = resultData;

            // 可视化: drawGrid 需要后端返回的 rows/cols 和用户输入的边界(currentParams)
            mapVisualizer.drawGrid(resultData, currentParams);

            // fitMapToArea 使用用户输入的边界(currentParams)
            if (currentParams.minLongitude != null) {
                mapVisualizer.fitMapToArea(currentParams);
            }

            if (resultData.timeSlots.length === 0 || Object.keys(resultData.densityMap).length === 0) {
                displayResultsInfo("分析完成。在指定条件下未找到有效的密度数据。");
                // 即使没有密度数据，网格和区域也已绘制，地图也已调整
                return;
            }

            mapVisualizer.updateHeatmapForTimeSlot(0);
            mapVisualizer.addLegend();
            // fitMapToArea 已在 drawGrid 后调用

            displayResultsInfo(`密度分析成功，共 ${resultData.timeSlots.length} 个时间点。使用时间切换按钮查看。`);

        } catch (error) {
            console.error("Density Analysis (F4): 密度分析过程中发生错误:", error);
            displayError(error.message || "分析失败，请检查网络或联系管理员。");
        }
    }

    // --- 8. 事件监听器设置 ---
    function setupEventListeners() {
        if (!ui.analyzeBtn) {
            console.error("Density Analysis (F4): 分析按钮 'densityAnalysisBtn' 未在DOM中找到。");
            return;
        }
        ui.analyzeBtn.addEventListener("click", handleAnalyzeDensity);

        if (ui.prevTimeSlotBtn) {
            ui.prevTimeSlotBtn.addEventListener("click", () => {
                if (appState.densityResult && appState.densityResult.timeSlots.length > 0 && appState.currentTimeIndex > 0) {
                    mapVisualizer.updateHeatmapForTimeSlot(appState.currentTimeIndex - 1);
                }
            });
        } else {
            console.warn("Density Analysis (F4): 上一时间槽按钮 'prevTimeSlot' 未找到。");
        }

        if (ui.nextTimeSlotBtn) {
            ui.nextTimeSlotBtn.addEventListener("click", () => {
                if (appState.densityResult && appState.densityResult.timeSlots.length > 0 && appState.currentTimeIndex < appState.densityResult.timeSlots.length - 1) {
                    mapVisualizer.updateHeatmapForTimeSlot(appState.currentTimeIndex + 1);
                }
            });
        } else {
            console.warn("Density Analysis (F4): 下一时间槽按钮 'nextTimeSlot' 未找到。");
        }
        console.log("Density Analysis (F4): Event listeners set up.");
    }

    // --- 9. 初始化 ---
    document.addEventListener("DOMContentLoaded", () => {
        console.log("Density Analysis (F4): DOMContentLoaded 事件触发。");

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
        ui.commonResultDiv = document.getElementById("result");
        ui.currentTimeSlotDisplay = document.getElementById("currentTimeSlot");
        ui.prevTimeSlotBtn = document.getElementById("prevTimeSlot");
        ui.nextTimeSlotBtn = document.getElementById("nextTimeSlot");
        ui.mapContainerParent = document.getElementById("container");

        if (!ui.resultDiv) {
            const f4Container = document.getElementById("f4");
            if (f4Container) {
                const newResultDiv = document.createElement("div");
                newResultDiv.id = "f4_result";
                newResultDiv.className = "function-result";
                f4Container.appendChild(newResultDiv);
                ui.resultDiv = newResultDiv;
                console.log("Density Analysis (F4): #f4_result div 已动态创建。");
            } else {
                console.error("Density Analysis (F4): 无法找到父容器 #f4 来创建 #f4_result div。");
            }
        }

        if (!ui.analyzeBtn) {
            console.error("Density Analysis (F4): 分析按钮未找到，功能可能无法使用。");
            if(ui.resultDiv) displayError("页面初始化不完整（缺少分析按钮），功能可能无法使用。");
            return;
        }
        console.log("Density Analysis (F4): UI 元素引用已填充。开始轮询地图实例...");

        let mapCheckInterval;
        let mapCheckTimeout;
        const MAX_MAP_WAIT_TIME = 10000;
        const MAP_CHECK_INTERVAL_MS = 200;

        function initializeMapDependentComponents(mapInstance) {
            if (!mapVisualizer.initialize(mapInstance)) {
                displayError("地图可视化组件初始化失败。");
                return false;
            }
            setupEventListeners();
            displayResultsInfo("请设置参数并点击“分析区域车流密度”按钮开始。");
            console.log("Density Analysis (F4): 地图相关组件已成功初始化。");
            return true;
        }

        function tryInitializeMapLogic() {
            if (typeof window.map !== "undefined" &&
                window.map instanceof BMapGL.Map &&
                typeof window.map.getCenter === "function" &&
                window.map.getCenter()
            ) {
                console.log("Density Analysis (F4): 全局 'map' 实例找到并已准备就绪。");
                clearInterval(mapCheckInterval);
                clearTimeout(mapCheckTimeout);
                initializeMapDependentComponents(window.map);
            }
        }

        mapCheckInterval = setInterval(tryInitializeMapLogic, MAP_CHECK_INTERVAL_MS);

        mapCheckTimeout = setTimeout(() => {
            clearInterval(mapCheckInterval);
            if (typeof window.map !== "undefined" &&
                window.map instanceof BMapGL.Map &&
                typeof window.map.getCenter === "function" &&
                window.map.getCenter()
            ) {
                if (!appState.mapInstance) {
                    initializeMapDependentComponents(window.map);
                }
            } else {
                if (!appState.mapInstance) {
                    console.error(`Density Analysis (F4): 全局 'map' 实例在 ${MAX_MAP_WAIT_TIME / 1000} 秒后仍未可用。`);
                    displayError(`地图服务在 ${MAX_MAP_WAIT_TIME / 1000} 秒后仍不可用。请确保地图已正确加载或刷新页面。`);
                }
            }
        }, MAX_MAP_WAIT_TIME);
    });

})(BMapGL, window, document);