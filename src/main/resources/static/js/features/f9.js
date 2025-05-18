document.addEventListener('DOMContentLoaded', function() {
    const communicationTimeBtn = document.getElementById('communicationTimeBtn');

    if (communicationTimeBtn) {
        communicationTimeBtn.addEventListener('click', function() {
            const startTime = document.getElementById('f9_startTime').value;
            const endTime = document.getElementById('f9_endTime').value;
            const areaATopLeftLng = document.getElementById('f9_areaA_topLeftLng').value;
            const areaATopLeftLat = document.getElementById('f9_areaA_topLeftLat').value;
            const areaABottomRightLng = document.getElementById('f9_areaA_bottomRightLng').value;
            const areaABottomRightLat = document.getElementById('f9_areaA_bottomRightLat').value;
            const areaBTopLeftLng = document.getElementById('f9_areaB_topLeftLng').value;
            const areaBTopLeftLat = document.getElementById('f9_areaB_topLeftLat').value;
            const areaBBottomRightLng = document.getElementById('f9_areaB_bottomRightLng').value;
            const areaBBottomRightLat = document.getElementById('f9_areaB_bottomRightLat').value;

            if (!startTime || !endTime ||
                !areaATopLeftLng || !areaATopLeftLat || !areaABottomRightLng || !areaABottomRightLat ||
                !areaBTopLeftLng || !areaBTopLeftLat || !areaBBottomRightLng || !areaBBottomRightLat) {
                alert('请填写完整的分析条件');
                return;
            }

            // 验证时间格式
            const startDate = new Date(startTime);
            const endDate = new Date(endTime);

            if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
                alert('时间格式无效，请确保输入的时间是有效的日期和时间格式。');
                return;
            }

            performCommunicationTimeAnalysis(
                startDate.toISOString(),
                endDate.toISOString(),
                areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat,
                areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat
            );
        });
    }

    function performCommunicationTimeAnalysis(startTime, endTime, areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat, areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat) {
        const resultDiv = document.getElementById('f9_result');
        if (!resultDiv) {
            console.error('未找到 f9_result 元素');
            return;
        }
        resultDiv.innerHTML = '<p>正在进行通信时间分析...</p>';

        const params = {
            regionA: {
                minLon: parseFloat(areaATopLeftLng),
                minLat: parseFloat(areaATopLeftLat),
                maxLon: parseFloat(areaABottomRightLng),
                maxLat: parseFloat(areaABottomRightLat)
            },
            regionB: {
                minLon: parseFloat(areaBTopLeftLng),
                minLat: parseFloat(areaBTopLeftLat),
                maxLon: parseFloat(areaBBottomRightLng),
                maxLat: parseFloat(areaBBottomRightLat)
            },
            startTime,
            endTime
        };
        const baseURL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : '';
        const apiUrl = `${baseURL}/travelTime/analyze`;

        fetch(apiUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(params)
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`网络响应异常，状态码: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('后端返回的数据:', data); // 调试信息，查看返回的数据
            if (data && data.found) {
                let resultHtml = '<p>通信时间分析结果：</p><ul>';
                resultHtml += `<li>最短通行时间: ${data.minTravelTime} 分钟</li>`;
                if (data.shortestPath && data.shortestPath.length > 0) {
                    resultHtml += '<li>轨迹点：</li><ul>';
                    data.shortestPath.forEach(point => {
                        resultHtml += `<li>出租车ID: ${point.taxiId}, 时间: ${point.timestamp}, 经度: ${point.longitude}, 纬度: ${point.latitude}</li>`;
                    });
                    resultHtml += '</ul>';
                }
                resultHtml += '</ul>';
                resultDiv.innerHTML = resultHtml;
            } else {
                resultDiv.innerHTML = '<p>未获取到有效的分析结果。</p>';
            }
        })
        .catch(error => {
            resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`;
            console.error('Error:', error);
        });
    }
});