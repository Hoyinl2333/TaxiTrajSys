// --- F3模块初始化 ---
// 确保全局覆盖物管理器已初始化
if (typeof window.allFeatureOverlays === 'undefined') {
    window.allFeatureOverlays = {};
}
// 为 F3 功能模块在全局覆盖物管理器中创建或清空一个专属的数组条目
window.allFeatureOverlays["F3_区域矩形"] = [];
// console.log("F3-AREA-SEARCH: 已在 window.allFeatureOverlays 中初始化 'F3_区域矩形' 数组。"); // 精简日志

/**
 * F3: 区域范围查找
 */
function searchTaxisInArea() {
    // 获取表单输入值 (假设值总是存在且格式正确，由后端校验)
    var startTimeValue = document.getElementById("f3_startTime").value;
    var endTimeValue = document.getElementById("f3_endTime").value;
    var topLeftLng = Number.parseFloat(document.getElementById("f3_topLeftLng").value);
    var topLeftLat = Number.parseFloat(document.getElementById("f3_topLeftLat").value);
    var bottomRightLng = Number.parseFloat(document.getElementById("f3_bottomRightLng").value);
    var bottomRightLat = Number.parseFloat(document.getElementById("f3_bottomRightLat").value);

    var resultDiv = document.getElementById("f3_result");
    resultDiv.innerHTML = "<p>正在查询区域内的出租车数量...</p>";

    // --- 关键：在执行任何操作前，调用全局清除函数 ---
    clearOverlays();

    // 绘制查询区域矩形
    var minLng = Math.min(topLeftLng, bottomRightLng);
    var actualMinLat = Math.min(topLeftLat, bottomRightLat);
    var maxLng = Math.max(topLeftLng, bottomRightLng);
    var actualMaxLat = Math.max(topLeftLat, bottomRightLat);

    var polygonPoints = [
        new BMapGL.Point(minLng, actualMinLat),
        new BMapGL.Point(maxLng, actualMinLat),
        new BMapGL.Point(maxLng, actualMaxLat),
        new BMapGL.Point(minLng, actualMaxLat)
    ];

    var polygon = new BMapGL.Polygon(polygonPoints, {
        strokeColor: "blue",
        strokeWeight: 2,
        strokeOpacity: 0.8,
        fillColor: "blue",
        fillOpacity: 0.2
    });

    map.addOverlay(polygon);
    // --- 关键：将创建的覆盖物添加到 F3 在全局管理器中的专属数组 ---
    window.allFeatureOverlays["F3_区域矩形"].push(polygon);

    map.setViewport(polygonPoints);

    // 准备API请求参数
    const requestBody = {
        minLongitude: minLng,
        minLatitude: actualMinLat,
        maxLongitude: maxLng,
        maxLatitude: actualMaxLat,
        startTime: startTimeValue,
        endTime: endTimeValue
    };

    // console.log("F3 发送给后端的参数:", JSON.stringify(requestBody, null, 2)); // 精简日志

    const baseURL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
    var apiUrl = `${baseURL}/region/count`;
    const featureName = "F3区域范围查找";

    fetchApi(apiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
    })
        .then(data => {
            const f3ResultDiv = document.getElementById("f3_result");
            //  data 和 data.taxiCount 总是按预期结构返回
            f3ResultDiv.innerHTML = `<p>在指定时间段和区域内的出租车数量为: ${data.taxiCount} 辆</p>`;
        })
        .catch(error => {
            displayFetchError(error, "f3_result", featureName);
            // 出错时再次调用全局 clearOverlays 是一个合理的保险措施
            clearOverlays();
            // console.log("F3-AREA-SEARCH: 查询出错，再次调用全局 clearOverlays()。"); // 精简日志
        });
}

// --- DOMContentLoaded 事件监听器 ---
document.addEventListener("DOMContentLoaded", () => {
    const areaSearchBtn = document.getElementById("areaSearchBtn");
    areaSearchBtn.addEventListener("click", searchTaxisInArea); // 假设 areaSearchBtn 总是存在

    // F3 模块加载时，确保它在 window.allFeatureOverlays 中的条目存在
    if (typeof window.allFeatureOverlays !== 'object' || window.allFeatureOverlays === null) {
        window.allFeatureOverlays = {};
    }
    if (!window.allFeatureOverlays["F3_区域矩形"]) {
        window.allFeatureOverlays["F3_区域矩形"] = [];
    }
    // console.log("F3-AREA-SEARCH (DOMContentLoaded): 确保 window.allFeatureOverlays['F3_区域矩形'] 已初始化。"); // 精简日志

    // 为输入框设置默认值
    document.getElementById("f3_topLeftLng").value = "116.34";
    document.getElementById("f3_topLeftLat").value = "40.00";
    document.getElementById("f3_bottomRightLng").value = "116.48";
    document.getElementById("f3_bottomRightLat").value = "39.85";
    document.getElementById("f3_startTime").value = "2008-02-02T00:00";
    document.getElementById("f3_endTime").value = "2008-02-03T00:00";
});