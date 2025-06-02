// --- F1模块初始化 ---
// 确保全局覆盖物管理器已初始化 (在map-utils.js中完成)

if (typeof window.allFeatureOverlays === 'undefined') {
    window.allFeatureOverlays = {};
}
// 为 F1 功能模块在全局覆盖物管理器中创建或清空一个专属的数组条目
// 使用一个描述性的键名，例如 "F1_轨迹点"
window.allFeatureOverlays["F1_轨迹点"] = [];
console.log("F1-TRAJECTORY: 已在 window.allFeatureOverlays 中初始化 'F1_轨迹点' 数组。");



function fetchTaxiTrajectory() {
    var taxiId = document.getElementById("taxiId").value;
    var resultDiv = document.getElementById("f1_result");

    resultDiv.innerHTML = "<p>正在加载数据...</p>";

    // --- 在执行任何操作前，调用全局清除函数 ---
    // 这将清除所有模块（包括F1自身上一次执行）的覆盖物
    if (typeof clearOverlays === "function") {
        clearOverlays();
    } else {
        console.warn("F1-TRAJECTORY: 全局 clearOverlays() 函数未定义！");
    }

    const baseURL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
    var apiUrl = `${baseURL}/taxi/${taxiId}`;
    const featureName = "F1出租车轨迹查询";

    fetchApi(apiUrl, { method: 'GET' }, featureName)
        .then((data) => {
            const f1ResultDiv = document.getElementById("f1_result"); // 使用局部变量，避免与外部的 resultDiv 混淆
            if (f1ResultDiv) {
                f1ResultDiv.innerHTML = `<p>成功获取出租车 ${taxiId} 的轨迹数据，共 ${data.length} 个点</p>`;
            }

            if (!data || data.length === 0) {
                // console.log("F1-TRAJECTORY: 未查询到轨迹数据或数据为空。"); // 无需显示在结果区，控制台日志即可
                return; // 如果没有数据点，则不进行后续操作
            }

            var points = [];
            data.forEach((record) => {
                // 确保经纬度存在且是数字
                if (typeof record.longitude === 'number' && typeof record.latitude === 'number') {
                    points.push(new BMapGL.Point(record.longitude, record.latitude));
                } else {
                    console.warn(`F1-TRAJECTORY: 记录 ${record.taxiId} 的经纬度无效或缺失，已跳过该点。`, record);
                }
            });

            if (points.length === 0) {
                // console.log("F1-TRAJECTORY: 转换后没有有效的轨迹点。");
                if (f1ResultDiv) f1ResultDiv.innerHTML += "<p>但未找到有效的轨迹点进行显示。</p>";
                return;
            }


            if (points.length > 0) {
                points.forEach((point) => {
                    // 直接绘图（不再转换坐标）
                    var dotOverlay = addDotToMap(point); // 默认颜色是 red
                    if (dotOverlay) {
                        if (window.allFeatureOverlays && window.allFeatureOverlays["F1_轨迹点"]) {
                            window.allFeatureOverlays["F1_轨迹点"].push(dotOverlay);
                        } else {
                            console.error("F1-TRAJECTORY: window.allFeatureOverlays['F1_轨迹点'] 未初始化！无法存储覆盖物。");
                        }
                    }
                });

                // 调整地图视野
                if (map) {
                    if (points.length > 1) {
                        map.setViewport(points);
                    } else if (points.length === 1) {
                        map.setCenter(points[0]);
                        map.setZoom(15);
                    }
                }
            }
        })
        .catch((error) => {
            // displayFetchError 会处理错误信息的显示
            displayFetchError(error, "f1_result", featureName);
            if (typeof clearOverlays === "function") {
                console.log("F1-TRAJECTORY: 查询出错，再次调用全局 clearOverlays() 以确保地图清洁。");
                clearOverlays();
            }
        });
}

// --- DOMContentLoaded 事件监听器 ---
document.addEventListener("DOMContentLoaded", () => {
    const queryBtn = document.getElementById("queryTrajectoryBtn");
    if (queryBtn) {
        queryBtn.addEventListener("click", fetchTaxiTrajectory);
    }

    // F1 模块加载时，确保它在 window.allFeatureOverlays 中的条目存在
    if (typeof window.allFeatureOverlays !== 'object' || window.allFeatureOverlays === null) {
        window.allFeatureOverlays = {}; // 以防万一 map-utils.js 还未加载或初始化
    }
    if (!window.allFeatureOverlays["F1_轨迹点"]) { // 避免重复创建
        window.allFeatureOverlays["F1_轨迹点"] = [];
        console.log("F1-TRAJECTORY (DOMContentLoaded): 确保 window.allFeatureOverlays['F1_轨迹点'] 已初始化。");
    }
});