document.addEventListener('DOMContentLoaded', function() {
    const communicationTimeBtn = document.getElementById('communicationTimeBtn');

    if (communicationTimeBtn) {
        communicationTimeBtn.addEventListener('click', function() {
            // 使用 const 获取 DOM 元素的值
            const startTimeStr = document.getElementById('f9_startTime').value;
            const endTimeStr = document.getElementById('f9_endTime').value;
            const areaATopLeftLng = document.getElementById('f9_areaA_topLeftLng').value;
            const areaATopLeftLat = document.getElementById('f9_areaA_topLeftLat').value;
            const areaABottomRightLng = document.getElementById('f9_areaA_bottomRightLng').value;
            const areaABottomRightLat = document.getElementById('f9_areaA_bottomRightLat').value;
            const areaBTopLeftLng = document.getElementById('f9_areaB_topLeftLng').value;
            const areaBTopLeftLat = document.getElementById('f9_areaB_topLeftLat').value;
            const areaBBottomRightLng = document.getElementById('f9_areaB_bottomRightLng').value;
            const areaBBottomRightLat = document.getElementById('f9_areaB_bottomRightLat').value;

            // --- 客户端验证 ---
            if (!startTimeStr || !endTimeStr ||
                !areaATopLeftLng || !areaATopLeftLat || !areaABottomRightLng || !areaABottomRightLat ||
                !areaBTopLeftLng || !areaBTopLeftLat || !areaBBottomRightLng || !areaBBottomRightLat) {
                alert('请填写完整的分析条件');
                return;
            }

            const parsedStartTime = new Date(startTimeStr);
            const parsedEndTime = new Date(endTimeStr);

            if (isNaN(parsedStartTime.getTime()) || isNaN(parsedEndTime.getTime())) {
                alert('时间格式无效，请确保输入的时间是有效的日期和时间格式。\n例如: YYYY-MM-DDTHH:mm:ss 或 YYYY/MM/DD HH:mm:ss');
                return;
            }

            if (parsedStartTime >= parsedEndTime) {
                alert('开始时间必须早于结束时间。');
                return;
            }

            const coords = [
                areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat,
                areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat
            ];
            for (const coord of coords) {
                if (isNaN(parseFloat(coord))) {
                    alert(`坐标值 "${coord}" 无效，请输入数字。`);
                    return;
                }
            }

            // 调用分析函数
            performCommunicationTimeAnalysis(
                startTimeStr,
                endTimeStr,
                areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat,
                areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat
            );
        });
    }

    function performCommunicationTimeAnalysis(
        startTime, endTime,
        areaATopLeftLngStr, areaATopLeftLatStr, areaABottomRightLngStr, areaABottomRightLatStr,
        areaBTopLeftLngStr, areaBTopLeftLatStr, areaBBottomRightLngStr, areaBBottomRightLatStr
    ) {
        const resultDiv = document.getElementById('f9_result');
        if (!resultDiv) {
            console.error('未找到 f9_result 元素');
            return;
        }
        resultDiv.innerHTML = '<p>正在进行通信时间分析...</p>';

        // 解析经纬度字符串为浮点数
        const atlLng = parseFloat(areaATopLeftLngStr);
        const atlLat = parseFloat(areaATopLeftLatStr);
        const abrLng = parseFloat(areaABottomRightLngStr);
        const abrLat = parseFloat(areaABottomRightLatStr);

        const btlLng = parseFloat(areaBTopLeftLngStr);
        const btlLat = parseFloat(areaBTopLeftLatStr);
        const bbrLng = parseFloat(areaBBottomRightLngStr);
        const bbrLat = parseFloat(areaBBottomRightLatStr);

        const params = {
            regionA: {
                minLon: Math.min(atlLng, abrLng),
                minLat: Math.min(atlLat, abrLat),
                maxLon: Math.max(atlLng, abrLng),
                maxLat: Math.max(atlLat, abrLat)
            },
            regionB: {
                minLon: Math.min(btlLng, bbrLng),
                minLat: Math.min(btlLat, bbrLat),
                maxLon: Math.max(btlLng, bbrLng),
                maxLat: Math.max(btlLat, bbrLat)
            },
            startTime: startTime,
            endTime: endTime
        };

        const apiUrl = `http://localhost:8080/travelTime/analyze`;

        fetch(apiUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(params)
        })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(`网络响应异常，状态码: ${response.status}, 错误信息: ${text || response.statusText}`);
                    });
                }
                return response.json();
            })
            .then(data => {
                console.log('后端返回的数据:', data);
                if (data && data.found) {
                    let resultHtml = `<p>通信时间分析结果：</p><ul>`;

                    if (data.minTravelTimeFormatted) {
                        resultHtml += `<li>最短通行时间: ${data.minTravelTimeFormatted}</li>`;
                    } else {
                        // 作为备用，如果 formatted 字符串不存在但路径已找到
                        // (后端逻辑应确保 minTravelTimeFormatted 在找到路径且时间有效时存在，包括 "0秒")
                        resultHtml += `<li>最短通行时间: 未提供或计算错误</li>`;
                    }

                    if (data.shortestPath && data.shortestPath.length > 0) {
                        resultHtml += '<li>轨迹点：</li><ul>';
                        data.shortestPath.forEach(point => {
                            // 处理时间戳显示
                            const pointTimestamp = point.timestamp ? new Date(point.timestamp).toLocaleString() : 'N/A';
                            resultHtml += `<li>出租车ID: ${point.taxiId}, 时间: ${pointTimestamp}, 经度: ${point.longitude}, 纬度: ${point.latitude}</li>`;
                        });
                        resultHtml += '</ul>';
                    }
                    resultHtml += '</ul>';
                    resultDiv.innerHTML = resultHtml;
                } else if (data && data.message) {
                    resultDiv.innerHTML = `<p>分析提示：${data.message}</p>`;
                } else {
                    resultDiv.innerHTML = '<p>未获取到有效的分析结果，或未找到符合条件的路径。</p>';
                }
            })
            .catch(error => {
                resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`;
                console.error('Error during API call:', error);
            });
    }
});