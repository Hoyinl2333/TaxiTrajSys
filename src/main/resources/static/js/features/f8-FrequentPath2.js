/**
 * F8: 频繁路径分析2
 * 分析两个矩形区域间的频繁路径，并在地图上显示
 */

// 全局变量存储路径数据
let f8PathFrequenciesData = [];
let f8CurrentPathIndex = 0;
window.f8PathPolylines = []; // 存储路径折线对象，暴露到全局作用域
window.f8AreaRectangles = []; // 存储区域矩形覆盖物，暴露到全局作用域

document.addEventListener("DOMContentLoaded", () => {
    const frequentPath2Btn = document.getElementById("frequentPath2Btn");
    const pathSelector = document.getElementById("f8_path_selector");
    const pathSelectorContainer = document.getElementById("f8PathSelectorContainer");
    const pathDetails = document.getElementById("f8_path_details");

    // 初始隐藏路径选择器和详情
    if (pathSelectorContainer) pathSelectorContainer.style.display = "none";
    if (pathDetails) pathDetails.style.display = "none";

    if (frequentPath2Btn) {
        frequentPath2Btn.addEventListener("click", () => {
            const k = document.getElementById("f8_k").value;
            const areaATopLeftLng = document.getElementById("f8_areaA_topLeftLng").value;
            const areaATopLeftLat = document.getElementById("f8_areaA_topLeftLat").value;
            const areaABottomRightLng = document.getElementById("f8_areaA_bottomRightLng").value;
            const areaABottomRightLat = document.getElementById("f8_areaA_bottomRightLat").value;
            const areaBTopLeftLng = document.getElementById("f8_areaB_topLeftLng").value;
            const areaBTopLeftLat = document.getElementById("f8_areaB_topLeftLat").value;
            const areaBBottomRightLng = document.getElementById("f8_areaB_bottomRightLng").value;
            const areaBBottomRightLat = document.getElementById("f8_areaB_bottomRightLat").value;

            // 验证输入
            if (
                !k ||
                !areaATopLeftLng ||
                !areaATopLeftLat ||
                !areaABottomRightLng ||
                !areaABottomRightLat ||
                !areaBTopLeftLng ||
                !areaBTopLeftLat ||
                !areaBBottomRightLng ||
                !areaBBottomRightLat
            ) {
                alert("请填写完整的分析条件");
                return;
            }

            // 清除之前的覆盖物
            clearF8Overlays();

            // 绘制区域A和区域B的矩形
            drawF8AreaRectangles(
                areaATopLeftLng,
                areaATopLeftLat,
                areaABottomRightLng,
                areaABottomRightLat,
                areaBTopLeftLng,
                areaBTopLeftLat,
                areaBBottomRightLng,
                areaBBottomRightLat
            );

            // 调用后端接口
            performFrequentPathAnalysis2(
                k,
                areaATopLeftLng,
                areaATopLeftLat,
                areaABottomRightLng,
                areaABottomRightLat,
                areaBTopLeftLng,
                areaBTopLeftLat,
                areaBBottomRightLng,
                areaBBottomRightLat
            );
        });
    }

    // 添加路径选择器的变化事件
    if (pathSelector) {
        pathSelector.addEventListener("change", function () {
            const selectedIndex = Number.parseInt(this.value);
            f8CurrentPathIndex = selectedIndex;
            displayF8PathOnMap(selectedIndex);
            updateF8PathDetails(selectedIndex);
        });
    }

    // 添加上一条/下一条路径按钮事件
    const prevPathBtn = document.getElementById("f8PrevPathBtn");
    const nextPathBtn = document.getElementById("f8NextPathBtn");

    if (prevPathBtn) {
        prevPathBtn.addEventListener("click", () => {
            if (f8PathFrequenciesData.length === 0) return;

            f8CurrentPathIndex = (f8CurrentPathIndex - 1 + f8PathFrequenciesData.length) % f8PathFrequenciesData.length;
            pathSelector.value = f8CurrentPathIndex;
            displayF8PathOnMap(f8CurrentPathIndex);
            updateF8PathDetails(f8CurrentPathIndex);
        });
    }

    if (nextPathBtn) {
        nextPathBtn.addEventListener("click", () => {
            if (f8PathFrequenciesData.length === 0) return;

            f8CurrentPathIndex = (f8CurrentPathIndex + 1) % f8PathFrequenciesData.length;
            pathSelector.value = f8CurrentPathIndex;
            displayF8PathOnMap(f8CurrentPathIndex);
            updateF8PathDetails(f8CurrentPathIndex);
        });
    }
});

// 绘制区域A和区域B的矩形
function drawF8AreaRectangles(
    areaATopLeftLng,
    areaATopLeftLat,
    areaABottomRightLng,
    areaABottomRightLat,
    areaBTopLeftLng,
    areaBTopLeftLat,
    areaBBottomRightLng,
    areaBBottomRightLat
) {
    // 清除之前的区域矩形
    if (window.f8AreaRectangles && window.f8AreaRectangles.length > 0) {
        window.f8AreaRectangles.forEach(overlay => {
            map.removeOverlay(overlay);
        });
        window.f8AreaRectangles = [];
    }

    // 创建区域A的矩形
    const areaAPoints = [
        new BMapGL.Point(Number.parseFloat(areaATopLeftLng), Number.parseFloat(areaATopLeftLat)),
        new BMapGL.Point(Number.parseFloat(areaABottomRightLng), Number.parseFloat(areaATopLeftLat)),
        new BMapGL.Point(Number.parseFloat(areaABottomRightLng), Number.parseFloat(areaABottomRightLat)),
        new BMapGL.Point(Number.parseFloat(areaATopLeftLng), Number.parseFloat(areaABottomRightLat))
    ];

    const areaAPolygon = new BMapGL.Polygon(areaAPoints, {
        strokeColor: "#FF0000",
        strokeWeight: 2,
        strokeOpacity: 1,
        fillColor: "#FF0000",
        fillOpacity: 0.3
    });
    map.addOverlay(areaAPolygon);
    window.f8AreaRectangles.push(areaAPolygon);

    // 添加区域A标签
    const areaALabel = new BMapGL.Label("区域A", {
        position: new BMapGL.Point(
            (Number.parseFloat(areaATopLeftLng) + Number.parseFloat(areaABottomRightLng)) / 2,
            (Number.parseFloat(areaATopLeftLat) + Number.parseFloat(areaABottomRightLat)) / 2
        ),
        offset: new BMapGL.Size(0, 0)
    });
    areaALabel.setStyle({
        color: "#fff",
        backgroundColor: "rgba(255, 0, 0, 0.8)",
        border: "none",
        fontSize: "14px",
        padding: "5px 10px",
        borderRadius: "3px"
    });
    map.addOverlay(areaALabel);
    window.f8AreaRectangles.push(areaALabel);

    // 创建区域B的矩形
    const areaBPoints = [
        new BMapGL.Point(Number.parseFloat(areaBTopLeftLng), Number.parseFloat(areaBTopLeftLat)),
        new BMapGL.Point(Number.parseFloat(areaBBottomRightLng), Number.parseFloat(areaBTopLeftLat)),
        new BMapGL.Point(Number.parseFloat(areaBBottomRightLng), Number.parseFloat(areaBBottomRightLat)),
        new BMapGL.Point(Number.parseFloat(areaBTopLeftLng), Number.parseFloat(areaBBottomRightLat))
    ];

    const areaBPolygon = new BMapGL.Polygon(areaBPoints, {
        strokeColor: "#0000FF",
        strokeWeight: 2,
        strokeOpacity: 1,
        fillColor: "#0000FF",
        fillOpacity: 0.3
    });
    map.addOverlay(areaBPolygon);
    window.f8AreaRectangles.push(areaBPolygon);

    // 添加区域B标签
    const areaBLabel = new BMapGL.Label("区域B", {
        position: new BMapGL.Point(
            (Number.parseFloat(areaBTopLeftLng) + Number.parseFloat(areaBBottomRightLng)) / 2,
            (Number.parseFloat(areaBTopLeftLat) + Number.parseFloat(areaBBottomRightLat)) / 2
        ),
        offset: new BMapGL.Size(0, 0)
    });
    areaBLabel.setStyle({
        color: "#fff",
        backgroundColor: "rgba(0, 0, 255, 0.8)",
        border: "none",
        fontSize: "14px",
        padding: "5px 10px",
        borderRadius: "3px"
    });
    map.addOverlay(areaBLabel);
    window.f8AreaRectangles.push(areaBLabel);

    // 调整地图视野以包含两个区域
    const allPoints = [...areaAPoints, ...areaBPoints];
    map.setViewport(allPoints);

    console.log("已绘制区域矩形，共", window.f8AreaRectangles.length, "个覆盖物");
}

function performFrequentPathAnalysis2(
    k,
    areaATopLeftLng,
    areaATopLeftLat,
    areaABottomRightLng,
    areaABottomRightLat,
    areaBTopLeftLng,
    areaBTopLeftLat,
    areaBBottomRightLng,
    areaBBottomRightLat
) {
    const resultDiv = document.getElementById("f8_result");
    const pathSelectorContainer = document.getElementById("f8PathSelectorContainer");
    const pathDetails = document.getElementById("f8_path_details");

    if (!resultDiv) {
        console.error("未找到 f8_result 元素");
        return;
    }

    resultDiv.innerHTML = "<p>正在进行区域间频繁路径分析...</p>";

    // 隐藏路径选择器和详情
    if (pathSelectorContainer) pathSelectorContainer.style.display = "none";
    if (pathDetails) pathDetails.style.display = "none";

    // 清除之前的路径显示
    clearF8PathPolylines();

    // 构建请求参数
    const params = {
        k: Number.parseInt(k, 10),
        regionA: {
            minLat: Number.parseFloat(areaABottomRightLat),
            maxLat: Number.parseFloat(areaATopLeftLat),
            minLon: Number.parseFloat(areaATopLeftLng),
            maxLon: Number.parseFloat(areaABottomRightLng)
        },
        regionB: {
            minLat: Number.parseFloat(areaBBottomRightLat),
            maxLat: Number.parseFloat(areaBTopLeftLat),
            minLon: Number.parseFloat(areaBTopLeftLng),
            maxLon: Number.parseFloat(areaBBottomRightLng)
        }
    };

    console.log("发送的请求体:", params); // 调试信息，查看发送的请求体

    // 构建请求URL
    const baseURL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : '';
    const apiUrl = `${baseURL}/paths/frequent/regional`;

    // 发起POST请求
    fetch(apiUrl, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(params)
    })
        .then((response) => {
            if (!response.ok) {
                throw new Error(`网络响应异常，状态码: ${response.status}`);
            }
            return response.json();
        })
        .then((data) => {
            console.log("后端返回的数据:", data); // 调试信息，查看返回的数据
            processF8FrequentPathData(data);
        })
        .catch((error) => {
            resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`;
            console.error("Error:", error);
        });
}

function processF8FrequentPathData(data) {
    const resultDiv = document.getElementById("f8_result");
    const pathSelector = document.getElementById("f8_path_selector");
    const pathSelectorContainer = document.getElementById("f8PathSelectorContainer");
    const pathDetails = document.getElementById("f8_path_details");

    if (data && data.pathFrequencies && data.pathFrequencies.length > 0) {
        // 保存数据到全局变量
        f8PathFrequenciesData = data.pathFrequencies;
        f8CurrentPathIndex = 0;

        // 更新结果显示
        resultDiv.innerHTML = `<p>区域间频繁路径分析结果：找到 ${data.pathFrequencies.length} 条频繁路径</p>`;

        // 清空并填充路径选择器
        pathSelector.innerHTML = "";
        data.pathFrequencies.forEach((pathFrequency, index) => {
            const option = document.createElement("option");
            option.value = index;
            option.textContent = `路径 ${index + 1} (频率: ${pathFrequency.frequency})`;
            pathSelector.appendChild(option);
        });

        // 显示路径选择器和详情
        if (pathSelectorContainer) pathSelectorContainer.style.display = "flex";
        if (pathDetails) pathDetails.style.display = "block";

        // 显示第一条路径
        pathSelector.value = 0;
        displayF8PathOnMap(0);
        updateF8PathDetails(0);
    } else {
        resultDiv.innerHTML = "<p>未获取到有效的分析结果。</p>";

        // 隐藏路径选择器和详情
        if (pathSelectorContainer) pathSelectorContainer.style.display = "none";
        if (pathDetails) pathDetails.style.display = "none";
    }
}

function displayF8PathOnMap(pathIndex) {
    if (!f8PathFrequenciesData || pathIndex >= f8PathFrequenciesData.length) {
        console.error("无效的路径索引或数据");
        return;
    }

    // 清除之前的路径显示（但保留区域矩形）
    clearF8PathPolylines();

    // 获取选中的路径数据
    const pathData = f8PathFrequenciesData[pathIndex];
    const coordinates = pathData.pathCoordinates;

    // 转换坐标
    const points = coordinates.map((coord) => new BMapGL.Point(coord.longitude, coord.latitude));

    // 使用坐标转换函数
    convertCoordinates(points, (data) => {
        if (data.status === 0) {
            // 创建折线
            const polyline = new BMapGL.Polyline(data.points, {
                strokeColor: getF8PathColor(pathIndex),
                strokeWeight: 5,
                strokeOpacity: 0.8
            });

            // 添加到地图
            map.addOverlay(polyline);
            window.f8PathPolylines.push(polyline);

            // 添加起点和终点标记
            if (data.points.length > 0) {
                // 添加起点 - 绿色
                const startDot = addDotToMap(data.points[0], "green");
                window.f8PathPolylines.push(startDot);

                // 添加终点 - 红色
                const endDot = addDotToMap(data.points[data.points.length - 1], "red");
                window.f8PathPolylines.push(endDot);

                // 添加路径点 - 蓝色
                for (let i = 1; i < data.points.length - 1; i++) {
                    const dot = addDotToMap(data.points[i], "blue");
                    window.f8PathPolylines.push(dot);
                }
            }
        }
    });
}

function updateF8PathDetails(pathIndex) {
    const pathDetails = document.getElementById("f8_path_details");
    if (!pathDetails || !f8PathFrequenciesData || pathIndex >= f8PathFrequenciesData.length) return;

    const pathData = f8PathFrequenciesData[pathIndex];

    // 显示路径详情
    /*let detailsHtml = `<h4>路径 ${pathIndex + 1} 详情 (频率: ${pathData.frequency})</h4>`;
    detailsHtml += '<div class="path-details">';

    pathData.pathCoordinates.forEach((coord, idx) => {
        detailsHtml += `
            <div class="path-detail-item">
                <span class="path-detail-label">点 ${idx + 1}:</span>
                <span>经度: ${coord.longitude.toFixed(6)}, 纬度: ${coord.latitude.toFixed(6)}</span>
            </div>
        `;
    });

    detailsHtml += "</div>";
    pathDetails.innerHTML = detailsHtml;*/
    // 显示路径详情

}

// 清除F8功能的所有覆盖物
window.clearF8Overlays = function() {
    console.log("清除F8所有覆盖物");

    // 清除区域矩形和标记
    if (window.f8AreaRectangles && Array.isArray(window.f8AreaRectangles)) {
        window.f8AreaRectangles.forEach((overlay) => {
            if (map && map.removeOverlay) {
                map.removeOverlay(overlay);
            }
        });
        window.f8AreaRectangles = [];
    }

    // 清除路径折线
    clearF8PathPolylines();
};

// 清除路径，保留区域矩形
function clearF8PathPolylines() {
    // 清除之前的路径折线
    if (window.f8PathPolylines && Array.isArray(window.f8PathPolylines)) {
        window.f8PathPolylines.forEach((overlay) => {
            if (map && map.removeOverlay) {
                map.removeOverlay(overlay);
            }
        });
        window.f8PathPolylines = [];

        // 清除添加的点
          for (let i = 0; i < overlays.length; i++) {
            if (map && map.removeOverlay && overlays[i]) {
              map.removeOverlay(overlays[i])
            }
          }
          overlays = []
    }
}

function getF8PathColor(index) {
    // 为不同的路径提供不同的颜色
    const colors = [
        "#FF5252", // 红色
        "#4CAF50", // 绿色
        "#2196F3", // 蓝色
        "#FFC107", // 黄色
        "#9C27B0", // 紫色
        "#00BCD4", // 青色
        "#FF9800", // 橙色
        "#795548", // 棕色
        "#607D8B", // 蓝灰色
        "#E91E63", // 粉色
    ];

    return colors[index % colors.length];
}

// 修改map-utils.js中的clearOverlays函数，确保它能清除f8的覆盖物
// 这个函数会在原始的clearOverlays函数中被调用
if (typeof window.originalClearOverlays !== 'function') {
    // 保存原始的clearOverlays函数
    window.originalClearOverlays = window.clearOverlays;

    // 重写clearOverlays函数
    window.clearOverlays = function() {
        // 调用原始的clearOverlays函数
        if (typeof window.originalClearOverlays === 'function') {
            window.originalClearOverlays();
        }

        // 确保清除f8的覆盖物
        if (typeof window.clearF8Overlays === 'function') {
            window.clearF8Overlays();
        }
    };
}