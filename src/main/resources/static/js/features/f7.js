document.addEventListener('DOMContentLoaded', function() {
    const frequentPath1Btn = document.getElementById('frequentPath1Btn');

    if (frequentPath1Btn) {
        frequentPath1Btn.addEventListener('click', function() {
            const k = document.getElementById('f7_k').value;
            const minDistance = document.getElementById('f7_distance').value;

            // 验证输入
            if (!k || !minDistance) {
                alert('请填写完整的分析条件');
                return;
            }

            // 调用后端接口
            performFrequentPathAnalysis1(k, minDistance);
        });
    }

    function performFrequentPathAnalysis1(k, minDistance) {
        const resultDiv = document.getElementById('f7_result');
        if (!resultDiv) {
            console.error('未找到 f7_result 元素');
            return;
        }
        resultDiv.innerHTML = '<p>正在进行频繁路径分析...</p>';

        // 构建请求参数对象
        const params = {
            k: parseInt(k, 10), // 假设 k 需要是整数
            minPathDistanceKM: parseFloat(minDistance) // 假设 minDistance 需要是浮点数
        };

        console.log('发送的请求体:', params); // 调试信息，查看发送的请求体

        // 构建请求URL
        const baseURL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : '';
        const apiUrl = `${baseURL}/paths/frequent/citywide`; // POST 请求的 URL 通常不包含查询参数

        // 发起POST请求
        fetch(apiUrl, {
            method: 'POST', // 将请求方法改为 POST
            headers: {
                'Content-Type': 'application/json' // 设置请求头为 JSON
            },
            body: JSON.stringify(params) // 将参数对象转换为 JSON 字符串作为请求体
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`网络响应异常，状态码: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('后端返回的数据:', data); // 调试信息，查看返回的数据
            if (data && data.pathFrequencies) {
                let resultHtml = '<p>频繁路径分析结果：</p><ul>';
                data.pathFrequencies.forEach((pathFrequency, index) => {
                    resultHtml += `<li>路径 ${index + 1} (频率: ${pathFrequency.frequency})</li>`;
                    resultHtml += '<ul>';
                    pathFrequency.pathCoordinates.forEach(coordinate => {
                        resultHtml += `<li>经度: ${coordinate.longitude}, 纬度: ${coordinate.latitude}</li>`;
                    });
                    resultHtml += '</ul>';
                });
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