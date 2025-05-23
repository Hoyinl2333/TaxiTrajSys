document.addEventListener("DOMContentLoaded", () => {
    // 存储 f5 模块创建的覆盖物
    window.f5Overlays = []

    let timeData = {} // 存储时间点和对应的车流量数据
    let timePoints = [] // 存储所有时间点
    let currentTimeIndex = 0 // 当前时间点索引

    // 获取时间选择器元素
    const prevTimeBtn = document.getElementById("f5_prevTime")
    const nextTimeBtn = document.getElementById("f5_nextTime")
    const currentTimeDisplay = document.getElementById("f5_currentTime")

    // 添加时间选择器按钮事件
    if (prevTimeBtn && nextTimeBtn) {
        prevTimeBtn.addEventListener("click", () => {
            if (timePoints.length > 0 && currentTimeIndex > 0) {
                currentTimeIndex--
                updateFlowDisplay()
            }
        })

        nextTimeBtn.addEventListener("click", () => {
            if (timePoints.length > 0 && currentTimeIndex < timePoints.length - 1) {
                currentTimeIndex++
                updateFlowDisplay()
            }
        })
    }

    // 更新车流量显示
    function updateFlowDisplay() {
        if (timePoints.length === 0) return

        const currentTime = timePoints[currentTimeIndex]
        const flows = timeData[currentTime]

        // 更新时间显示
        if (currentTimeDisplay) {
            // 格式化时间显示
            const dateObj = new Date(currentTime)
            const formattedTime = dateObj.toLocaleString("zh-CN", {
                year: "numeric",
                month: "2-digit",
                day: "2-digit",
                hour: "2-digit",
                minute: "2-digit",
            })
            currentTimeDisplay.value = formattedTime
        }

        // 更新车流量显示
        document.getElementById("flow_1_to_2").textContent = flows[0]
        document.getElementById("flow_2_to_1").textContent = flows[1]

        // 显示车流量区域
        document.getElementById("f5_flow_display").style.display = "block"
    }

    const areaCorrelation1Btn = document.getElementById("areaCorrelation1Btn")

    if (areaCorrelation1Btn) {
        areaCorrelation1Btn.addEventListener("click", () => {
            const startTime = document.getElementById("f5_startTime").value
            const endTime = document.getElementById("f5_endTime").value
            const area1TopLeftLng = document.getElementById("f5_area1_topLeftLng").value
            const area1TopLeftLat = document.getElementById("f5_area1_topLeftLat").value
            const area1BottomRightLng = document.getElementById("f5_area1_bottomRightLng").value
            const area1BottomRightLat = document.getElementById("f5_area1_bottomRightLat").value
            const area2TopLeftLng = document.getElementById("f5_area2_topLeftLng").value
            const area2TopLeftLat = document.getElementById("f5_area2_topLeftLat").value
            const area2BottomRightLng = document.getElementById("f5_area2_bottomRightLng").value
            const area2BottomRightLat = document.getElementById("f5_area2_bottomRightLat").value
            const timeSlotMinutes = document.getElementById("f5_timeInterval").value

            if (
                !startTime ||
                !endTime ||
                !area1TopLeftLng ||
                !area1TopLeftLat ||
                !area1BottomRightLng ||
                !area1BottomRightLat ||
                !area2TopLeftLng ||
                !area2TopLeftLat ||
                !area2BottomRightLng ||
                !area2BottomRightLat
            ) {
                alert("请填写完整的分析条件")
                return
            }

            // 清除之前的覆盖物
            clearF5Overlays()

            // 绘制区域
            if (window.map) { // 确保全局 map 对象存在
                drawAreaOnMap(
                    area1TopLeftLng,
                    area1TopLeftLat,
                    area1BottomRightLng,
                    area1BottomRightLat,
                    "blue", // 区域1的颜色
                    "区域1"  // 区域1的标签
                );

                drawAreaOnMap(
                    area2TopLeftLng,
                    area2TopLeftLat,
                    area2BottomRightLng,
                    area2BottomRightLat,
                    "red",  // 区域2的颜色
                    "区域2" // 区域2的标签
                );
            }

            // （可选）调整地图视野以适应绘制的区域
            const pointsToView = [
                new BMapGL.Point(area1TopLeftLng, area1BottomRightLat),
                new BMapGL.Point(area1BottomRightLng, area1TopLeftLat),
                new BMapGL.Point(area2TopLeftLng, area2BottomRightLat),
                new BMapGL.Point(area2BottomRightLng, area2TopLeftLat),
            ];
            map.setViewport(pointsToView, {margins:[30,30,30,30]});

            performAreaCorrelationAnalysis1(
                startTime,
                endTime,
                area1TopLeftLng,
                area1TopLeftLat,
                area1BottomRightLng,
                area1BottomRightLat,
                area2TopLeftLng,
                area2TopLeftLat,
                area2BottomRightLng,
                area2BottomRightLat,
            )
        })
    }

    // 清除 f5 模块创建的覆盖物
    window.clearF5Overlays = () => {
        if (window.f5Overlays && Array.isArray(window.f5Overlays)) {
            window.f5Overlays.forEach((overlay) => {
                if (map && map.removeOverlay) {
                    map.removeOverlay(overlay)
                }
            })
            window.f5Overlays = []
        }
    }

    function performAreaCorrelationAnalysis1(
        startTime,
        endTime,
        a1TLLng,
        a1TLLat,
        a1BRLng,
        a1BRLat,
        a2TLLng,
        a2TLLat,
        a2BRLng,
        a2BRLat,
    ) {
        const resultDiv = document.getElementById("f5_result");
        if (!resultDiv) {
            console.error("未找到 f5_result 元素");
            return;
        }
        resultDiv.innerHTML = "<p>正在进行区域关联分析...</p>";

        const params = {
            startTime: startTime, // 直接使用传入的ISO格式startTime
            endTime: endTime,     // 直接使用传入的ISO格式endTime
            timeSlotMinutes: Number.parseInt(document.getElementById("f5_timeInterval").value),
            region1: {
                minLon: Number.parseFloat(a1TLLng), // 使用传入的参数构建
                maxLat: Number.parseFloat(a1TLLat),
                maxLon: Number.parseFloat(a1BRLng),
                minLat: Number.parseFloat(a1BRLat)
            },
            region2: {
                minLon: Number.parseFloat(a2TLLng),
                maxLat: Number.parseFloat(a2TLLat),
                maxLon: Number.parseFloat(a2BRLng),
                minLat: Number.parseFloat(a2BRLat)
            }
        };
        // 调试：打印将要发送的参数
        console.log("发送给F5后端的参数:", JSON.stringify(params, null, 2));


        const baseURL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : '';
        const apiUrl = `${baseURL}/correlation/trafficFlowChangeBetweenRegions`;

        fetch(apiUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(params),
        })
            .then(async (response) => { // 使用 async 关键字，以便在内部使用 await
                //console.log("F5 响应状态:", response.status, "OK状态:", response.ok); // 调试日志
                if (!response.ok) {
                    let errorData;
                    let responseTextForDebug = "";
                    try {
                        // 克隆响应对象，因为 .json() 或 .text() 会消耗响应体
                        const clonedResponse = response.clone();
                        errorData = await response.json(); // 尝试异步解析JSON
                        console.log("F5 成功解析后端错误JSON:", errorData); // 调试日志
                    } catch (e) {
                        console.warn("F5 解析后端错误响应为JSON失败:", e);
                        // 如果JSON解析失败，尝试获取文本响应作为调试信息
                        try {
                            // 使用之前克隆的响应对象来读取文本
                            responseTextForDebug = await clonedResponse.text();
                            console.warn("F5 后端返回的原始文本内容:", responseTextForDebug);
                        } catch (textError) {
                            console.error("F5 读取错误响应的文本内容也失败:", textError);
                        }
                        errorData = null;
                    }

                    // 构建错误消息
                    let errorMessage = `网络响应异常，状态码: ${response.status}`;
                    if (errorData && errorData.details && errorData.details.length > 0) {
                        const detailsHtml = errorData.details.map(detail => `<li>${escapeHtml(detail)}</li>`).join('');
                        errorMessage += `<br/>详情如下:<ul>${detailsHtml}</ul>`;
                    } else if (errorData && errorData.message) {
                        errorMessage += `<br/>服务器消息: ${escapeHtml(errorData.message)}`;
                    } else if (responseTextForDebug) {
                        errorMessage += `<br/>服务器原始响应 (部分): ${escapeHtml(responseTextForDebug.substring(0, 200))}`; // 显示部分原始响应
                    } else {
                        errorMessage += `。无法获取详细错误信息。`;
                    }

                    // 创建一个Error对象并抛出，它将被下面的 .catch 捕获
                    const customError = new Error(errorMessage);
                    if (errorData) customError.errorData = errorData; // 可选: 将解析出的错误数据附加到Error对象
                    throw customError;
                }
                return response.json(); // 如果 response.ok 为 true，正常解析业务数据
            })
            .then((data) => { // 此 .then 只处理成功的业务响应
                const resultDiv = document.getElementById("f5_result");
                if (resultDiv) { // 确保元素存在
                    if (data && data.trafficFlowChange && Object.keys(data.trafficFlowChange).length > 0) {
                        timeData = data.trafficFlowChange;
                        timePoints = Object.keys(data.trafficFlowChange).sort();
                        currentTimeIndex = 0;
                        updateFlowDisplay();

                        let resultHtml = "<p>区域关联分析结果：</p>";
                        resultHtml +=
                            "<p>已获取到 " +
                            timePoints.length +
                            " 个时间点的车流量数据。请使用上方的时间选择器浏览不同时间点的数据。</p>";
                        resultDiv.innerHTML = resultHtml;
                    } else {
                        resultDiv.innerHTML = "<p>未获取到有效的分析结果。</p>";
                        document.getElementById("f5_flow_display").style.display = "none";
                    }
                }
            })
            .catch((error) => { // 此 .catch 捕获所有前面Promise链中抛出的错误
                const resultDiv = document.getElementById("f5_result");
                let displayMessage = "查询出错：";

                if (error && error.message) {
                    displayMessage += `<br/>${error.message}`; // error.message 现在应包含我们构造的详细信息
                } else {
                    displayMessage += "<br/>发生未知错误。";
                }

                if (resultDiv) {
                    resultDiv.innerHTML = `<p class="error-message">${displayMessage}</p>`;
                }
                // 在控制台打印更详细的错误对象，以便调试
                console.error("查询出错详情 (Error Object):", error);
                if (error && error.errorData) { // 如果我们附加了原始解析的错误数据
                    console.error("后端返回的原始ErrorData:", error.errorData);
                }
            });
    }

    // 修改后的绘制区域函数，增加区域标签
    function drawAreaOnMap(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat, color, labelText) {
        try {
            const topLeftPoint = new BMapGL.Point(Number.parseFloat(topLeftLng), Number.parseFloat(topLeftLat))
            const bottomRightPoint = new BMapGL.Point(Number.parseFloat(bottomRightLng), Number.parseFloat(bottomRightLat));

            // 创建矩形
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
                },
            )
            map.addOverlay(rectangle)

            // 添加区域标签
            const label = new BMapGL.Label(labelText, {
                position: new BMapGL.Point(
                    (Number.parseFloat(topLeftLng) + Number.parseFloat(bottomRightLng)) / 2,
                    (Number.parseFloat(topLeftLat) + Number.parseFloat(bottomRightLat)) / 2
                ),
                offset: new BMapGL.Size(0, 0),
            })
            label.setStyle({
                color: "#fff",
                backgroundColor: `rgba(${color === "blue" ? "0,0,255" : "255,0,0"}, 0.8)`,
                border: "none",
                fontSize: "14px",
                padding: "5px 10px",
                borderRadius: "3px",
            })
            map.addOverlay(label)

            // 将覆盖物添加到全局数组以便后续清除
            window.f5Overlays.push(rectangle, label)
        } catch (error) {
            console.error("绘制区域时出错:", error)
        }
    }

})