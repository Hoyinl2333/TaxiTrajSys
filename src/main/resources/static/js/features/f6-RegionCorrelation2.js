document.addEventListener("DOMContentLoaded", () => {
    // 存储 f6 模块创建的覆盖物
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
        if (!timeData[currentTime]) {
            console.warn("F6: 当前时间点数据未找到:", currentTime, timeData);
            document.getElementById("flow_enter").textContent = "-";
            document.getElementById("flow_leave").textContent = "-";
            if (currentTimeDisplay) currentTimeDisplay.value = "无数据";
            return;
        }
        const flows = timeData[currentTime];

        if (currentTimeDisplay) {
            const dateObj = new Date(currentTime);
            const formattedTime = dateObj.toLocaleString("zh-CN", { /* ... format options ... */ });
            currentTimeDisplay.value = formattedTime;
        }
        document.getElementById("flow_enter").textContent = flows[0];
        document.getElementById("flow_leave").textContent = flows[1];
        document.getElementById("f6_flow_display").style.display = "block";
    }

    const areaCorrelation2Btn = document.getElementById("areaCorrelation2Btn");

    if (areaCorrelation2Btn) {
        areaCorrelation2Btn.addEventListener("click", () => {
            // 从DOM获取所有输入值
            const startTimeValue = document.getElementById("f6_startTime").value;
            const endTimeValue = document.getElementById("f6_endTime").value;
            const topLeftLngValue = document.getElementById("f6_topLeftLng").value;
            const topLeftLatValue = document.getElementById("f6_topLeftLat").value;
            const bottomRightLngValue = document.getElementById("f6_bottomRightLng").value;
            const bottomRightLatValue = document.getElementById("f6_bottomRightLat").value;
            const timeSlotMinutesValue = document.getElementById("f6_timeInterval").value;

            // 基本的前端非空检查 (后端@NotNull会做最终校验)
            if (!startTimeValue || !endTimeValue || !topLeftLngValue || !topLeftLatValue ||
                !bottomRightLngValue || !bottomRightLatValue || !timeSlotMinutesValue) {
                alert("请填写完整的分析条件（F6）");
                return;
            }

            if (typeof clearF6Overlays === "function") clearF6Overlays(); // 清除之前的覆盖物

            // 将获取的值传递给分析函数
            performAreaCorrelationAnalysis2(
                startTimeValue,
                endTimeValue,
                topLeftLngValue,
                topLeftLatValue,
                bottomRightLngValue,
                bottomRightLatValue,
                timeSlotMinutesValue
            );
        });
    }

    // 清除 f6 模块创建的覆盖物
    window.clearF6Overlays = () => {
        if (window.f6Overlays && Array.isArray(window.f6Overlays)) {
            window.f6Overlays.forEach((overlay) => {
                if (map && map.removeOverlay && overlay) { // 确保 map 和 overlay 有效
                    map.removeOverlay(overlay);
                }
            });
            window.f6Overlays = [];
        }
    };

    function performAreaCorrelationAnalysis2(
        startTime,
        endTime,
        valTopLeftLng,
        valTopLeftLat,
        valBottomRightLng,
        valBottomRightLat,
        valTimeSlotMinutes
    ) {
        const resultDiv = document.getElementById("f6_result");
        if (!resultDiv) {
            console.error("未找到 f6_result 元素");
            return;
        }
        resultDiv.innerHTML = "<p>正在进行区域关联分析2...</p>";

        const params = {
            startTime: startTime,
            endTime: endTime,
            topLeftLongitude: Number.parseFloat(valTopLeftLng),
            topLeftLatitude: Number.parseFloat(valTopLeftLat),
            bottomRightLongitude: Number.parseFloat(valBottomRightLng),
            bottomRightLatitude: Number.parseFloat(valBottomRightLat),
            timeSlotMinutes: Number.parseInt(valTimeSlotMinutes),
        };
        // 调试日志
        console.log("F6 发送给后端的参数:", JSON.stringify(params, null, 2));

        const baseURL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
        const apiUrl = `${baseURL}/correlation/trafficFlowChangeWithOtherRegions`;
        const featureName = "F6单区域关联分析";

        fetch(apiUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(params),
        })
            .then(response => {
                console.log(`${featureName} 响应状态:`, response.status, "OK状态:", response.ok);
                if (!response.ok) {
                    return response.json()
                        .then(errorData => {
                            let errorMessage = `网络响应异常，状态码: ${response.status}`;
                            console.log(`${featureName} 成功解析后端错误JSON:`, errorData);
                            if (errorData && errorData.details && errorData.details.length > 0) {
                                const detailsText = errorData.details.map(detail => escapeHtml(detail)).join('; ');
                                errorMessage += `<br/>详情: ${detailsText}`;
                            } else if (errorData && errorData.message) {
                                errorMessage += `<br/>服务器消息: ${escapeHtml(errorData.message)}`;
                            } else {
                                errorMessage += `。无法获取详细错误信息（非JSON或无内容）。`;
                            }
                            throw new Error(errorMessage);
                        })
                        .catch(async (parsingErrorOrThrownError) => {
                            if (parsingErrorOrThrownError.message.includes("网络响应异常，状态码:")) {
                                throw parsingErrorOrThrownError;
                            }
                            console.warn(`${featureName} 解析JSON错误或处理错误数据时出错:`, parsingErrorOrThrownError);
                            let responseTextForDebug = "";
                            try {
                                const textResponse = response.clone();
                                responseTextForDebug = await textResponse.text();
                                console.warn(`${featureName} 后端返回的原始文本内容:`, responseTextForDebug);
                            } catch (e) { /* 忽略 */ }
                            let finalErrorMessage = `网络响应异常，状态码: ${response.status}`;
                            if (responseTextForDebug) {
                                finalErrorMessage += `<br/>服务器原始响应 (部分): ${escapeHtml(responseTextForDebug.substring(0,200))}`;
                            } else {
                                finalErrorMessage += `。无法获取服务器返回的详细错误内容。`;
                            }
                            throw new Error(finalErrorMessage);
                        });
                }
                return response.json();
            })
            .then((data) => {
                const resultDiv = document.getElementById("f6_result"); // 重新获取
                const trafficData = data.trafficFlowChange || data; // 兼容后端可能直接返回map的情况
                if (resultDiv) {
                    if (trafficData && Object.keys(trafficData).length > 0) {
                        timeData = trafficData;
                        timePoints = Object.keys(trafficData).sort();
                        currentTimeIndex = 0;
                        updateFlowDisplay();
                        resultDiv.innerHTML = `<p>区域关联分析结果：<br/>已获取到 ${timePoints.length} 个时间点的车流量数据。请使用上方的时间选择器浏览。</p>`;
                    } else {
                        resultDiv.innerHTML = "<p>未获取到有效的分析结果。</p>";
                        document.getElementById("f6_flow_display").style.display = "none";
                    }
                }
                // 绘制区域的逻辑应该在获取参数后，或者成功回调后
                // 但由于 drawAreaOnMap 使用的是原始的 topLeftLng 等值，可以放在前面
                // 但为了逻辑清晰，如果只在成功时绘制，可以移到这里
                if (typeof map !== "undefined" && map !== null) {
                    drawAreaOnMap(valTopLeftLng, valTopLeftLat, valBottomRightLng, valBottomRightLat, "blue"); // 使用传入的参数
                }
            })
            .catch((error) => {
                const resultDiv = document.getElementById("f6_result");
                if (resultDiv) {
                    resultDiv.innerHTML = `<p class="error-message">查询出错：<br/>${error.message}</p>`;
                }
                console.error(`${featureName} 查询出错详情 (Error Object):`, error);
            });
    }

    // drawAreaOnMap 函数与F5中的类似，但只绘制一个区域，且不需要label参数
    function drawAreaOnMap(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat, color) {
        try {
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
            if (map && map.addOverlay) map.addOverlay(rectangle);
            if (window.f6Overlays) window.f6Overlays.push(rectangle); // 添加到f6的覆盖物列表
        } catch (error) {
            console.error("F6 绘制区域时出错:", error);
        }
    }

    // 确保 map 和 BMapGL 已定义
    if (typeof map === "undefined") console.warn("map 未定义，请确保已加载百度地图 API");
    if (typeof BMapGL === "undefined") console.warn("BMapGL 未定义，请确保已加载百度地图 GL API");
});