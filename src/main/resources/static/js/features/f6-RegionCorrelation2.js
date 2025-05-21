document.addEventListener("DOMContentLoaded", () => {
    // 存储 f6 模块创建的覆盖物（代码2新增）
    window.f6Overlays = [];

    let timeData = {}; // 存储时间点和对应的车流量数据
    let timePoints = []; // 存储所有时间点
    let currentTimeIndex = 0; // 当前时间点索引

    // 获取时间选择器元素
    const prevTimeBtn = document.getElementById("f6_prevTime");
    const nextTimeBtn = document.getElementById("f6_nextTime");
    const currentTimeDisplay = document.getElementById("f6_currentTime");

    // 添加时间选择器按钮事件
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

    // 更新车流量显示
    function updateFlowDisplay() {
        if (timePoints.length === 0) return;

        const currentTime = timePoints[currentTimeIndex];
        const flows = timeData[currentTime];

        // 更新时间显示
        if (currentTimeDisplay) {
            const dateObj = new Date(currentTime);
            const formattedTime = dateObj.toLocaleString("zh-CN", {
                year: "numeric",
                month: "2-digit",
                day: "2-digit",
                hour: "2-digit",
                minute: "2-digit",
            });
            currentTimeDisplay.value = formattedTime;
        }

        // 更新车流量显示
        document.getElementById("flow_enter").textContent = flows[0];
        document.getElementById("flow_leave").textContent = flows[1];

        // 显示车流量区域
        document.getElementById("f6_flow_display").style.display = "block";
    }

    const areaCorrelation2Btn = document.getElementById("areaCorrelation2Btn");

    if (areaCorrelation2Btn) {
        areaCorrelation2Btn.addEventListener("click", () => {
            const startTime = new Date(document.getElementById("f6_startTime").value).toISOString();
            const endTime = new Date(document.getElementById("f6_endTime").value).toISOString();
            const topLeftLng = document.getElementById("f6_topLeftLng").value;
            const topLeftLat = document.getElementById("f6_topLeftLat").value;
            const bottomRightLng = document.getElementById("f6_bottomRightLng").value;
            const bottomRightLat = document.getElementById("f6_bottomRightLat").value;
            const timeSlotMinutes = document.getElementById("f6_timeInterval").value;

            if (!startTime || !endTime || !topLeftLng || !topLeftLat || !bottomRightLng || !bottomRightLat) {
                alert("请填写完整的分析条件");
                return;
            }

            // 清除之前的覆盖物（代码2新增）
            clearF6Overlays();

            performAreaCorrelationAnalysis2(startTime, endTime, topLeftLng, topLeftLat, bottomRightLng, bottomRightLat);
        });
    }

    // 清除 f6 模块创建的覆盖物（代码2新增）
    window.clearF6Overlays = () => {
        if (window.f6Overlays && Array.isArray(window.f6Overlays)) {
            window.f6Overlays.forEach((overlay) => {
                if (map && map.removeOverlay) {
                    map.removeOverlay(overlay);
                }
            });
            window.f6Overlays = [];
        }
    };

    function performAreaCorrelationAnalysis2(startTime, endTime, topLeftLng, topLeftLat, bottomRightLng, bottomRightLat) {
        const resultDiv = document.getElementById("f6_result");
        if (!resultDiv) {
            console.error("未找到 f6_result 元素");
            return;
        }
        resultDiv.innerHTML = "<p>正在进行区域关联分析...</p>";

        const params = {
            startTime,
            endTime,
            topLeftLongitude: Number.parseFloat(topLeftLng),
            topLeftLatitude: Number.parseFloat(topLeftLat),
            bottomRightLongitude: Number.parseFloat(bottomRightLng),
            bottomRightLatitude: Number.parseFloat(bottomRightLat),
            timeSlotMinutes: Number.parseInt(document.getElementById("f6_timeInterval").value),
        };
        // 注意接口路径差异：代码1为/correlation/，代码2为/SingleCorrelation/，需根据实际后端调整
        const baseURL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
        const apiUrl = `${baseURL}/correlation/trafficFlowChangeWithOtherRegions`; // 保持代码1的路径（需确认实际接口）

        fetch(apiUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(params),
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`网络响应异常，状态码: ${response.status}`);
                }
                return response.json();
            })
            .then((data) => {
                // 兼容数据结构差异：代码1使用data.trafficFlowChange，代码2直接使用data
                const trafficData = data.trafficFlowChange || data; // 自动兼容两种结构
                if (trafficData && Object.keys(trafficData).length > 0) {
                    timeData = trafficData;
                    timePoints = Object.keys(trafficData).sort();
                    currentTimeIndex = 0;
                    updateFlowDisplay();

                    const resultHtml = `
            <p>区域关联分析结果：</p>
            <p>已获取到 ${timePoints.length} 个时间点的车流量数据。请使用上方的时间选择器浏览不同时间点的数据。</p>
          `;
                    resultDiv.innerHTML = resultHtml;
                } else {
                    resultDiv.innerHTML = "<p>未获取到有效的分析结果。</p>";
                    document.getElementById("f6_flow_display").style.display = "none";
                }

                if (typeof map !== "undefined" && map !== null) {
                    drawAreaOnMap(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat, "blue");
                }
            })
            .catch((error) => {
                resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`;
                console.error("Error:", error);
            });
    }

    function drawAreaOnMap(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat, color) {
        try {
            const topLeftPoint = new BMapGL.Point(Number.parseFloat(topLeftLng), Number.parseFloat(topLeftLat));
            const bottomRightPoint = new BMapGL.Point(Number.parseFloat(bottomRightLng), Number.parseFloat(topLeftLat));
            const rectangle = new BMapGL.Polygon(
                [
                    topLeftPoint,
                    new BMapGL.Point(Number.parseFloat(bottomRightLng), Number.parseFloat(topLeftLat)),
                    bottomRightPoint,
                    new BMapGL.Point(Number.parseFloat(topLeftLng), Number.parseFloat(bottomRightLat)),
                ],
                {
                    strokeColor: color,
                    strokeWeight: 2,
                    strokeOpacity: 0.8,
                    fillColor: color,
                    fillOpacity: 0.2,
                }
            );
            if (map && map.addOverlay) {
                map.addOverlay(rectangle);
            }
            // 新增：存储覆盖物以便清除（代码2功能）
            window.f6Overlays.push(rectangle);
        } catch (error) {
            console.error("绘制区域时出错:", error);
        }
    }

    // 代码2新增：检查地图对象是否初始化
    if (typeof map === "undefined") {
        console.warn("map 未定义，请确保已加载百度地图 API");
    }
    if (typeof BMapGL === "undefined") {
        console.warn("BMapGL 未定义，请确保已加载百度地图 GL API");
    }
});