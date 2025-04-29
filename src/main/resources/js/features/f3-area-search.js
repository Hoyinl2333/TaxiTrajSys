
/**
 * F3: 区域范围查找
 * 根据时间段和地理范围查找出租车数量，并在地图上绘制查询区域
 */
function searchTaxisInArea() {
    // 1. 获取并验证输入
    var startTime = document.getElementById('f3_startTime').value;
    var endTime = document.getElementById('f3_endTime').value;
    // 转换为数字
    var topLeftLng = parseFloat(document.getElementById('f3_topLeftLng').value);
    var topLeftLat = parseFloat(document.getElementById('f3_topLeftLat').value);
    var bottomRightLng = parseFloat(document.getElementById('f3_bottomRightLng').value);
    var bottomRightLat = parseFloat(document.getElementById('f3_bottomRightLat').value);

    if (!startTime || !endTime || isNaN(topLeftLng) || isNaN(topLeftLat) || isNaN(bottomRightLng) || isNaN(bottomRightLat)) {
        alert('请填写完整的或有效的查询条件');
        return;
    }

    var resultDiv = document.getElementById('result');
    resultDiv.innerHTML = '<p>正在查询区域内的出租车数量...</p>';

    // 2. 清除地图上之前的覆盖物 (矩形)
    if (typeof map === 'undefined' || map === null) {
        console.error("Baidu Map instance is not available.");
        // 如果地图不可用，可能无法继续绘制矩形
        // return; // 可以选择返回
    } else {
        clearOverlays(); // 使用 map-utils.js 的函数
    }


    // 3. 绘制查询区域的矩形
    var polygon = null;
    var polygonPoints = []; // 用于调整视野
    if (typeof map !== 'undefined' && map !== null) {
        // 确保使用正确的 min/max 经纬度来定义矩形的四个角点
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
            strokeColor: "blue", strokeWeight: 2, strokeOpacity: 0.8,
            fillColor: "blue", fillOpacity: 0.2
        });

        map.addOverlay(polygon);
        overlays.push(polygon); // 添加到全局覆盖物列表，供 clearOverlays 清除
    }


    // 4. 构建请求参数和URL
    var params = {
        lon1: topLeftLng,
        lat1: topLeftLat,
        lon2: bottomRightLng,
        lat2: bottomRightLat,
        startTime: startTime,
        endTime: endTime
    };
    var queryString = Object.keys(params)
        .map(key => encodeURIComponent(key) + '=' + encodeURIComponent(params[key]))
        .join('&');
    var apiUrl = `http://localhost:8080/region/count?${queryString}`; // 后端 API 仍然需要调用以获取数量

    // 5. 发起 API 请求获取数量
    fetch(apiUrl)
        .then(response => {
            if (!response.ok) {
                // 如果响应非2xx，抛出错误，进入 catch 块
                throw new Error('网络响应异常: ' + response.status);
            }
            return response.json(); // 仍然需要解析 JSON 以获取 taxiCount
        })
        .then(data => {
            // 6. 显示出租车数量
            if (data && typeof data.taxiCount !== 'undefined') {
                resultDiv.innerHTML = `<p>在指定时间段和区域内的出租车数量为: ${data.taxiCount} 辆</p>`;

                // 7. 调整地图视野以显示绘制的矩形
                if (typeof map !== 'undefined' && map !== null && polygonPoints.length > 0) {
                    map.setViewport(polygonPoints);
                }

            } else {
                // API 调用成功，但返回的数据结构异常（没有 taxiCount）
                resultDiv.innerHTML = `<p>查询成功，但返回数据结构异常。</p>`;
                console.error("Backend response structure unexpected:", data);
                // 即使数据异常，也尝试调整视野到绘制的矩形
                if (typeof map !== 'undefined' && map !== null && polygonPoints.length > 0) {
                    map.setViewport(polygonPoints);
                }
            }
        })
        .catch(error => {
            // 8. 处理 API 调用错误 (网络问题等)
            resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`;
            console.error("Error during fetch or processing:", error);
            // 清理工作由 clearOverlays 在下次查询或函数开始时完成
        });
}

// 添加事件监听器
document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('areaSearchBtn').addEventListener('click', searchTaxisInArea);

    // 可选：为 F3 输入框设置一些默认的经纬度值和时间范围方便测试 (不变)
    document.getElementById('f3_topLeftLng').value = '116.34';
    document.getElementById('f3_topLeftLat').value = '40.00';
    document.getElementById('f3_bottomRightLng').value = '116.48';
    document.getElementById('f3_bottomRightLat').value = '39.85';
    document.getElementById('f3_startTime').value = '2008-02-02T00:00';
    document.getElementById('f3_endTime').value = '2008-02-03T00:00';
});