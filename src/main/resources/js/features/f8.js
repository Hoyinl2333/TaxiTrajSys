document.addEventListener('DOMContentLoaded', function() {
    const frequentPath2Btn = document.getElementById('frequentPath2Btn');

    if (frequentPath2Btn) {
        frequentPath2Btn.addEventListener('click', function() {
            const k = document.getElementById('f8_k').value;
            const areaATopLeftLng = document.getElementById('f8_areaA_topLeftLng').value;
            const areaATopLeftLat = document.getElementById('f8_areaA_topLeftLat').value;
            const areaABottomRightLng = document.getElementById('f8_areaA_bottomRightLng').value;
            const areaABottomRightLat = document.getElementById('f8_areaA_bottomRightLat').value;
            const areaBTopLeftLng = document.getElementById('f8_areaB_topLeftLng').value;
            const areaBTopLeftLat = document.getElementById('f8_areaB_topLeftLat').value;
            const areaBBottomRightLng = document.getElementById('f8_areaB_bottomRightLng').value;
            const areaBBottomRightLat = document.getElementById('f8_areaB_bottomRightLat').value;

            // 验证输入
            if (!k ||
                !areaATopLeftLng || !areaATopLeftLat || !areaABottomRightLng || !areaABottomRightLat ||
                !areaBTopLeftLng || !areaBTopLeftLat || !areaBBottomRightLng || !areaBBottomRightLat) {
                alert('请填写完整的分析条件');
                return;
            }

            // 调用后端接口
            performFrequentPathAnalysis2(k, areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat, areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat);
        });
    }

    function performFrequentPathAnalysis2(k, areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat, areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat) {
        const resultDiv = document.getElementById('f8_result');
        if (!resultDiv) {
            console.error('未找到 f8_result 元素');
            return;
        }
        resultDiv.innerHTML = '<p>正在进行区域间频繁路径分析...</p>';

        // 构建请求参数
        const params = {
            k: parseInt(k, 10),
            regionA: {
                minLat: parseFloat(areaATopLeftLat),
                maxLat: parseFloat(areaABottomRightLat),
                minLon: parseFloat(areaATopLeftLng),
                maxLon: parseFloat(areaABottomRightLng)
            },
            regionB: {
                minLat: parseFloat(areaBTopLeftLat),
                maxLat: parseFloat(areaBBottomRightLat),
                minLon: parseFloat(areaBTopLeftLng),
                maxLon: parseFloat(areaBBottomRightLng)
            }
        };

        console.log('发送的请求体:', params); // 调试信息，查看发送的请求体

        // 构建请求URL
        const apiUrl = `http://localhost:8080/paths/frequent/regional`;

        // 发起POST请求
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
            if (data && data.pathFrequencies) {
                let resultHtml = '<p>区域间频繁路径分析结果：</p><ul>';
                data.pathFrequencies.forEach((pathFrequency, index) => {
                    resultHtml += `<li>路径 ${index + 1} (频率: ${pathFrequency.frequency})</li>`;
                    resultHtml += '<ul>';
                    pathFrequency.path.cellIdSequence.forEach(cellId => {
                        resultHtml += `<li>${cellId}</li>`;
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