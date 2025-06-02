// 文件: static/js/features/f6-RegionCorrelation2.js

// --- F6模块初始化：在全局覆盖物管理器中注册专属条目 ---
if (typeof window.allFeatureOverlays === 'undefined') {
    window.allFeatureOverlays = {};
}
// F6 绘制一个指定区域
window.allFeatureOverlays["F6_指定区域覆盖物"] = []; // 用于存储区域的矩形和可能的标签
// console.log("F6-RegionCorrelation2: 已在 window.allFeatureOverlays 中初始化 F6 的专属条目。");

document.addEventListener("DOMContentLoaded", () => {
    // window.f6Overlays = []; // 不再需要这个独立的全局数组

    let timeDataF6 = {}; // 使用F6后缀以避免与F5的timeData潜在冲突（如果它们作用域不隔离）
    let timePointsF6 = [];
    let currentTimeIndexF6 = 0;

    const prevTimeBtn = document.getElementById("f6_prevTime");
    const nextTimeBtn = document.getElementById("f6_nextTime");
    const currentTimeDisplay = document.getElementById("f6_currentTime");

    if (prevTimeBtn && nextTimeBtn) {
        prevTimeBtn.addEventListener("click", () => {
            if (timePointsF6.length > 0 && currentTimeIndexF6 > 0) {
                currentTimeIndexF6--;
                updateF6FlowDisplay();
            }
        });
        nextTimeBtn.addEventListener("click", () => {
            if (timePointsF6.length > 0 && currentTimeIndexF6 < timePointsF6.length - 1) {
                currentTimeIndexF6++;
                updateF6FlowDisplay();
            }
        });
    }

    function updateF6FlowDisplay() {
        if (timePointsF6.length === 0 || !timeDataF6[timePointsF6[currentTimeIndexF6]]) {
            if (currentTimeDisplay) currentTimeDisplay.value = "无数据";
            document.getElementById("flow_enter").textContent = "-";
            document.getElementById("flow_leave").textContent = "-";
            // document.getElementById("f6_flow_display").style.display = "none"; // 考虑UI行为
            return;
        }
        const currentTime = timePointsF6[currentTimeIndexF6];
        const flows = timeDataF6[currentTime];
        if (currentTimeDisplay) {
            const dateObj = new Date(currentTime);
            currentTimeDisplay.value = dateObj.toLocaleString("zh-CN", { /* 格式化选项 */ });
        }
        document.getElementById("flow_enter").textContent = flows[0];
        document.getElementById("flow_leave").textContent = flows[1];
        document.getElementById("f6_flow_display").style.display = "block";
    }

    const areaCorrelation2Btn = document.getElementById("areaCorrelation2Btn");
    if (areaCorrelation2Btn) {
        areaCorrelation2Btn.addEventListener("click", () => {
            const startTimeValue = document.getElementById("f6_startTime").value;
            const endTimeValue = document.getElementById("f6_endTime").value;
            const topLeftLngValue = document.getElementById("f6_topLeftLng").value;
            const topLeftLatValue = document.getElementById("f6_topLeftLat").value;
            const bottomRightLngValue = document.getElementById("f6_bottomRightLng").value;
            const bottomRightLatValue = document.getElementById("f6_bottomRightLat").value;
            const timeSlotMinutesValue = document.getElementById("f6_timeInterval").value;

            performAreaCorrelationAnalysis2(
                startTimeValue, endTimeValue,
                topLeftLngValue, topLeftLatValue,
                bottomRightLngValue, bottomRightLatValue,
                timeSlotMinutesValue
            );
        });
    }

    // window.clearF6Overlays 函数不再需要
    // window.clearF6Overlays = () => { ... }; // 删除此函数

    function performAreaCorrelationAnalysis2(
        startTime, endTime,
        valTopLeftLng, valTopLeftLat,
        valBottomRightLng, valBottomRightLat,
        valTimeSlotMinutes
    ) {
        const resultDiv = document.getElementById("f6_result");
        resultDiv.innerHTML = "<p>正在进行区域关联分析2...</p>";
        document.getElementById("f6_flow_display").style.display = "none";

        // --- 关键修改：在执行任何F6特定操作前，调用全局清除函数 ---
        if (typeof clearOverlays === "function") {
            // console.log("F6 (performAreaCorrelationAnalysis2): 调用全局 clearOverlays()。");
            clearOverlays();
        } else {
            console.warn("F6 (performAreaCorrelationAnalysis2): 全局 clearOverlays() 函数未定义!");
        }

        // 绘制指定区域 (覆盖物将添加到 window.allFeatureOverlays["F6_指定区域覆盖物"])
        drawF6AreaOnMap(valTopLeftLng, valTopLeftLat, valBottomRightLng, valBottomRightLat, "green", "指定区域");

        // （可选）调整地图视野
        try {
            if (map && map.setViewport) {
                map.setViewport([
                    new BMapGL.Point(valTopLeftLng, valBottomRightLat),
                    new BMapGL.Point(valBottomRightLng, valTopLeftLat)
                ], {margins:[30,30,30,30]});
            }
        } catch(e) { /* console.warn("F6: 设置视野时出错", e); */ }


        const params = {
            startTime: startTime,
            endTime: endTime,
            topLeftLongitude: Number.parseFloat(valTopLeftLng),
            topLeftLatitude: Number.parseFloat(valTopLeftLat),
            bottomRightLongitude: Number.parseFloat(valBottomRightLng),
            bottomRightLatitude: Number.parseFloat(valBottomRightLat),
            timeSlotMinutes: Number.parseInt(valTimeSlotMinutes),
        };
        // console.log("F6 发送给后端的参数:", JSON.stringify(params, null, 2));

        const baseURL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
        const apiUrl = `${baseURL}/correlation/trafficFlowChangeWithOtherRegions`;
        const featureName = "F6单区域关联分析";

        fetchApi(apiUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(params),
        }, featureName)
            .then(data => {
                const f6ResultDiv = document.getElementById("f6_result");
                const trafficData = data.trafficFlowChange || data; // 兼容后端可能直接返回map
                if (trafficData && Object.keys(trafficData).length > 0) {
                    timeDataF6 = trafficData;
                    timePointsF6 = Object.keys(trafficData).sort();
                    currentTimeIndexF6 = 0;
                    updateF6FlowDisplay();
                    f6ResultDiv.innerHTML = `<p>区域关联分析结果：已获取 ${timePointsF6.length} 个时间点的车流量数据。请使用时间选择器浏览。</p>`;
                } else {
                    f6ResultDiv.innerHTML = "<p>未获取到有效的关联分析结果，或结果为空。</p>";
                    document.getElementById("f6_flow_display").style.display = "none";
                    timeDataF6 = {}; timePointsF6 = [];
                    updateF6FlowDisplay();
                }
            })
            .catch(error => {
                displayFetchError(error, "f6_result", featureName);
                timeDataF6 = {}; timePointsF6 = [];
                updateF6FlowDisplay();
            });
    }

    // F6 模块内部的绘制函数
    function drawF6AreaOnMap(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat, color, labelText) {
        // 假设 map 和 BMapGL 总是可用的
        const tlPoint = new BMapGL.Point(Number.parseFloat(topLeftLng), Number.parseFloat(topLeftLat));
        const brPoint = new BMapGL.Point(Number.parseFloat(bottomRightLng), Number.parseFloat(bottomRightLat));

        const rectangle = new BMapGL.Polygon(
            [
                tlPoint, // 左上
                new BMapGL.Point(brPoint.lng, tlPoint.lat), // 右上
                brPoint, // 右下
                new BMapGL.Point(tlPoint.lng, brPoint.lat)  // 左下
            ],
            { strokeColor: color, strokeWeight: 2, strokeOpacity: 0.8, fillColor: color, fillOpacity: 0.2 }
        );
        map.addOverlay(rectangle);

        // （可选）为F6区域添加标签
        const label = new BMapGL.Label(labelText, {
            position: new BMapGL.Point(
                (Number.parseFloat(topLeftLng) + Number.parseFloat(bottomRightLng)) / 2,
                (Number.parseFloat(topLeftLat) + Number.parseFloat(bottomRightLat)) / 2
            ),
            offset: new BMapGL.Size(-25, -10) // 根据 "指定区域" 文本调整
        });
        label.setStyle({
            color: "white", backgroundColor: "rgba(0,128,0,0.8)", // Greenish
            border: "none", fontSize: "14px", padding: "5px 10px", borderRadius: "3px",
        });
        map.addOverlay(label);

        // --- 关键修改：将覆盖物添加到全局管理器中的F6专属数组 ---
        if (window.allFeatureOverlays && window.allFeatureOverlays["F6_指定区域覆盖物"]) {
            window.allFeatureOverlays["F6_指定区域覆盖物"].push(rectangle);
            window.allFeatureOverlays["F6_指定区域覆盖物"].push(label); // 如果添加了标签
        } else {
            console.error(`F6 (drawF6AreaOnMap): window.allFeatureOverlays["F6_指定区域覆盖物"] 未初始化！`);
        }
    }

    // 初始化F6在allFeatureOverlays中的条目 (DOMContentLoaded中再次确保)
    if (typeof window.allFeatureOverlays !== 'object' || window.allFeatureOverlays === null) {
        window.allFeatureOverlays = {};
    }
    if (!window.allFeatureOverlays["F6_指定区域覆盖物"]) {
        window.allFeatureOverlays["F6_指定区域覆盖物"] = [];
    }
    // console.log("F6-RegionCorrelation2 (DOMContentLoaded): 确保 F6 的覆盖物数组已初始化。");
});