document.addEventListener("DOMContentLoaded", () => {
    let timeData = {} // 存储时间点和对应的车流量数据
    let timePoints = [] // 存储所有时间点
    let currentTimeIndex = 0 // 当前时间点索引

    // 获取时间选择器元素
    const prevTimeBtn = document.getElementById("f6_prevTime")
    const nextTimeBtn = document.getElementById("f6_nextTime")
    const currentTimeDisplay = document.getElementById("f6_currentTime")

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
        document.getElementById("flow_enter").textContent = flows[0]
        document.getElementById("flow_leave").textContent = flows[1]

        // 显示车流量区域
        document.getElementById("f6_flow_display").style.display = "block"
    }

    const areaCorrelation2Btn = document.getElementById("areaCorrelation2Btn")

    if (areaCorrelation2Btn) {
        areaCorrelation2Btn.addEventListener("click", () => {
            const startTime = new Date(document.getElementById("f6_startTime").value).toISOString()
            const endTime = new Date(document.getElementById("f6_endTime").value).toISOString()
            const topLeftLng = document.getElementById("f6_topLeftLng").value
            const topLeftLat = document.getElementById("f6_topLeftLat").value
            const bottomRightLng = document.getElementById("f6_bottomRightLng").value
            const bottomRightLat = document.getElementById("f6_bottomRightLat").value
            const timeSlotMinutes = document.getElementById("f6_timeInterval").value

            if (!startTime || !endTime || !topLeftLng || !topLeftLat || !bottomRightLng || !bottomRightLat) {
                alert("请填写完整的分析条件")
                return
            }

            performAreaCorrelationAnalysis2(startTime, endTime, topLeftLng, topLeftLat, bottomRightLng, bottomRightLat)
        })
    }

    function performAreaCorrelationAnalysis2(startTime, endTime, topLeftLng, topLeftLat, bottomRightLng, bottomRightLat) {
        const resultDiv = document.getElementById("f6_result")
        if (!resultDiv) {
            console.error("未找到 f6_result 元素")
            return
        }
        resultDiv.innerHTML = "<p>正在进行区域关联分析...</p>"

        const params = {
            startTime,
            endTime,
            topLeftLongitude: Number.parseFloat(topLeftLng),
            topLeftLatitude: Number.parseFloat(topLeftLat),
            bottomRightLongitude: Number.parseFloat(bottomRightLng),
            bottomRightLatitude: Number.parseFloat(bottomRightLat),
            timeSlotMinutes: Number.parseInt(document.getElementById("f6_timeInterval").value),
        }
        const baseURL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : '';
        const apiUrl = `${baseURL}/SingleCorrelation/trafficFlowChangeWithOtherRegions`

        fetch(apiUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(params),
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`网络响应异常，状态码: ${response.status}`)
                }
                return response.json()
            })
            .then((data) => {
                if (data && Object.keys(data).length > 0) {
                    // 保存数据到全局变量
                    timeData = data
                    timePoints = Object.keys(data).sort()
                    currentTimeIndex = 0

                    // 显示第一个时间点的数据
                    updateFlowDisplay()

                    let resultHtml = "<p>区域关联分析结果：</p>"
                    resultHtml +=
                        "<p>已获取到 " +
                        timePoints.length +
                        " 个时间点的车流量数据。请使用上方的时间选择器浏览不同时间点的数据。</p>"
                    resultDiv.innerHTML = resultHtml
                } else {
                    resultDiv.innerHTML = "<p>未获取到有效的分析结果。</p>"
                    document.getElementById("f6_flow_display").style.display = "none"
                }

                if (typeof map !== "undefined" && map !== null) {
                    drawAreaOnMap(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat, "blue")
                }
            })
            .catch((error) => {
                resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`
                console.error("Error:", error)
            })
    }

    function drawAreaOnMap(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat, color) {
        try {
            const topLeftPoint = new BMapGL.Point(Number.parseFloat(topLeftLng), Number.parseFloat(topLeftLat))
            const bottomRightPoint = new BMapGL.Point(Number.parseFloat(bottomRightLng), Number.parseFloat(bottomRightLat))
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
        } catch (error) {
            console.error("绘制区域时出错:", error)
        }
    }
})
