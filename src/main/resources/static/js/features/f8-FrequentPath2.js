/**
 * F8: 区域间频繁路径分析
 */

// 全局变量，F8功能专属
let f8PathFrequenciesData = [];
let f8CurrentPathIndex = 0;
// window.f8PathPolylines 和 window.f8AreaRectangles 由 map-utils.js 的 clearOverlays 统一管理
// 或者此模块自己管理，并在功能切换时由 map-utils.js 调用特定清除函数
// 为了模块独立性和清晰性，F8模块自己管理其覆盖物数组是好的
window.f8PathPolylines = window.f8PathPolylines || [];
window.f8AreaRectangles = window.f8AreaRectangles || [];



document.addEventListener("DOMContentLoaded", () => {
    const frequentPath2Btn = document.getElementById("frequentPath2Btn");
    const pathSelectorF8 = document.getElementById("f8_path_selector"); // 使用 f8_path_selector
    const pathSelectorContainerF8 = document.getElementById("f8PathSelectorContainer");

    if (pathSelectorContainerF8) pathSelectorContainerF8.style.display = "none";

    if (frequentPath2Btn) {
        frequentPath2Btn.addEventListener("click", () => {
            const kValue = document.getElementById("f8_k").value;
            // 区域A坐标 (遵循HTML ID)
            const areaATopLeftLngVal = document.getElementById("f8_areaA_topLeftLng").value;
            const areaATopLeftLatVal = document.getElementById("f8_areaA_topLeftLat").value;
            const areaABottomRightLngVal = document.getElementById("f8_areaA_bottomRightLng").value;
            const areaABottomRightLatVal = document.getElementById("f8_areaA_bottomRightLat").value;
            // 区域B坐标
            const areaBTopLeftLngVal = document.getElementById("f8_areaB_topLeftLng").value;
            const areaBTopLeftLatVal = document.getElementById("f8_areaB_topLeftLat").value;
            const areaBBottomRightLngVal = document.getElementById("f8_areaB_bottomRightLng").value;
            const areaBBottomRightLatVal = document.getElementById("f8_areaB_bottomRightLat").value;

            // 前端基础非空检查 (可选，后端会校验)
            if (!kValue || !areaATopLeftLngVal || !areaATopLeftLatVal || !areaABottomRightLngVal || !areaABottomRightLatVal ||
                !areaBTopLeftLngVal || !areaBTopLeftLatVal || !areaBBottomRightLngVal || !areaBBottomRightLatVal) {
                alert("F8: 请填写参数k以及区域A和区域B的完整坐标。");
                return;
            }
            // HTML 中没有 F8 的 distance 输入，使用默认值
            // TODO: f8实际上是不需要这个参数的
            const minDistanceValue =  "0.5";


            clearF8OverlaysAndPaths(); // 清除F8之前的所有覆盖物

            // 绘制新的区域矩形。这些函数现在接收从DOM获取的值。
            drawF8AreaRectangles(
                areaATopLeftLngVal, areaATopLeftLatVal, areaABottomRightLngVal, areaABottomRightLatVal,
                areaBTopLeftLngVal, areaBTopLeftLatVal, areaBBottomRightLngVal, areaBBottomRightLatVal
            );

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
                displayF8PathOnMap(f8CurrentPathIndex);
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
});

// 主分析函数
function performFrequentPathAnalysis2(
    k, minDistance, // 新增 minDistance 参数
    valAreaATopLeftLng, valAreaATopLeftLat, valAreaABottomRightLng, valAreaABottomRightLat,
    valAreaBTopLeftLng, valAreaBTopLeftLat, valAreaBBottomRightLng, valAreaBBottomRightLat
) {
    const resultDivF8 = document.getElementById("f8_result");
    const pathSelectorContainerF8 = document.getElementById("f8PathSelectorContainer");

    if (!resultDivF8) { console.error("F8: 未找到 f8_result 元素"); return; }
    resultDivF8.innerHTML = "<p>正在进行区域间频繁路径分析...</p>";
    if (pathSelectorContainerF8) pathSelectorContainerF8.style.display = "none";
    // clearF8PathPolylines(); // 路径线在 displayF8PathOnMap 开始时清除

    // 构建请求参数，符合 FrequentPathQuery DTO
    // 约定: 左上角 (经度小, 纬度大), 右下角 (经度大, 纬度小)
    // Region DTO 字段: minLon, maxLat, maxLon, minLat
    const params = {
        k: Number.parseInt(k, 10),
        minPathDistanceKM: Number.parseFloat(minDistance),
        regionA: {
            minLon: Number.parseFloat(valAreaATopLeftLng),
            maxLat: Number.parseFloat(valAreaATopLeftLat), // 左上纬度 -> maxLat
            maxLon: Number.parseFloat(valAreaABottomRightLng),
            minLat: Number.parseFloat(valAreaABottomRightLat)  // 右下纬度 -> minLat
        },
        regionB: {
            minLon: Number.parseFloat(valAreaBTopLeftLng),
            maxLat: Number.parseFloat(valAreaBTopLeftLat),
            maxLon: Number.parseFloat(valAreaBBottomRightLng),
            minLat: Number.parseFloat(valAreaBBottomRightLat)
        }
    };
    console.log("F8 发送给后端的参数:", JSON.stringify(params, null, 2));

    const baseURL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : '';
    const apiUrl = `${baseURL}/paths/frequent/regional`;
    const featureName = "F8区域间频繁路径分析";

    // 使用通用的 fetchApi (已在 apiService.js 定义并引入)
    fetchApi(apiUrl, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(params) }, featureName)
        .then(data => { // data 是成功时解析后的业务数据
            const resultDiv = document.getElementById("f8_result"); // 确保正确获取
            const pathSelector = document.getElementById("f8_path_selector");
            const pathContainer = document.getElementById("f8PathSelectorContainer");

            if (resultDiv) {
                if (data && data.pathFrequencies && data.pathFrequencies.length > 0) {
                    f8PathFrequenciesData = data.pathFrequencies;
                    f8CurrentPathIndex = 0;

                    resultDiv.innerHTML = `<p>区域间频繁路径分析结果：找到 ${f8PathFrequenciesData.length} 条频繁路径。</p>`;

                    if (pathSelector) {
                        pathSelector.innerHTML = "";
                        f8PathFrequenciesData.forEach((pathFrequency, index) => {
                            const option = document.createElement("option");
                            option.value = index;
                            option.textContent = `路径 ${index + 1} (频率: ${pathFrequency.frequency})`;
                            pathSelector.appendChild(option);
                        });
                        pathSelector.value = 0;
                    }
                    if (pathContainer) pathContainer.style.display = "flex";
                    displayF8PathOnMap(0);
                } else {
                    resultDiv.innerHTML = "<p>未获取到有效的区域间频繁路径结果。</p>";
                    if (pathContainer) pathContainer.style.display = "none";
                    f8PathFrequenciesData = [];
                    clearF8PathPolylines();
                }
            }
        })
        .catch(error => { // error 对象由 fetchApi 抛出，message 已包含详情
            // 使用通用的 displayFetchError (已在 apiService.js 定义并引入)
            displayFetchError(error, "f8_result", featureName);

            const pathContainer = document.getElementById("f8PathSelectorContainer");
            if (pathContainer) pathContainer.style.display = "none";
            f8PathFrequenciesData = [];
            clearF8PathPolylines();
        });
}

// 绘制F8的两个区域矩形和标签
function drawF8AreaRectangles(
    valAreaATopLeftLng, valAreaATopLeftLat, valAreaABottomRightLng, valAreaABottomRightLat,
    valAreaBTopLeftLng, valAreaBTopLeftLat, valAreaBBottomRightLng, valAreaBBottomRightLat
) {

    const createRectAndLabel = (topLeftLngStr, topLeftLatStr, bottomRightLngStr, bottomRightLatStr, color, labelText, targetArray) => {
        try {
            const topLeftLng = Number.parseFloat(topLeftLngStr);
            const topLeftLat = Number.parseFloat(topLeftLatStr);
            const bottomRightLng = Number.parseFloat(bottomRightLngStr);
            const bottomRightLat = Number.parseFloat(bottomRightLatStr);

            // 遵循“左上角经度小、纬度大”约定来构造BMapGL点
            const tlPoint = new BMapGL.Point(topLeftLng, topLeftLat); // (minLon, maxLat)
            const brPoint = new BMapGL.Point(bottomRightLng, bottomRightLat); // (maxLon, minLat)

            const rectPoints = [
                tlPoint,                                     // 左上
                new BMapGL.Point(brPoint.lng, tlPoint.lat),  // 右上 (maxLon, maxLat)
                brPoint,                                     // 右下
                new BMapGL.Point(tlPoint.lng, brPoint.lat)   // 左下 (minLon, minLat)
            ];
            const polygon = new BMapGL.Polygon(rectPoints, {
                strokeColor: color, strokeWeight: 2, strokeOpacity: 1,
                fillColor: color, fillOpacity: 0.3
            });
            map.addOverlay(polygon);
            targetArray.push(polygon);

            const label = new BMapGL.Label(labelText, {
                // 标签位置在矩形中心
                position: new BMapGL.Point((topLeftLng + bottomRightLng) / 2, (topLeftLat + bottomRightLat) / 2),
                offset: new BMapGL.Size(-20, -10) // 根据标签大小调整偏移
            });
            label.setStyle({
                color: "white", backgroundColor: color, padding: "2px 5px",
                borderColor: "dark" + color, fontSize: "12px", borderRadius: "3px"
            });
            map.addOverlay(label);
            targetArray.push(label);
            return rectPoints;
        } catch (e) {
            console.error(`F8: 绘制区域 ${labelText} 时出错:`, e);
            return [];
        }
    };

    const pointsA = createRectAndLabel(valAreaATopLeftLng, valAreaATopLeftLat, valAreaABottomRightLng, valAreaABottomRightLat, "#FF0000", "区域A", window.f8AreaRectangles);
    const pointsB = createRectAndLabel(valAreaBTopLeftLng, valAreaBTopLeftLat, valAreaBBottomRightLng, valAreaBBottomRightLat, "#0000FF", "区域B", window.f8AreaRectangles);

    if (pointsA.length > 0 && pointsB.length > 0 && map) {
        map.setViewport([...pointsA, ...pointsB]);
    } else if (pointsA.length > 0 && map) {
        map.setViewport(pointsA);
    } else if (pointsB.length > 0 && map) {
        map.setViewport(pointsB);
    }
}

// 在地图上显示选中的F8路径
function displayF8PathOnMap(pathIndex) {
    if (!f8PathFrequenciesData || pathIndex < 0 || pathIndex >= f8PathFrequenciesData.length) {
        clearF8PathPolylines();
        return;
    }
    clearF8PathPolylines();

    const pathData = f8PathFrequenciesData[pathIndex];
    if (!pathData || !pathData.pathCoordinates || pathData.pathCoordinates.length === 0) return;

    const coordinates = pathData.pathCoordinates;
    const bmapPoints = coordinates.map(coord => new BMapGL.Point(coord.longitude, coord.latitude));

    if (typeof convertCoordinates === "function" && typeof addDotToMap === "function" && typeof map !== "undefined" && BMapGL) {
        convertCoordinates(bmapPoints, (data) => {
            if (data.status === 0 && data.points && data.points.length > 0) {
                const polyline = new BMapGL.Polyline(data.points, { strokeColor: getF8PathColor(pathIndex), strokeWeight: 5, strokeOpacity: 0.8 });
                map.addOverlay(polyline);
                window.f8PathPolylines.push(polyline);

                const startDot = addDotToMap(data.points[0], "green");
                if(startDot) window.f8PathPolylines.push(startDot);
                const endDot = addDotToMap(data.points[data.points.length - 1], "red");
                if(endDot) window.f8PathPolylines.push(endDot);


            } else {
                console.error("F8: 路径坐标转换失败。", data);
            }
        });
    } else {
        console.error("F8: displayF8PathOnMap - 缺少地图工具函数。");
    }
}

// 清除F8的所有相关覆盖物 (区域和路径)
function clearF8OverlaysAndPaths() {
    if (window.f8AreaRectangles && Array.isArray(window.f8AreaRectangles)) {
        window.f8AreaRectangles.forEach(overlay => {
            if (map && map.removeOverlay && overlay) try { map.removeOverlay(overlay); } catch (e) { /* मौन */ }
        });
    }
    window.f8AreaRectangles = [];
    clearF8PathPolylines();
}

// 只清除F8的路径线和相关点
function clearF8PathPolylines() {
    if (window.f8PathPolylines && Array.isArray(window.f8PathPolylines)) {
        window.f8PathPolylines.forEach(overlay => {
            if (map && map.removeOverlay && overlay) try { map.removeOverlay(overlay); } catch (e) { /* मौन */ }
        });
    }
    window.f8PathPolylines = [];
}

// F8路径颜色
function getF8PathColor(index) {
    const colors = ["#FF5252", "#4CAF50", "#2196F3", "#FFC107", "#9C27B0", "#00BCD4", "#FF9800", "#795548", "#607D8B", "#E91E63"];
    return colors[index % colors.length];
}