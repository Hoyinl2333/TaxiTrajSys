// 文件: static/js/features/f5-RegionCorrelation1.js

// --- F5模块初始化：在全局覆盖物管理器中注册专属条目 ---
if (typeof window.allFeatureOverlays === 'undefined') {
    window.allFeatureOverlays = {};
}
// 为 F5 定义清晰的覆盖物存储键
window.allFeatureOverlays["F5_区域A_覆盖物"] = []; // 用于存储区域A的矩形和标签
window.allFeatureOverlays["F5_区域B_覆盖物"] = []; // 用于存储区域B的矩形和标签
// console.log("F5-RegionCorrelation1: 已在 window.allFeatureOverlays 中初始化 F5 的专属条目。");


document.addEventListener("DOMContentLoaded", () => {
    // window.f5Overlays = [] // 不再需要这个独立的全局数组

    let timeData = {};
    let timePoints = [];
    let currentTimeIndex = 0;

    const prevTimeBtn = document.getElementById("f5_prevTime");
    const nextTimeBtn = document.getElementById("f5_nextTime");
    const currentTimeDisplay = document.getElementById("f5_currentTime");

    if (prevTimeBtn && nextTimeBtn) {
        prevTimeBtn.addEventListener("click", () => {
            if (timePoints.length > 0 && currentTimeIndex > 0) {
                currentTimeIndex--;
                updateFlowDisplay();
            }
        });
        nextTimeBtn.addEventListener("click", () => {
            if (timePoints.length > 0 && currentTimeIndex < timePoints.length - 1) {
                currentTimeIndex++;
                updateFlowDisplay();
            }
        });
    }

    function updateFlowDisplay() {
        if (timePoints.length === 0 || !timeData[timePoints[currentTimeIndex]]) {
            if (currentTimeDisplay) currentTimeDisplay.value = "无数据";
            document.getElementById("flow_1_to_2").textContent = "-";
            document.getElementById("flow_2_to_1").textContent = "-";
            return;
        }
        const currentTime = timePoints[currentTimeIndex];
        const flows = timeData[currentTime];
        if (currentTimeDisplay) {
            const dateObj = new Date(currentTime);
            currentTimeDisplay.value = dateObj.toLocaleString("zh-CN", { /* 格式化选项 */ });
        }
        document.getElementById("flow_1_to_2").textContent = flows[0];
        document.getElementById("flow_2_to_1").textContent = flows[1];
        document.getElementById("f5_flow_display").style.display = "block";
    }

    const areaCorrelation1Btn = document.getElementById("areaCorrelation1Btn");
    if (areaCorrelation1Btn) {
        areaCorrelation1Btn.addEventListener("click", () => {
            const startTime = document.getElementById("f5_startTime").value;
            const endTime = document.getElementById("f5_endTime").value;
            const area1TopLeftLng = document.getElementById("f5_area1_topLeftLng").value;
            const area1TopLeftLat = document.getElementById("f5_area1_topLeftLat").value;
            const area1BottomRightLng = document.getElementById("f5_area1_bottomRightLng").value;
            const area1BottomRightLat = document.getElementById("f5_area1_bottomRightLat").value;
            const area2TopLeftLng = document.getElementById("f5_area2_topLeftLng").value;
            const area2TopLeftLat = document.getElementById("f5_area2_topLeftLat").value;
            const area2BottomRightLng = document.getElementById("f5_area2_bottomRightLng").value;
            const area2BottomRightLat = document.getElementById("f5_area2_bottomRightLat").value;

            // 假设输入总是有效的，由后端校验
            performAreaCorrelationAnalysis1(
                startTime, endTime,
                area1TopLeftLng, area1TopLeftLat, area1BottomRightLng, area1BottomRightLat,
                area2TopLeftLng, area2TopLeftLat, area2BottomRightLng, area2BottomRightLat
            );
        });
    }

    function performAreaCorrelationAnalysis1(
        startTime, endTime,
        a1TLLng, a1TLLat, a1BRLng, a1BRLat,
        a2TLLng, a2TLLat, a2BRLng, a2BRLat
    ) {
        const resultDiv = document.getElementById("f5_result");
        resultDiv.innerHTML = "<p>正在进行区域关联分析1...</p>";
        document.getElementById("f5_flow_display").style.display = "none"; // 初始隐藏车流显示

        // --- 关键修改：在执行任何F5特定操作前，调用全局清除函数 ---
        if (typeof clearOverlays === "function") {
            // console.log("F5 (performAreaCorrelationAnalysis1): 调用全局 clearOverlays()。");
            clearOverlays();
        } else {
            console.warn("F5 (performAreaCorrelationAnalysis1): 全局 clearOverlays() 函数未定义!");
        }

        // 绘制区域 (覆盖物将添加到 window.allFeatureOverlays)
        drawF5AreaOnMap(a1TLLng, a1TLLat, a1BRLng, a1BRLat, "blue", "区域1", "F5_区域A_覆盖物");
        drawF5AreaOnMap(a2TLLng, a2TLLat, a2BRLng, a2BRLat, "red",  "区域2", "F5_区域B_覆盖物");

        // （可选）调整地图视野
        try {
            const pointsToView = [
                new BMapGL.Point(a1TLLng, a1BRLat), new BMapGL.Point(a1BRLng, a1TLLat),
                new BMapGL.Point(a2TLLng, a2BRLat), new BMapGL.Point(a2BRLng, a2TLLat),
            ];
            if (map && map.setViewport) map.setViewport(pointsToView, {margins:[30,30,30,30]});
        } catch(e) { /* console.warn("F5: 设置视野时出错", e); */ }


        const params = {
            startTime: startTime,
            endTime: endTime,
            timeSlotMinutes: Number.parseInt(document.getElementById("f5_timeInterval").value), // 从DOM获取
            region1: {
                minLon: Number.parseFloat(a1TLLng), maxLat: Number.parseFloat(a1TLLat),
                maxLon: Number.parseFloat(a1BRLng), minLat: Number.parseFloat(a1BRLat)
            },
            region2: {
                minLon: Number.parseFloat(a2TLLng), maxLat: Number.parseFloat(a2TLLat),
                maxLon: Number.parseFloat(a2BRLng), minLat: Number.parseFloat(a2BRLat)
            }
        };
        // console.log("F5 发送给后端的参数:", JSON.stringify(params, null, 2));

        const baseURL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : '';
        const apiUrl = `${baseURL}/correlation/trafficFlowChangeBetweenRegions`;
        const featureName = "F5区域间关联分析";

        fetchApi(apiUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(params),
        }, featureName)
            .then(data => {
                const f5ResultDiv = document.getElementById("f5_result"); // 重新获取，确保作用域
                if (data && data.trafficFlowChange && Object.keys(data.trafficFlowChange).length > 0) {
                    timeData = data.trafficFlowChange;
                    timePoints = Object.keys(data.trafficFlowChange).sort(); // 按时间排序
                    currentTimeIndex = 0;
                    updateFlowDisplay();
                    f5ResultDiv.innerHTML = `<p>区域关联分析结果：已获取 ${timePoints.length} 个时间点的车流量数据。请使用时间选择器浏览。</p>`;
                } else {
                    f5ResultDiv.innerHTML = "<p>未获取到有效的关联分析结果，或结果为空。</p>";
                    document.getElementById("f5_flow_display").style.display = "none";
                    timeData = {}; timePoints = []; // 清空旧数据
                    updateFlowDisplay(); // 更新显示为无数据
                }
            })
            .catch(error => {
                displayFetchError(error, "f5_result", featureName);
                timeData = {}; timePoints = []; // 清空旧数据
                updateFlowDisplay(); // 更新显示为无数据
                // 全局 clearOverlays 已在函数开始时调用，错误时通常不需要再次调用，
                // 除非API调用成功但后续处理失败且已绘制了部分新内容（不太可能）。
            });
    }

    // 修改后的绘制区域函数，将覆盖物添加到指定的全局条目中
    function drawF5AreaOnMap(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat, color, labelText, globalOverlayKey) {
        // 假设 map 和 BMapGL 总是可用的
        const topLeftPoint = new BMapGL.Point(Number.parseFloat(topLeftLng), Number.parseFloat(topLeftLat));
        const bottomRightPoint = new BMapGL.Point(Number.parseFloat(bottomRightLng), Number.parseFloat(bottomRightLat));

        const rectangle = new BMapGL.Polygon(
            [
                topLeftPoint,
                new BMapGL.Point(bottomRightPoint.lng, topLeftPoint.lat), // 右上角
                bottomRightPoint,
                new BMapGL.Point(topLeftPoint.lng, bottomRightPoint.lat), // 左下角
            ],
            { strokeColor: color, strokeWeight: 2, strokeOpacity: 0.8, fillColor: color, fillOpacity: 0.2 }
        );
        map.addOverlay(rectangle);

        const label = new BMapGL.Label(labelText, {
            position: new BMapGL.Point(
                (Number.parseFloat(topLeftLng) + Number.parseFloat(bottomRightLng)) / 2,
                (Number.parseFloat(topLeftLat) + Number.parseFloat(bottomRightLat)) / 2
            ),
            offset: new BMapGL.Size(-20, -10) // 根据标签文本长度调整偏移
        });
        label.setStyle({
            color: "#fff",
            backgroundColor: `rgba(${color === "blue" ? "0,0,255" : "255,0,0"}, 0.8)`, // 示例颜色处理
            border: "none", fontSize: "14px", padding: "5px 10px", borderRadius: "3px",
        });
        map.addOverlay(label);

        // --- 关键修改：将覆盖物添加到全局管理器中的F5专属数组 ---
        if (window.allFeatureOverlays && window.allFeatureOverlays[globalOverlayKey]) {
            window.allFeatureOverlays[globalOverlayKey].push(rectangle);
            window.allFeatureOverlays[globalOverlayKey].push(label);
        } else {
            console.error(`F5 (drawF5AreaOnMap): window.allFeatureOverlays["${globalOverlayKey}"] 未初始化！`);
        }
    }

    // 初始化F5在allFeatureOverlays中的条目 (DOMContentLoaded中再次确保)
    if (typeof window.allFeatureOverlays !== 'object' || window.allFeatureOverlays === null) {
        window.allFeatureOverlays = {};
    }
    if (!window.allFeatureOverlays["F5_区域A_覆盖物"]) {
        window.allFeatureOverlays["F5_区域A_覆盖物"] = [];
    }
    if (!window.allFeatureOverlays["F5_区域B_覆盖物"]) {
        window.allFeatureOverlays["F5_区域B_覆盖物"] = [];
    }
    // console.log("F5-RegionCorrelation1 (DOMContentLoaded): 确保 F5 的覆盖物数组已初始化。");
});