/**
 * F9: 通信时间分析
 * 分析两个矩形区域间的最短通信时间，并在地图上显示
 */

// 全局变量，用于存储地图覆盖物
let f9AreaRectangles = []; // 存储区域矩形覆盖物
let f9PathPolyline = null; // 存储路径折线对象

const communicationTimeBtn = document.getElementById("communicationTimeBtn");

if (communicationTimeBtn) {
    communicationTimeBtn.addEventListener("click", () => {
        // 使用 const 获取 DOM 元素的值
        const startTimeStr = document.getElementById("f9_startTime").value;
        const endTimeStr = document.getElementById("f9_endTime").value;
        const areaATopLeftLng = document.getElementById("f9_areaA_topLeftLng").value;
        const areaATopLeftLat = document.getElementById("f9_areaA_topLeftLat").value;
        const areaABottomRightLng = document.getElementById("f9_areaA_bottomRightLng").value;
        const areaABottomRightLat = document.getElementById("f9_areaA_bottomRightLat").value;
        const areaBTopLeftLng = document.getElementById("f9_areaB_topLeftLng").value;
        const areaBTopLeftLat = document.getElementById("f9_areaB_topLeftLat").value;
        const areaBBottomRightLng = document.getElementById("f9_areaB_bottomRightLng").value;
        const areaBBottomRightLat = document.getElementById("f9_areaB_bottomRightLat").value;

        // --- 客户端验证 ---
        if (
            !startTimeStr ||
            !endTimeStr ||
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

        const parsedStartTime = new Date(startTimeStr);
        const parsedEndTime = new Date(endTimeStr);

        if (isNaN(parsedStartTime.getTime()) || isNaN(parsedEndTime.getTime())) {
            alert(
                "时间格式无效，请确保输入的时间是有效的日期和时间格式。\n例如: YYYY-MM-DDTHH:mm:ss 或 YYYY/MM/DD HH:mm:ss",
            );
            return;
        }

        if (parsedStartTime >= parsedEndTime) {
            alert("开始时间必须早于结束时间。");
            return;
        }

        const coords = [
            areaATopLeftLng,
            areaATopLeftLat,
            areaABottomRightLng,
            areaABottomRightLat,
            areaBTopLeftLng,
            areaBTopLeftLat,
            areaBBottomRightLng,
            areaBBottomRightLat,
        ];
        for (const coord of coords) {
            if (isNaN(Number.parseFloat(coord))) {
                alert(`坐标值 "${coord}" 无效，请输入数字。`);
                return;
            }
        }

        // 清除之前的覆盖物
        clearF9Overlays();

        // 绘制区域A和区域B的矩形
        drawF9AreaRectangles(
            areaATopLeftLng,
            areaATopLeftLat,
            areaABottomRightLng,
            areaABottomRightLat,
            areaBTopLeftLng,
            areaBTopLeftLat,
            areaBBottomRightLng,
            areaBBottomRightLat,
        );

        // 调用分析函数
        performCommunicationTimeAnalysis(
            startTimeStr,
            endTimeStr,
            areaATopLeftLng,
            areaATopLeftLat,
            areaABottomRightLng,
            areaABottomRightLat,
            areaBTopLeftLng,
            areaBTopLeftLat,
            areaBBottomRightLng,
            areaBBottomRightLat,
        );
    });
}

// 绘制区域A和区域B的矩形
function drawF9AreaRectangles(
    areaATopLeftLng,
    areaATopLeftLat,
    areaABottomRightLng,
    areaABottomRightLat,
    areaBTopLeftLng,
    areaBTopLeftLat,
    areaBBottomRightLng,
    areaBBottomRightLat,
) {
    // 创建区域A的矩形
    const areaAPoints = [
        new BMapGL.Point(Number.parseFloat(areaATopLeftLng), Number.parseFloat(areaATopLeftLat)),
        new BMapGL.Point(Number.parseFloat(areaABottomRightLng), Number.parseFloat(areaATopLeftLat)),
        new BMapGL.Point(Number.parseFloat(areaABottomRightLng), Number.parseFloat(areaABottomRightLat)),
        new BMapGL.Point(Number.parseFloat(areaATopLeftLng), Number.parseFloat(areaABottomRightLat)),
    ];

    const areaAPolygon = new BMapGL.Polygon(areaAPoints, {
        strokeColor: "#FF0000",
        strokeWeight: 2,
        strokeOpacity: 1,
        fillColor: "#FF0000",
        fillOpacity: 0.3,
    });
    map.addOverlay(areaAPolygon);

    // 添加区域A标签
    const areaALabel = new BMapGL.Label("区域A", {
        position: new BMapGL.Point(
            (Number.parseFloat(areaATopLeftLng) + Number.parseFloat(areaABottomRightLng)) / 2,
            (Number.parseFloat(areaATopLeftLat) + Number.parseFloat(areaABottomRightLat)) / 2,
        ),
        offset: new BMapGL.Size(0, 0),
    });
    areaALabel.setStyle({
        color: "#fff",
        backgroundColor: "rgba(255, 0, 0, 0.8)",
        border: "none",
        fontSize: "14px",
        padding: "5px 10px",
        borderRadius: "3px",
    });
    map.addOverlay(areaALabel);

    // 创建区域B的矩形
    const areaBPoints = [
        new BMapGL.Point(Number.parseFloat(areaBTopLeftLng), Number.parseFloat(areaBTopLeftLat)),
        new BMapGL.Point(Number.parseFloat(areaBBottomRightLng), Number.parseFloat(areaBTopLeftLat)),
        new BMapGL.Point(Number.parseFloat(areaBBottomRightLng), Number.parseFloat(areaBBottomRightLat)),
        new BMapGL.Point(Number.parseFloat(areaBTopLeftLng), Number.parseFloat(areaBBottomRightLat)),
    ];

    const areaBPolygon = new BMapGL.Polygon(areaBPoints, {
        strokeColor: "#0000FF",
        strokeWeight: 2,
        strokeOpacity: 1,
        fillColor: "#0000FF",
        fillOpacity: 0.3,
    });
    map.addOverlay(areaBPolygon);

    // 添加区域B标签
    const areaBLabel = new BMapGL.Label("区域B", {
        position: new BMapGL.Point(
            (Number.parseFloat(areaBTopLeftLng) + Number.parseFloat(areaBBottomRightLng)) / 2,
            (Number.parseFloat(areaBTopLeftLat) + Number.parseFloat(areaBBottomRightLat)) / 2,
        ),
        offset: new BMapGL.Size(0, 0),
    });
    areaBLabel.setStyle({
        color: "#fff",
        backgroundColor: "rgba(0, 0, 255, 0.8)",
        border: "none",
        fontSize: "14px",
        padding: "5px 10px",
        borderRadius: "3px",
    });
    map.addOverlay(areaBLabel);

    // 存储矩形覆盖物以便后续清除
    f9AreaRectangles = [areaAPolygon, areaALabel, areaBPolygon, areaBLabel];

    // 调整地图视野以包含两个区域
    const allPoints = [...areaAPoints, ...areaBPoints];
    map.setViewport(allPoints);
}

// 绘制最短路径
function drawF9ShortestPath(pathPoints) {
    if (!pathPoints || pathPoints.length < 2) return;

    // 创建路径点数组
    const points = pathPoints.map(
        (point) => new BMapGL.Point(Number.parseFloat(point.longitude), Number.parseFloat(point.latitude)),
    );

    // 创建折线
    f9PathPolyline = new BMapGL.Polyline(points, {
        strokeColor: "#00FF00",
        strokeWeight: 4,
        strokeOpacity: 0.8,
    });
    map.addOverlay(f9PathPolyline);

    // 添加起点和终点标记
    const startMarker = new BMapGL.Marker(points[0]);
    const endMarker = new BMapGL.Marker(points[points.length - 1]);
    map.addOverlay(startMarker);
    map.addOverlay(endMarker);

    // 将标记也添加到覆盖物数组中，以便后续清除
    f9AreaRectangles.push(startMarker, endMarker);
}

// 清除F9功能的所有覆盖物
function clearF9Overlays() {
    // 清除区域矩形和标记
    f9AreaRectangles.forEach((overlay) => {
        map.removeOverlay(overlay);
    });
    f9AreaRectangles = [];

    // 清除路径折线
    if (f9PathPolyline) {
        map.removeOverlay(f9PathPolyline);
        f9PathPolyline = null;
    }
}

function performCommunicationTimeAnalysis(
    startTime,
    endTime,
    areaATopLeftLngStr,
    areaATopLeftLatStr,
    areaABottomRightLngStr,
    areaABottomRightLatStr,
    areaBTopLeftLngStr,
    areaBTopLeftLatStr,
    areaBBottomRightLngStr,
    areaBBottomRightLatStr,
) {
    const resultDiv = document.getElementById("f9_result");
    if (!resultDiv) {
        console.error("未找到 f9_result 元素");
        return;
    }
    resultDiv.innerHTML = "<p>正在进行通信时间分析...</p>";

    // 解析经纬度字符串为浮点数
    const atlLng = Number.parseFloat(areaATopLeftLngStr);
    const atlLat = Number.parseFloat(areaATopLeftLatStr);
    const abrLng = Number.parseFloat(areaABottomRightLngStr);
    const abrLat = Number.parseFloat(areaABottomRightLatStr);

    const btlLng = Number.parseFloat(areaBTopLeftLngStr);
    const btlLat = Number.parseFloat(areaBTopLeftLatStr);
    const bbrLng = Number.parseFloat(areaBBottomRightLngStr);
    const bbrLat = Number.parseFloat(areaBBottomRightLatStr);

    const params = {
        regionA: {
            minLon: Math.min(atlLng, abrLng),
            minLat: Math.min(atlLat, abrLat),
            maxLon: Math.max(atlLng, abrLng),
            maxLat: Math.max(atlLat, abrLat),
        },
        regionB: {
            minLon: Math.min(btlLng, bbrLng),
            minLat: Math.min(btlLat, bbrLat),
            maxLon: Math.max(btlLng, bbrLng),
            maxLat: Math.max(btlLat, bbrLat),
        },
        startTime: startTime,
        endTime: endTime,
    };

    const baseURL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : '';
    const apiUrl = `${baseURL}/travelTime/analyze`;

    fetch(apiUrl, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(params),
    })
        .then((response) => {
            if (!response.ok) {
                return response.text().then((text) => {
                    throw new Error(`网络响应异常，状态码: ${response.status}, 错误信息: ${text || response.statusText}`);
                });
            }
            return response.json();
        })
        .then((data) => {
            console.log("后端返回的数据:", data);
            if (data && data.found) {
                let resultHtml = `<p>通信时间分析结果：</p><ul>`;

                if (data.minTravelTimeFormatted) {
                    resultHtml += `<li>最短通行时间: ${data.minTravelTimeFormatted}</li>`;
                } else {
                    resultHtml += `<li>最短通行时间: 未提供或计算错误</li>`;
                }

                if (data.shortestPath && data.shortestPath.length > 0) {
                    resultHtml += `<li>出租车ID: ${data.shortestPath[0].taxiId}</li>`;
                    resultHtml += "<li>轨迹点：</li><ul>";
                    data.shortestPath.forEach((point) => {
                        const pointTimestamp = point.timestamp ? new Date(point.timestamp).toLocaleString() : "N/A";
                        resultHtml += `<li>时间: ${pointTimestamp}, 经度: ${point.longitude}, 纬度: ${point.latitude}</li>`;
                    });
                    resultHtml += "</ul>";

                    // 绘制最短路径
                    drawF9ShortestPath(data.shortestPath);
                }
                resultHtml += "</ul>";
                resultDiv.innerHTML = resultHtml;
            } else if (data && data.message) {
                resultDiv.innerHTML = `<p>分析提示：${data.message}</p>`;
            } else {
                resultDiv.innerHTML = "<p>未获取到有效的分析结果，或未找到符合条件的路径。</p>";
            }
        })
        .catch((error) => {
            resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`;
            console.error("Error during API call:", error);
        });
}