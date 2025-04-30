document.addEventListener('DOMContentLoaded', function() {
    const areaCorrelation1Btn = document.getElementById('areaCorrelation1Btn');

    if (areaCorrelation1Btn) {
        areaCorrelation1Btn.addEventListener('click', function() {
            const startTime = document.getElementById('f5_startTime').value;
            const endTime = document.getElementById('f5_endTime').value;
            const area1TopLeftLng = document.getElementById('f5_area1_topLeftLng').value;
            const area1TopLeftLat = document.getElementById('f5_area1_topLeftLat').value;
            const area1BottomRightLng = document.getElementById('f5_area1_bottomRightLng').value;
            const area1BottomRightLat = document.getElementById('f5_area1_bottomRightLat').value;
            const area2TopLeftLng = document.getElementById('f5_area2_topLeftLng').value;
            const area2TopLeftLat = document.getElementById('f5_area2_topLeftLat').value;
            const area2BottomRightLng = document.getElementById('f5_area2_bottomRightLng').value;
            const area2BottomRightLat = document.getElementById('f5_area2_bottomRightLat').value;

            if (!startTime || !endTime ||
                !area1TopLeftLng || !area1TopLeftLat || !area1BottomRightLng || !area1BottomRightLat ||
                !area2TopLeftLng || !area2TopLeftLat || !area2BottomRightLng || !area2BottomRightLat) {
                alert('请填写完整的分析条件');
                return;
            }

            performAreaCorrelationAnalysis1(
                startTime, endTime,
                area1TopLeftLng, area1TopLeftLat, area1BottomRightLng, area1BottomRightLat,
                area2TopLeftLng, area2TopLeftLat, area2BottomRightLng, area2BottomRightLat
            );
        });
    }

    function performAreaCorrelationAnalysis1(startTime, endTime, a1TLLng, a1TLLat, a1BRLng, a1BRLat, a2TLLng, a2TLLat, a2BRLng, a2BRLat) {
        const resultDiv = document.getElementById('f5_result');
        if (!resultDiv) {
            console.error('未找到 f5_result 元素');
            return;
        }
        resultDiv.innerHTML = '<p>正在进行区域关联分析...</p>';

        // 构建请求参数
        const params = {
            startTime,
            endTime,
            topLeftLongitude1: parseFloat(a1TLLng),
            topLeftLatitude1: parseFloat(a1TLLat),
            bottomRightLongitude1: parseFloat(a1BRLng),
            bottomRightLatitude1: parseFloat(a1BRLat),
            topLeftLongitude2: parseFloat(a2TLLng),
            topLeftLatitude2: parseFloat(a2TLLat),
            bottomRightLongitude2: parseFloat(a2BRLng),
            bottomRightLatitude2: parseFloat(a2BRLat),
        };

        // 后端 API 的 URL
        const apiUrl = `http://localhost:8080/api/trafficFlowChangeBetweenRegions`;

        // 发起 POST 请求
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
                    resultHtml += `<li>时间: ${time}, 从区域1到区域2的车流量: ${flows[0]}, 从区域2到区域1的车流量: ${flows[1]}</li>`;
                }
                resultHtml += '</ul>';
                resultDiv.innerHTML = resultHtml;
            } else {
                resultDiv.innerHTML = '<p>未获取到有效的分析结果。</p>';
            }

            // 在地图上绘制两个区域
            if (typeof map !== 'undefined' && map !== null) {
                drawAreaOnMap(a1TLLng, a1TLLat, a1BRLng, a1BRLat, 'blue');
                drawAreaOnMap(a2TLLng, a2TLLat, a2BRLng, a2BRLat, 'red');
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