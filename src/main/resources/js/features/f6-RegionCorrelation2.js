document.addEventListener('DOMContentLoaded', function() {
    const areaCorrelation2Btn = document.getElementById('areaCorrelation2Btn');

    if (areaCorrelation2Btn) {
        areaCorrelation2Btn.addEventListener('click', function() {
            const startTime = new Date(document.getElementById('f6_startTime').value).toISOString();
            const endTime = new Date(document.getElementById('f6_endTime').value).toISOString();
            const topLeftLng = document.getElementById('f6_topLeftLng').value;
            const topLeftLat = document.getElementById('f6_topLeftLat').value;
            const bottomRightLng = document.getElementById('f6_bottomRightLng').value;
            const bottomRightLat = document.getElementById('f6_bottomRightLat').value;

            if (!startTime || !endTime ||
                !topLeftLng || !topLeftLat || !bottomRightLng || !bottomRightLat) {
                alert('请填写完整的分析条件');
                return;
            }

            performAreaCorrelationAnalysis2(startTime, endTime, topLeftLng, topLeftLat, bottomRightLng, bottomRightLat);
        });
    }

    function performAreaCorrelationAnalysis2(startTime, endTime, topLeftLng, topLeftLat, bottomRightLng, bottomRightLat) {
        const resultDiv = document.getElementById('f6_result');
        if (!resultDiv) {
            console.error('未找到 f6_result 元素');
            return;
        }
        resultDiv.innerHTML = '<p>正在进行区域关联分析...</p>';

        const params = {
            startTime,
            endTime,
            topLeftLongitude: parseFloat(topLeftLng),
            topLeftLatitude: parseFloat(topLeftLat),
            bottomRightLongitude: parseFloat(bottomRightLng),
            bottomRightLatitude: parseFloat(bottomRightLat),
            timeSlotMinutes: 30 // 添加默认参数
        };

        const apiUrl = `http://localhost:8080/SingleCorrelation/trafficFlowChangeWithOtherRegions`;

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
            if (data && Object.keys(data).length > 0) {
                let resultHtml = '<p>区域关联分析结果：</p><ul>';
                for (const [time, flows] of Object.entries(data)) {
                    resultHtml += `<li>时间: ${time}, 车流量: ${flows}</li>`;
                }
                resultHtml += '</ul>';
                resultDiv.innerHTML = resultHtml;
            } else {
                resultDiv.innerHTML = '<p>未获取到有效的分析结果。</p>';
            }

            if (typeof map !== 'undefined' && map !== null) {
                drawAreaOnMap(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat, 'blue');
            }
        })
        .catch(error => {
            resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`;
            console.error('Error:', error);
        });
    }

    function drawAreaOnMap(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat, color) {
        try {
            const topLeftPoint = new BMapGL.Point(parseFloat(topLeftLng), parseFloat(topLeftLat));
            const bottomRightPoint = new BMapGL.Point(parseFloat(bottomRightLng), parseFloat(bottomRightLat));
            const rectangle = new BMapGL.Polygon([
                topLeftPoint,
                new BMapGL.Point(parseFloat(bottomRightLng), parseFloat(topLeftLat)),
                bottomRightPoint,
                new BMapGL.Point(parseFloat(topLeftLng), parseFloat(bottomRightLat))
            ], {
                strokeColor: color,
                strokeWeight: 2,
                strokeOpacity: 0.8,
                fillColor: color,
                fillOpacity: 0.2
            });
            map.addOverlay(rectangle);
        } catch (error) {
            console.error('绘制区域时出错:', error);
        }
    }
});