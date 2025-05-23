// 在 f3-area-search.js 文件中

/**
 * F3: 区域范围查找
 * 根据时间段和地理范围查找出租车数量，并在地图上绘制查询区域
 */
function searchTaxisInArea() {
    // 1. 获取表单输入值
    var startTimeValue = document.getElementById("f3_startTime").value;
    var endTimeValue = document.getElementById("f3_endTime").value;
    var topLeftLng = Number.parseFloat(document.getElementById("f3_topLeftLng").value);
    var topLeftLat = Number.parseFloat(document.getElementById("f3_topLeftLat").value);
    var bottomRightLng = Number.parseFloat(document.getElementById("f3_bottomRightLng").value);
    var bottomRightLat = Number.parseFloat(document.getElementById("f3_bottomRightLat").value);

    if (
        !startTimeValue ||
        !endTimeValue ||
        isNaN(topLeftLng) ||
        isNaN(topLeftLat) ||
        isNaN(bottomRightLng) ||
        isNaN(bottomRightLat)
    ) {
        alert("请填写完整的或有效的查询条件");
        return;
    }

    var resultDiv = document.getElementById("f3_result");
    resultDiv.innerHTML = "<p>正在查询区域内的出租车数量...</p>";

     clearOverlays();

    var polygon = null;
    var polygonPoints = [];
    if (typeof map !== "undefined" && map !== null) {
        var minLng = Math.min(topLeftLng, bottomRightLng);
        var minLat = Math.min(topLeftLat, bottomRightLat);
        var maxLng = Math.max(topLeftLng, bottomRightLng);
        var maxLat = Math.max(topLeftLat, bottomRightLat);

        polygonPoints = [
            new BMapGL.Point(minLng, minLat),
            new BMapGL.Point(maxLng, minLat),
            new BMapGL.Point(maxLng, maxLat),
            new BMapGL.Point(minLng, maxLat)
        ];
        polygon = new BMapGL.Polygon(polygonPoints, {
            strokeColor: "blue",
            strokeWeight: 2,
            strokeOpacity: 0.8,
            fillColor: "blue",
            fillOpacity: 0.2
        });
        map.addOverlay(polygon);
        overlays.push(polygon);
        if (polygonPoints.length > 0) {
            map.setViewport(polygonPoints);
        }
    }

    const requestBody = {
        minLongitude: topLeftLng,
        minLatitude: bottomRightLat,
        maxLongitude: bottomRightLng,
        maxLatitude: topLeftLat,
        startTime: startTimeValue,
        endTime: endTimeValue
    };
    // 调试日志
    console.log("F3 发送给后端的参数:", JSON.stringify(requestBody, null, 2));

    const baseURL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
    var apiUrl = `${baseURL}/region/count`;
    const featureName = "F3区域范围查找"; // 用于日志和错误消息

    fetch(apiUrl, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
    })
        .then(response => {
            console.log(`${featureName} 响应状态:`, response.status, "OK状态:", response.ok);
            if (!response.ok) {
                return response.json() // 尝试解析JSON错误体
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
                        // 如果 errorData.details 成功构造了 errorMessage 并被 throw，这里会捕获它
                        // 如果 response.json() 解析失败，parsingErrorOrThrownError 是解析错误
                        if (parsingErrorOrThrownError.message.includes("网络响应异常，状态码:")) {
                            throw parsingErrorOrThrownError; // 直接重新抛出已构造的详细错误
                        }
                        // 进入这里意味着 response.json() 解析失败
                        console.warn(`${featureName} 解析JSON错误或处理错误数据时出错:`, parsingErrorOrThrownError);
                        let responseTextForDebug = "";
                        try {
                            const textResponse = response.clone();
                            responseTextForDebug = await textResponse.text();
                            console.warn(`${featureName} 后端返回的原始文本内容:`, responseTextForDebug);
                        } catch (e) { /* 忽略读取文本的错误 */ }

                        let finalErrorMessage = `网络响应异常，状态码: ${response.status}`;
                        if(responseTextForDebug){
                            finalErrorMessage += `<br/>服务器原始响应 (部分): ${escapeHtml(responseTextForDebug.substring(0,200))}`;
                        } else {
                            finalErrorMessage += `。无法获取详细错误信息。`;
                        }
                        throw new Error(finalErrorMessage);
                    });
            }
            return response.json(); // 成功时
        })
        .then(data => {
            const resultDiv = document.getElementById("f3_result");
            if (resultDiv) {
                if (data && typeof data.taxiCount !== "undefined") {
                    resultDiv.innerHTML = `<p>在指定时间段和区域内的出租车数量为: ${data.taxiCount} 辆</p>`;
                } else {
                    resultDiv.innerHTML = `<p>查询成功，但返回数据结构异常。</p>`;
                    console.error(`${featureName} 后端响应结构意外:`, data);
                }
            }
        })
        .catch(error => {
            const resultDiv = document.getElementById("f3_result");
            if (resultDiv) {
                resultDiv.innerHTML = `<p class="error-message">查询出错：<br/>${error.message}</p>`;
            }
            console.error(`${featureName} 查询出错详情 (Error Object):`, error);
        });
}



document.addEventListener("DOMContentLoaded", () => {
    const areaSearchBtn = document.getElementById("areaSearchBtn");
    if (areaSearchBtn) {
        areaSearchBtn.addEventListener("click", searchTaxisInArea);
    }
    // 为输入框设置默认值
    const f3_topLeftLng = document.getElementById("f3_topLeftLng");
    if(f3_topLeftLng) f3_topLeftLng.value = "116.34";
    const f3_topLeftLat = document.getElementById("f3_topLeftLat");
    if(f3_topLeftLat) f3_topLeftLat.value = "40.00";
    const f3_bottomRightLng = document.getElementById("f3_bottomRightLng");
    if(f3_bottomRightLng) f3_bottomRightLng.value = "116.48";
    const f3_bottomRightLat = document.getElementById("f3_bottomRightLat");
    if(f3_bottomRightLat) f3_bottomRightLat.value = "39.85";
    const f3_startTime = document.getElementById("f3_startTime");
    if(f3_startTime) f3_startTime.value = "2008-02-02T00:00";
    const f3_endTime = document.getElementById("f3_endTime");
    if(f3_endTime) f3_endTime.value = "2008-02-03T00:00";
});

