/**
 * F7: 频繁路径分析1
 * 分析全市范围内的频繁路径，并在地图上显示
 */

// 全局变量存储路径数据
let pathFrequenciesData = [];
let currentPathIndex = 0;
let pathPolylines = []; // 存储路径折线对象

document.addEventListener("DOMContentLoaded", () => {
    const frequentPath1Btn = document.getElementById("frequentPath1Btn");
    const pathSelector = document.getElementById("path_selector");
    const pathSelectorContainer = document.getElementById("pathSelectorContainer");
    const pathDetails = document.getElementById("path_details");
    
    // 初始隐藏路径选择器和详情
    if (pathSelectorContainer) pathSelectorContainer.style.display = "none";
    if (pathDetails) pathDetails.style.display = "none";

    if (frequentPath1Btn) {
        frequentPath1Btn.addEventListener("click", () => {
            const k = document.getElementById("f7_k").value;
            const minDistance = document.getElementById("f7_distance").value;

            // 验证输入
            if (!k || !minDistance) {
                alert("请填写完整的分析条件");
                return;
            }

            // 调用后端接口
            performFrequentPathAnalysis1(k, minDistance);
        });
    }

    // 添加路径选择器的变化事件
    if (pathSelector) {
        pathSelector.addEventListener("change", function() {
            const selectedIndex = Number.parseInt(this.value);
            currentPathIndex = selectedIndex;
            displayPathOnMap(selectedIndex);
            updatePathDetails(selectedIndex);
        });
    }

    // 添加上一条/下一条路径按钮事件
    const prevPathBtn = document.getElementById("prevPathBtn");
    const nextPathBtn = document.getElementById("nextPathBtn");
    
    if (prevPathBtn) {
        prevPathBtn.addEventListener("click", () => {
            if (pathFrequenciesData.length === 0) return;

            currentPathIndex = (currentPathIndex - 1 + pathFrequenciesData.length) % pathFrequenciesData.length;
            pathSelector.value = currentPathIndex;
            displayPathOnMap(currentPathIndex);
            updatePathDetails(currentPathIndex);
        });
    }
    
    if (nextPathBtn) {
        nextPathBtn.addEventListener("click", () => {
            if (pathFrequenciesData.length === 0) return;

            currentPathIndex = (currentPathIndex + 1) % pathFrequenciesData.length;
            pathSelector.value = currentPathIndex;
            displayPathOnMap(currentPathIndex);
            updatePathDetails(currentPathIndex);
        });
    }
});

function performFrequentPathAnalysis1(k, minDistance) {
    const resultDiv = document.getElementById("f7_result");
    const pathSelectorContainer = document.getElementById("pathSelectorContainer");
    const pathDetails = document.getElementById("path_details");
    
    if (!resultDiv) {
        console.error("未找到 f7_result 元素");
        return;
    }
    
    resultDiv.innerHTML = "<p>正在进行频繁路径分析...</p>";
    
    // 隐藏路径选择器和详情
    if (pathSelectorContainer) pathSelectorContainer.style.display = "none";
    if (pathDetails) pathDetails.style.display = "none";

    // 清除之前的路径显示
    clearPathPolylines();

    const params = {
            k: parseInt(k, 10), // 假设 k 需要是整数
            minPathDistanceKM: parseFloat(minDistance) // 假设 minDistance 需要是浮点数
    };

    console.log('发送的请求体:', params); // 调试信息，查看发送的请求体
    // 构建请求URL

    const baseURL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : '';
    const apiUrl = `${baseURL}/paths/frequent/citywide`;


    // 发起GET请求
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
        processFrequentPathData(data);
    })
    .catch(error => {
        resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`;
        console.error('Error:', error);
    });
}

function processFrequentPathData(data) {
    const resultDiv = document.getElementById("f7_result");
    const pathSelector = document.getElementById("path_selector");
    const pathSelectorContainer = document.getElementById("pathSelectorContainer");
    const pathDetails = document.getElementById("path_details");

    if (data && data.pathFrequencies && data.pathFrequencies.length > 0) {
        // 保存数据到全局变量
        pathFrequenciesData = data.pathFrequencies;
        currentPathIndex = 0;

        // 更新结果显示
        resultDiv.innerHTML = `<p>频繁路径分析结果：找到 ${data.pathFrequencies.length} 条频繁路径</p>`;
        
        // 清空并填充路径选择器
        pathSelector.innerHTML = '';
        data.pathFrequencies.forEach((pathFrequency, index) => {
            const option = document.createElement('option');
            option.value = index;
            option.textContent = `路径 ${index + 1} (频率: ${pathFrequency.frequency})`;
            pathSelector.appendChild(option);
        });
        
        // 显示路径选择器和详情
        if (pathSelectorContainer) pathSelectorContainer.style.display = "flex";
        if (pathDetails) pathDetails.style.display = "block";

        // 显示第一条路径
        pathSelector.value = 0;
        displayPathOnMap(0);
        updatePathDetails(0);
    } else {
        resultDiv.innerHTML = "<p>未获取到有效的分析结果。</p>";
        
        // 隐藏路径选择器和详情
        if (pathSelectorContainer) pathSelectorContainer.style.display = "none";
        if (pathDetails) pathDetails.style.display = "none";
    }
}

function displayPathOnMap(pathIndex) {
    if (!pathFrequenciesData || pathIndex >= pathFrequenciesData.length) {
        console.error("无效的路径索引或数据");
        return;
    }

    // 清除之前的路径显示
    clearPathPolylines();

    // 获取选中的路径数据
    const pathData = pathFrequenciesData[pathIndex];
    const coordinates = pathData.pathCoordinates;

    // 转换坐标
    const points = coordinates.map(coord => new BMapGL.Point(coord.longitude, coord.latitude));

    // 使用坐标转换函数
    convertCoordinates(points, (data) => {
        if (data.status === 0) {
            // 创建折线
            const polyline = new BMapGL.Polyline(data.points, {
                strokeColor: getPathColor(pathIndex),
                strokeWeight: 5,
                strokeOpacity: 0.8
            });

            // 添加到地图
            map.addOverlay(polyline);
            pathPolylines.push(polyline);

            // 添加起点和终点标记
            if (data.points.length > 0) {
                // 添加起点 - 绿色
                addDotToMap(data.points[0], "green");
                
                // 添加终点 - 红色
                addDotToMap(data.points[data.points.length - 1], "red");
                
                // 添加路径点 - 蓝色
                for (let i = 1; i < data.points.length - 1; i++) {
                    addDotToMap(data.points[i], "blue");
                }
                
                // 设置地图中心点
                map.setCenter(data.points[0]);
                map.setZoom(15);
            }
        }
    });
}

function updatePathDetails(pathIndex) {
    const pathDetails = document.getElementById("path_details");
    if (!pathDetails || !pathFrequenciesData || pathIndex >= pathFrequenciesData.length) return;
    
    const pathData = pathFrequenciesData[pathIndex];
    
    let detailsHtml = `<h4>路径 ${pathIndex + 1} 详情 (频率: ${pathData.frequency})</h4>`;
    detailsHtml += '<div class="path-details">';
    
    pathData.pathCoordinates.forEach((coord, idx) => {
        detailsHtml += `
            <div class="path-detail-item">
                <span class="path-detail-label">点 ${idx + 1}:</span>
                <span>经度: ${coord.longitude.toFixed(6)}, 纬度: ${coord.latitude.toFixed(6)}</span>
            </div>
        `;
    });
    
    detailsHtml += '</div>';
    pathDetails.innerHTML = detailsHtml;
}

function clearPathPolylines() {
    // 清除之前的路径折线
    pathPolylines.forEach(overlay => {
        map.removeOverlay(overlay);
    });
    pathPolylines = [];
    
    // 使用全局清除函数
    clearOverlays();
}

function getPathColor(index) {
    // 为不同的路径提供不同的颜色
    const colors = [
        "#FF5252", // 红色
        "#4CAF50", // 绿色
        "#2196F3", // 蓝色
        "#FFC107", // 黄色
        "#9C27B0", // 紫色
        "#00BCD4", // 青色
        "#FF9800", // 橙色
        "#795548", // 棕色
        "#607D8B", // 蓝灰色
        "#E91E63"  // 粉色
    ];

    return colors[index % colors.length];
}