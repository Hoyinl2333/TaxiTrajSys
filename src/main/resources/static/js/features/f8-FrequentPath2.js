// --- F8模块初始化：在全局覆盖物管理器中注册专属条目 ---
if (typeof window.allFeatureOverlays === 'undefined') {
    window.allFeatureOverlays = {};
}
window.allFeatureOverlays["F8_路径覆盖物"] = [];    // 存储路径折线和起终点标记
window.allFeatureOverlays["F8_区域A_覆盖物"] = [];  // 存储区域A的矩形和标签
window.allFeatureOverlays["F8_区域B_覆盖物"] = [];  // 存储区域B的矩形和标签
// console.log("F8-FrequentPath2: 已在 window.allFeatureOverlays 中初始化 F8 的专属条目。");


// 全局变量 (可以考虑作用域隔离)
let f8PathFrequenciesData = []; // F8的路径数据
let f8CurrentPathIndex = 0;    // F8当前路径索引

document.addEventListener("DOMContentLoaded", () => {
    const frequentPath2Btn = document.getElementById("frequentPath2Btn");
    const pathSelectorF8 = document.getElementById("f8_path_selector");
    const pathSelectorContainerF8 = document.getElementById("f8PathSelectorContainer");

    if (pathSelectorContainerF8) pathSelectorContainerF8.style.display = "none";

    if (frequentPath2Btn) {
        frequentPath2Btn.addEventListener("click", () => {
            const kValue = document.getElementById("f8_k").value;
            const areaATopLeftLngVal = document.getElementById("f8_areaA_topLeftLng").value;
            const areaATopLeftLatVal = document.getElementById("f8_areaA_topLeftLat").value;
            const areaABottomRightLngVal = document.getElementById("f8_areaA_bottomRightLng").value;
            const areaABottomRightLatVal = document.getElementById("f8_areaA_bottomRightLat").value;
            const areaBTopLeftLngVal = document.getElementById("f8_areaB_topLeftLng").value;
            const areaBTopLeftLatVal = document.getElementById("f8_areaB_topLeftLat").value;
            const areaBBottomRightLngVal = document.getElementById("f8_areaB_bottomRightLng").value;
            const areaBBottomRightLatVal = document.getElementById("f8_areaB_bottomRightLat").value;
            const minDistanceValue = "0.5"; // F8的minDistance通常是固定的或内部处理

            performFrequentPathAnalysis2(
                kValue, minDistanceValue,
                areaATopLeftLngVal, areaATopLeftLatVal, areaABottomRightLngVal, areaABottomRightLatVal,
                areaBTopLeftLngVal, areaBTopLeftLatVal, areaBBottomRightLngVal, areaBBottomRightLatVal
            );
        });
    }

    if (pathSelectorF8) {
        pathSelectorF8.addEventListener("change", function () {
            const selectedIndex = Number.parseInt(this.value, 10);
            if (!isNaN(selectedIndex) && selectedIndex >= 0 && selectedIndex < f8PathFrequenciesData.length) {
                f8CurrentPathIndex = selectedIndex;
                displayF8PathOnMap(f8CurrentPathIndex); // 切换路径也算新任务，会触发全局清除
            }
        });
    }

    const prevPathBtnF8 = document.getElementById("f8PrevPathBtn");
    const nextPathBtnF8 = document.getElementById("f8NextPathBtn");

    if (prevPathBtnF8) {
        prevPathBtnF8.addEventListener("click", () => {
            if (!f8PathFrequenciesData || f8PathFrequenciesData.length === 0) return;
            f8CurrentPathIndex = (f8CurrentPathIndex - 1 + f8PathFrequenciesData.length) % f8PathFrequenciesData.length;
            if (pathSelectorF8) pathSelectorF8.value = f8CurrentPathIndex;
            displayF8PathOnMap(f8CurrentPathIndex);
        });
    }
    if (nextPathBtnF8) {
        nextPathBtnF8.addEventListener("click", () => {
            if (!f8PathFrequenciesData || f8PathFrequenciesData.length === 0) return;
            f8CurrentPathIndex = (f8CurrentPathIndex + 1) % f8PathFrequenciesData.length;
            if (pathSelectorF8) pathSelectorF8.value = f8CurrentPathIndex;
            displayF8PathOnMap(f8CurrentPathIndex);
        });
    }
    // 初始化F8在allFeatureOverlays中的条目
    if (typeof window.allFeatureOverlays !== 'object' || window.allFeatureOverlays === null) { window.allFeatureOverlays = {}; }
    if (!window.allFeatureOverlays["F8_路径覆盖物"]) { window.allFeatureOverlays["F8_路径覆盖物"] = []; }
    if (!window.allFeatureOverlays["F8_区域A_覆盖物"]) { window.allFeatureOverlays["F8_区域A_覆盖物"] = []; }
    if (!window.allFeatureOverlays["F8_区域B_覆盖物"]) { window.allFeatureOverlays["F8_区域B_覆盖物"] = []; }
});

function performFrequentPathAnalysis2(
    k, minDistance,
    valAreaATopLeftLng, valAreaATopLeftLat, valAreaABottomRightLng, valAreaABottomRightLat,
    valAreaBTopLeftLng, valAreaBTopLeftLat, valAreaBBottomRightLng, valAreaBBottomRightLat
) {
    const resultDivF8 = document.getElementById("f8_result");
    const pathSelectorContainerF8 = document.getElementById("f8PathSelectorContainer");

    resultDivF8.innerHTML = "<p>正在进行区域间频繁路径分析...</p>";
    if (pathSelectorContainerF8) pathSelectorContainerF8.style.display = "none";

    // --- 在执行任何F8特定操作前，调用全局清除函数 ---
    if (typeof clearOverlays === "function") {
        // console.log("F8 (performFrequentPathAnalysis2): 调用全局 clearOverlays()。");
        clearOverlays();
    } else {
        console.warn("F8 (performFrequentPathAnalysis2): 全局 clearOverlays() 函数未定义!");
    }

    // 绘制新的区域矩形 (这些会添加到 allFeatureOverlays)
    drawF8AreaRectanglesAndStore( // 重命名以示区分和明确其行为
        valAreaATopLeftLng, valAreaATopLeftLat, valAreaABottomRightLng, valAreaABottomRightLat, // 区域A
        valAreaBTopLeftLng, valAreaBTopLeftLat, valAreaBBottomRightLng, valAreaBBottomRightLat  // 区域B
    );

    const params = {
        k: Number.parseInt(k, 10),
        minPathDistanceKM: Number.parseFloat(minDistance),
        regionA: {
            minLon: Number.parseFloat(valAreaATopLeftLng), maxLat: Number.parseFloat(valAreaATopLeftLat),
            maxLon: Number.parseFloat(valAreaABottomRightLng), minLat: Number.parseFloat(valAreaABottomRightLat)
        },
        regionB: {
            minLon: Number.parseFloat(valAreaBTopLeftLng), maxLat: Number.parseFloat(valAreaBTopLeftLat),
            maxLon: Number.parseFloat(valAreaBBottomRightLng), minLat: Number.parseFloat(valAreaBBottomRightLat)
        }
    };
    // console.log("F8 发送给后端的参数:", JSON.stringify(params, null, 2));

    const baseURL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : '';
    const apiUrl = `${baseURL}/paths/frequent/regional`;
    const featureName = "F8区域间频繁路径分析";

    fetchApi(apiUrl, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(params) }, featureName)
        .then(data => {
            const f8ResultDiv = document.getElementById("f8_result"); // 使用局部变量
            const f8PathSelector = document.getElementById("f8_path_selector"); // 使用局部变量
            const f8PathContainer = document.getElementById("f8PathSelectorContainer"); // 使用局部变量

            if (data && data.pathFrequencies && data.pathFrequencies.length > 0) {
                f8PathFrequenciesData = data.pathFrequencies;
                f8CurrentPathIndex = 0;
                f8ResultDiv.innerHTML = `<p>区域间频繁路径分析结果：找到 ${f8PathFrequenciesData.length} 条频繁路径。</p>`;
                if (f8PathSelector) {
                    f8PathSelector.innerHTML = "";
                    f8PathFrequenciesData.forEach((pathFrequency, index) => {
                        const option = document.createElement("option");
                        option.value = index;
                        option.textContent = `路径 ${index + 1} (频率: ${pathFrequency.frequency})`;
                        f8PathSelector.appendChild(option);
                    });
                    f8PathSelector.value = 0;
                }
                if (f8PathContainer) f8PathContainer.style.display = "flex";
                displayF8PathOnMap(0); // 显示第一条路径（这也会触发一次全局清除，然后绘制区域和路径）
            } else {
                f8ResultDiv.innerHTML = "<p>未获取到有效的区域间频繁路径结果。</p>";
                if (f8PathContainer) f8PathContainer.style.display = "none";
                f8PathFrequenciesData = [];
            }
        })
        .catch(error => {
            displayFetchError(error, "f8_result", featureName);
            const f8PathContainer = document.getElementById("f8PathSelectorContainer");
            if (f8PathContainer) f8PathContainer.style.display = "none";
            f8PathFrequenciesData = [];
            if (typeof clearOverlays === "function") { clearOverlays(); } // 确保错误时也清理
        });
}

// F8区域绘制函数，将覆盖物存到allFeatureOverlays
function drawF8AreaRectanglesAndStore(
    valAreaATopLeftLng, valAreaATopLeftLat, valAreaABottomRightLng, valAreaABottomRightLat,
    valAreaBTopLeftLng, valAreaBTopLeftLat, valAreaBBottomRightLng, valAreaBBottomRightLat
) {
    const createRectAndLabelForF8 = (topLeftLngStr, topLeftLatStr, bottomRightLngStr, bottomRightLatStr, color, labelText, globalStorageKey) => {
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
            fillColor: color, fillOpacity: 0.3
        });
        map.addOverlay(polygon);
        window.allFeatureOverlays[globalStorageKey].push(polygon); // 添加到全局

        const label = new BMapGL.Label(labelText, {
            position: new BMapGL.Point((topLeftLng + bottomRightLng) / 2, (topLeftLat + bottomRightLat) / 2),
            offset: new BMapGL.Size(-20, -10)
        });
        label.setStyle({
            color: "white", backgroundColor: color, padding: "2px 5px",
            borderColor: "dark" + color, fontSize: "12px", borderRadius: "3px"
        });
        map.addOverlay(label);
        window.allFeatureOverlays[globalStorageKey].push(label); // 添加到全局
        return rectPoints;
    };

    const pointsA = createRectAndLabelForF8(valAreaATopLeftLng, valAreaATopLeftLat, valAreaABottomRightLng, valAreaABottomRightLat, "#FF0000", "区域A", "F8_区域A_覆盖物");
    const pointsB = createRectAndLabelForF8(valAreaBTopLeftLng, valAreaBTopLeftLat, valAreaBBottomRightLng, valAreaBBottomRightLat, "#0000FF", "区域B", "F8_区域B_覆盖物");

    // 调整视野以包含两个区域
    if (pointsA.length > 0 && pointsB.length > 0 && map && map.setViewport) {
        map.setViewport([...pointsA, ...pointsB]);
    } else if (pointsA.length > 0 && map && map.setViewport) {
        map.setViewport(pointsA);
    } else if (pointsB.length > 0 && map && map.setViewport) {
        map.setViewport(pointsB);
    }
}

function displayF8PathOnMap(pathIndex) {
    // 每次显示新路径前，都进行全局清除，然后重绘F8的区域和当前选定路径
    if (typeof clearOverlays === "function") {
        // console.log("F8 (displayF8PathOnMap): 为显示新路径，调用全局 clearOverlays()。");
        clearOverlays();
    }


    // ---F8区域 ---
    const areaATopLeftLngVal = document.getElementById("f8_areaA_topLeftLng").value;
    const areaATopLeftLatVal = document.getElementById("f8_areaA_topLeftLat").value;
    const areaABottomRightLngVal = document.getElementById("f8_areaA_bottomRightLng").value;
    const areaABottomRightLatVal = document.getElementById("f8_areaA_bottomRightLat").value;
    const areaBTopLeftLngVal = document.getElementById("f8_areaB_topLeftLng").value;
    const areaBTopLeftLatVal = document.getElementById("f8_areaB_topLeftLat").value;
    const areaBBottomRightLngVal = document.getElementById("f8_areaB_bottomRightLng").value;
    const areaBBottomRightLatVal = document.getElementById("f8_areaB_bottomRightLat").value;
    if(areaATopLeftLngVal && areaBTopLeftLngVal) { // 简单检查一下DOM是否还有值
        drawF8AreaRectanglesAndStore(
            areaATopLeftLngVal, areaATopLeftLatVal, areaABottomRightLngVal, areaABottomRightLatVal,
            areaBTopLeftLngVal, areaBTopLeftLatVal, areaBBottomRightLngVal, areaBBottomRightLatVal
        );
    }
    // --- F8区域 ---


    if (!f8PathFrequenciesData || pathIndex < 0 || pathIndex >= f8PathFrequenciesData.length) {
        return;
    }
    const pathData = f8PathFrequenciesData[pathIndex];
    if (!pathData || !pathData.pathCoordinates || pathData.pathCoordinates.length === 0) return;

    const coordinates = pathData.pathCoordinates;
    const bmapPoints = coordinates.map(coord => new BMapGL.Point(coord.longitude, coord.latitude));

    convertCoordinates(bmapPoints, (data) => {
        if (data.status === 0 && data.points && data.points.length > 0) {
            const polyline = new BMapGL.Polyline(data.points, {
                strokeColor: getF8PathColor(pathIndex), // 使用F8特定的颜色函数
                strokeWeight: 5, strokeOpacity: 0.8
            });
            map.addOverlay(polyline);
            window.allFeatureOverlays["F8_路径覆盖物"].push(polyline);

            if (data.points.length > 0) {
                const startDot = addDotToMap(data.points[0], "green");
                if(startDot) window.allFeatureOverlays["F8_路径覆盖物"].push(startDot);
                const endDot = addDotToMap(data.points[data.points.length - 1], "red");
                if(endDot) window.allFeatureOverlays["F8_路径覆盖物"].push(endDot);
            }
            // 调整视野以同时包含路径和已绘制的区域
        }
    });
}


function getF8PathColor(index) { // F8的颜色函数
    const colors = ["#E91E63", "#673AB7", "#03A9F4", "#009688", "#FF9800", "#F44336", "#795548", "#9E9E9E", "#607D8B"];
    return colors[index % colors.length];
}