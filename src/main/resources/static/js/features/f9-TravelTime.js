// --- F9模块初始化：在全局覆盖物管理器中注册专属条目 ---
if (typeof window.allFeatureOverlays === 'undefined') {
    window.allFeatureOverlays = {};
}
window.allFeatureOverlays["F9_区域A_覆盖物"] = [];
window.allFeatureOverlays["F9_区域B_覆盖物"] = [];
window.allFeatureOverlays["F9_路径覆盖物"] = [];
// console.log("F9-TravelTime: 已在 window.allFeatureOverlays 中初始化 F9 的专属条目。");


/**
 * F9: 绘制区域A和区域B的矩形及标签。
 */
function drawF9AreaRectanglesAndStore(
    areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat,
    areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat
) {
    // 辅助函数，用于创建单个区域的矩形和标签，并存入全局管理器
    const createRectAndLabelForF9 = (topLeftLngStr, topLeftLatStr, bottomRightLngStr, bottomRightLatStr, color, labelText, globalStorageKey) => {
        const topLeftLng = Number.parseFloat(topLeftLngStr);
        const topLeftLat = Number.parseFloat(topLeftLatStr);
        const bottomRightLng = Number.parseFloat(bottomRightLngStr);
        const bottomRightLat = Number.parseFloat(bottomRightLatStr);

        const tlPoint = new BMapGL.Point(topLeftLng, topLeftLat);
        const brPoint = new BMapGL.Point(bottomRightLng, bottomRightLat);

        const rectPoints = [
            tlPoint, new BMapGL.Point(brPoint.lng, tlPoint.lat),
            brPoint, new BMapGL.Point(tlPoint.lng, brPoint.lat)
        ];
        const polygon = new BMapGL.Polygon(rectPoints, {
            strokeColor: color, strokeWeight: 2, strokeOpacity: 1,
            fillColor: color, fillOpacity: 0.3,
        });
        map.addOverlay(polygon);
        window.allFeatureOverlays[globalStorageKey].push(polygon); // 添加到全局

        const label = new BMapGL.Label(labelText, {
            position: new BMapGL.Point((topLeftLng + bottomRightLng) / 2, (topLeftLat + bottomRightLat) / 2),
            offset: new BMapGL.Size(-20, -10)
        });
        label.setStyle({
            color: "#fff", backgroundColor: color === "#FF0000" ? "rgba(255,0,0,0.8)" : "rgba(0,0,255,0.8)",
            border: "none", fontSize: "14px", padding: "5px 10px", borderRadius: "3px",
        });
        map.addOverlay(label);
        window.allFeatureOverlays[globalStorageKey].push(label); // 添加到全局
        return rectPoints;
    };

    const pointsA = createRectAndLabelForF9(areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat, "#FF0000", "区域A", "F9_区域A_覆盖物");
    const pointsB = createRectAndLabelForF9(areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat, "#0000FF", "区域B", "F9_区域B_覆盖物");

    // 调整视野以包含两个区域
    if (map && map.setViewport && pointsA.length > 0 && pointsB.length > 0) {
        map.setViewport([...pointsA, ...pointsB]);
    } else if (map && map.setViewport && pointsA.length > 0) {
        map.setViewport(pointsA);
    } else if (map && map.setViewport && pointsB.length > 0) {
        map.setViewport(pointsB);
    }
}

/**
 * F9: 绘制最短路径及其起点终点标记。
 * 覆盖物会被添加到 window.allFeatureOverlays["F9_路径覆盖物"] 数组。
 */
function drawF9ShortestPathAndStore(pathPoints) {
    if (!pathPoints || pathPoints.length < 2 || !map) return;

    const bmapPoints = pathPoints.map(
        (p) => new BMapGL.Point(Number.parseFloat(p.longitude), Number.parseFloat(p.latitude))
    );

    const pathPolyline = new BMapGL.Polyline(bmapPoints, {
        strokeColor: "#00FF00", strokeWeight: 4, strokeOpacity: 0.8, // 绿色路径
    });
    map.addOverlay(pathPolyline);
    window.allFeatureOverlays["F9_路径覆盖物"].push(pathPolyline);

    const startMarker = new BMapGL.Marker(bmapPoints[0]);
    map.addOverlay(startMarker);
    window.allFeatureOverlays["F9_路径覆盖物"].push(startMarker);

    const endMarker = new BMapGL.Marker(bmapPoints[bmapPoints.length - 1]);
    map.addOverlay(endMarker);
    window.allFeatureOverlays["F9_路径覆盖物"].push(endMarker);

    // 绘制路径后，可以重新调整视野以确保路径可见
}

// 主要分析函数
function performCommunicationTimeAnalysis(
    startTime, endTime,
    valAreaATopLeftLng, valAreaATopLeftLat, valAreaABottomRightLng, valAreaABottomRightLat,
    valAreaBTopLeftLng, valAreaBTopLeftLat, valAreaBBottomRightLng, valAreaBBottomRightLat
) {
    const resultDivF9 = document.getElementById("f9_result");
    resultDivF9.innerHTML = "<p>正在进行通信时间分析...</p>";

    // --- 在执行任何F9特定操作前，调用全局清除函数 ---
    if (typeof clearOverlays === "function") {
        // console.log("F9 (performCommunicationTimeAnalysis): 调用全局 clearOverlays()。");
        clearOverlays();
    } else {
        console.warn("F9 (performCommunicationTimeAnalysis): 全局 clearOverlays() 函数未定义!");
    }

    // 绘制新的区域A和区域B (覆盖物将添加到 allFeatureOverlays)
    drawF9AreaRectanglesAndStore(
        valAreaATopLeftLng, valAreaATopLeftLat, valAreaABottomRightLng, valAreaABottomRightLat,
        valAreaBTopLeftLng, valAreaBTopLeftLat, valAreaBBottomRightLng, valAreaBBottomRightLat
    );

    const params = {
        startTime: startTime,
        endTime: endTime,
        regionA: {
            minLon: Number.parseFloat(valAreaATopLeftLng), maxLat: Number.parseFloat(valAreaATopLeftLat),
            maxLon: Number.parseFloat(valAreaABottomRightLng), minLat: Number.parseFloat(valAreaABottomRightLat)
        },
        regionB: {
            minLon: Number.parseFloat(valAreaBTopLeftLng), maxLat: Number.parseFloat(valAreaBTopLeftLat),
            maxLon: Number.parseFloat(valAreaBBottomRightLng), minLat: Number.parseFloat(valAreaBBottomRightLat)
        }
    };
    // console.log("F9 发送给后端的参数:", JSON.stringify(params, null, 2));

    const baseURL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
    const apiUrl = `${baseURL}/travelTime/analyze`;
    const featureName = "F9通行时间分析";

    fetchApi(apiUrl, { // 使用全局 fetchApi
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(params),
    }, featureName)
        .then(data => {
            const f9ResultDiv = document.getElementById("f9_result"); // 确保作用域
            if (data && data.found) {
                let resultHtml = `<p>通信时间分析结果：</p><ul>`;
                resultHtml += `<li>最短通行时间: ${escapeHtml(data.minTravelTimeFormatted || "N/A")}</li>`;
                if (data.shortestPath && data.shortestPath.length > 0) {
                    resultHtml += `<li>出租车ID: ${escapeHtml(data.shortestPath[0].taxiId)}</li>`;
                    // 绘制路径 (覆盖物将添加到 allFeatureOverlays)
                    drawF9ShortestPathAndStore(data.shortestPath);
                } else {
                    resultHtml += `<li>未找到具体路径点。</li>`;
                }
                resultHtml += "</ul>";
                f9ResultDiv.innerHTML = resultHtml;
            } else if (data && data.message) { // 后端可能返回未找到路径的消息
                f9ResultDiv.innerHTML = `<p>分析提示：${escapeHtml(data.message)}</p>`;
            } else {
                f9ResultDiv.innerHTML = "<p>未找到符合条件的通行路径，或未获取到有效分析结果。</p>";
            }
        })
        .catch(error => {
            displayFetchError(error, "f9_result", featureName); // 使用全局 displayFetchError
            // 全局 clearOverlays 已在函数开始时调用，错误时通常不需要再次调用，除非API调用成功但后续JS处理失败且已绘制部分新内容
        });
}

// --- DOMContentLoaded 事件监听器 ---
document.addEventListener("DOMContentLoaded", () => {
    const communicationTimeBtn = document.getElementById("communicationTimeBtn");
    if (communicationTimeBtn) {
        communicationTimeBtn.addEventListener("click", () => {
            const startTime = document.getElementById("f9_startTime").value;
            const endTime = document.getElementById("f9_endTime").value;
            const areaATopLeftLng = document.getElementById("f9_areaA_topLeftLng").value;
            const areaATopLeftLat = document.getElementById("f9_areaA_topLeftLat").value;
            const areaABottomRightLng = document.getElementById("f9_areaA_bottomRightLng").value;
            const areaABottomRightLat = document.getElementById("f9_areaA_bottomRightLat").value;
            const areaBTopLeftLng = document.getElementById("f9_areaB_topLeftLng").value;
            const areaBTopLeftLat = document.getElementById("f9_areaB_topLeftLat").value;
            const areaBBottomRightLng = document.getElementById("f9_areaB_bottomRightLng").value;
            const areaBBottomRightLat = document.getElementById("f9_areaB_bottomRightLat").value;

            // 假设输入总是有效的
            performCommunicationTimeAnalysis(
                startTime,endTime,
                areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat,
                areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat
            );
        });
    }

    // F9 模块加载时，确保它在 window.allFeatureOverlays 中的条目存在
    if (typeof window.allFeatureOverlays !== 'object' || window.allFeatureOverlays === null) {
        window.allFeatureOverlays = {};
    }
    if (!window.allFeatureOverlays["F9_区域A_覆盖物"]) { window.allFeatureOverlays["F9_区域A_覆盖物"] = []; }
    if (!window.allFeatureOverlays["F9_区域B_覆盖物"]) { window.allFeatureOverlays["F9_区域B_覆盖物"] = []; }
    if (!window.allFeatureOverlays["F9_路径覆盖物"]) { window.allFeatureOverlays["F9_路径覆盖物"] = []; }
    // console.log("F9-TravelTime (DOMContentLoaded): 确保 F9 的覆盖物数组已初始化。");
});