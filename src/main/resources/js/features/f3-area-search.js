/**
 * F3: 区域范围查找
 * 根据时间段和地理范围查找出租车
 */
function searchTaxisInArea() {
    var startTime = document.getElementById('f3_startTime').value;
    var endTime = document.getElementById('f3_endTime').value;
    var topLeftLng = document.getElementById('f3_topLeftLng').value;
    var topLeftLat = document.getElementById('f3_topLeftLat').value;
    var bottomRightLng = document.getElementById('f3_bottomRightLng').value;
    var bottomRightLat = document.getElementById('f3_bottomRightLat').value;

    // 验证输入
    if (!startTime || !endTime || !topLeftLng || !topLeftLat || !bottomRightLng || !bottomRightLat) {
        alert('请填写完整的查询条件');
        return;
    }

    var resultDiv = document.getElementById('result');
    resultDiv.innerHTML = '<p>正在查询区域内的出租车...</p>';

    // 构建请求参数
    var params = {
        lon1: topLeftLng,
        lat1: topLeftLat,
        lon2: bottomRightLng,
        lat2: bottomRightLat,
        startTime: startTime,
        endTime: endTime
    };

    // 构建查询字符串
    var queryString = Object.keys(params)
       .map(key => encodeURIComponent(key) + '=' + encodeURIComponent(params[key]))
       .join('&');

    // 后端 API 的 URL
    var apiUrl = `http://localhost:8080/region/count?${queryString}`;

    // 发起 API 请求
    fetch(apiUrl)
      .then(response => {
            if (!response.ok) {
                throw new Error('网络响应异常');
            }
            return response.json();
        })
      .then(data => {
            resultDiv.innerHTML = `<p>在指定时间段和区域内的出租车数量为: ${data} 辆</p>`;
        })
      .catch(error => {
            resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`;
        });
}

// 添加事件监听器
document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('areaSearchBtn').addEventListener('click', searchTaxisInArea);
});