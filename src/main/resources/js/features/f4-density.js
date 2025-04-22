/**
 * F4: 区域车流密度分析
 * 分析指定区域和时间段内的车流密度
 */

// 存储密度分析相关的覆盖物
var densityOverlays = [];

// 存储网格单元格引用
var gridCells = [];

// 存储时间槽数据
var timeSlots = [];
var currentTimeIndex = 0;

// 存储密度分析结果
var densityResult = null;

// 分析区域车流密度
function analyzeDensity() {
    var startTime = document.getElementById('f4_startTime').value;
    var endTime = document.getElementById('f4_endTime').value;
    var minLongitude = document.getElementById('f4_topLeftLng').value;
    var maxLatitude = document.getElementById('f4_topLeftLat').value;
    var maxLongitude = document.getElementById('f4_bottomRightLng').value;
    var minLatitude = document.getElementById('f4_bottomRightLat').value;
    var gridSize = document.getElementById('gridRadius').value;
    var timeSlotMinutes = document.getElementById('timeInterval').value;

    // 验证输入
    if (!startTime || !endTime || !minLongitude || !minLatitude || !maxLongitude || !maxLatitude || !gridSize || !timeSlotMinutes) {
        alert('请填写完整的分析条件');
        return;
    }

    var resultDiv = document.getElementById('result');
    resultDiv.innerHTML = '<p>正在分析区域车流密度...</p>';

    // 清除之前的密度分析覆盖物
    clearDensityOverlays();

    // 重置全局变量
    gridCells = [];
    currentTimeIndex = 0;
    timeSlots = [];
    densityResult = null;

    // 构建请求参数
    var params = {
        gridSize: gridSize,
        startTime: startTime,
        endTime: endTime,
        timeSlotMinutes: timeSlotMinutes,
        minLongitude: minLongitude,
        minLatitude: minLatitude,
        maxLongitude: maxLongitude,
        maxLatitude: maxLatitude
    };

    // 构建查询字符串
    var queryString = Object.keys(params)
        .map(key => encodeURIComponent(key) + '=' + encodeURIComponent(params[key]))
        .join('&');

    // 后端 API 的 URL
    var apiUrl = `http://localhost:8080/densityAnalysis/densityAnalysis?${queryString}`;

    // 发起 API 请求
    fetch(apiUrl)
        .then(response => {
            if (!response.ok) {
                throw new Error('网络响应异常');
            }
            return response.json();
        })
        .then(data => {
            // 保存密度分析结果
            densityResult = data;

            // 提取时间槽
            timeSlots = Object.keys(data.densityMap).sort();

            // 绘制网格
            drawGridFromData(data);

            // 如果有时间槽数据，显示第一个时间槽的热力图
            if (timeSlots.length > 0) {
                updateHeatmap(0);
                updateTimeDisplay();
            }

            // 更新结果信息
            resultDiv.innerHTML = `<p>成功获取密度数据，共 ${timeSlots.length} 个时间点</p>`;

            // 将地图中心设置到分析区域中心
            var centerLng = (parseFloat(data.minLon) + parseFloat(data.maxLon)) / 2;
            var centerLat = (parseFloat(data.minLat) + parseFloat(data.maxLat)) / 2;
            map.setCenter(new BMapGL.Point(centerLng, centerLat));

            // 调整地图缩放级别以适应分析区域
            fitMapToArea(data.minLon, data.maxLat, data.maxLon, data.minLat);

            // 添加图例
            addLegend();
        })
        .catch(error => {
            resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`;
            console.error('Error:', error);
        });
}

// 更新时间显示
function updateTimeDisplay() {
    if (timeSlots.length > 0) {
        document.getElementById('currentTimeSlot').value = timeSlots[currentTimeIndex];
    } else {
        document.getElementById('currentTimeSlot').value = "无数据";
    }
}

// 清除密度分析相关的覆盖物
function clearDensityOverlays() {
    for (var i = 0; i < densityOverlays.length; i++) {
        map.removeOverlay(densityOverlays[i]);
    }
    densityOverlays = [];

    // 移除图例
    var legend = document.getElementById('density-legend');
    if (legend) {
        legend.parentNode.removeChild(legend);
    }
}

// 根据API返回的数据绘制网格
function drawGridFromData(data) {
    var minLon = parseFloat(data.minLon);
    var minLat = parseFloat(data.minLat);
    var maxLon = parseFloat(data.maxLon);
    var maxLat = parseFloat(data.maxLat);
    var rows = parseInt(data.rows);
    var cols = parseInt(data.cols);

    // 计算单元格大小
    var cellWidth = (maxLon - minLon) / cols;
    var cellHeight = (maxLat - minLat) / rows;

    // 绘制区域边框
    var borderPoints = [
        new BMapGL.Point(minLon, maxLat),
        new BMapGL.Point(maxLon, maxLat),
        new BMapGL.Point(maxLon, minLat),
        new BMapGL.Point(minLon, minLat)
    ];

    var borderPolygon = new BMapGL.Polygon(borderPoints, {
        strokeColor: "#1E90FF",
        strokeWeight: 2,
        strokeOpacity: 1,
        fillOpacity: 0
    });

    map.addOverlay(borderPolygon);
    densityOverlays.push(borderPolygon);

    // 创建网格单元格
    for (var i = 0; i < rows; i++) {
        for (var j = 0; j < cols; j++) {
            var cellMinLon = minLon + j * cellWidth;
            var cellMaxLat = maxLat - i * cellHeight;
            var cellMaxLon = cellMinLon + cellWidth;
            var cellMinLat = cellMaxLat - cellHeight;

            var cellPoints = [
                new BMapGL.Point(cellMinLon, cellMaxLat),
                new BMapGL.Point(cellMaxLon, cellMaxLat),
                new BMapGL.Point(cellMaxLon, cellMinLat),
                new BMapGL.Point(cellMinLon, cellMinLat)
            ];

            var cellPolygon = new BMapGL.Polygon(cellPoints, {
                strokeColor: "#CCCCCC",
                strokeWeight: 0.5,
                strokeOpacity: 0.5,
                fillColor: "#FFFFFF",
                fillOpacity: 0.1
            });

            // 存储网格单元格信息
            var cellId = `${i},${j}`;

            // 保存网格单元格引用，用于后续更新颜色
            gridCells.push({
                polygon: cellPolygon,
                id: cellId
            });

            map.addOverlay(cellPolygon);
            densityOverlays.push(cellPolygon);
        }
    }
}

// 更新热力图显示
function updateHeatmap(timeIndex) {
    if (!densityResult || !timeSlots || timeSlots.length === 0) return;

    currentTimeIndex = timeIndex;
    var timeSlot = timeSlots[timeIndex];
    var slotDensity = densityResult.densityMap[timeSlot] || {};

    // 找出最大密度值，用于归一化
    var maxDensity = 0;
    for (var cellId in slotDensity) {
        var density = slotDensity[cellId];
        if (density > maxDensity) {
            maxDensity = density;
        }
    }

    // 重置所有网格颜色
    for (var i = 0; i < gridCells.length; i++) {
        var cellId = gridCells[i].id;
        // 默认设置为浅蓝色（零密度）
        gridCells[i].polygon.setFillColor("#ADD8E6");
        gridCells[i].polygon.setFillOpacity(0.7);
    }

    // 更新有数据的网格颜色
    for (var cellId in slotDensity) {
        var density = slotDensity[cellId];

        // 找到对应的网格单元格
        var cell = gridCells.find(c => c.id === cellId);
        if (cell) {
            // 归一化密度值（0-1范围）
            var normalizedDensity = maxDensity > 0 ? density / maxDensity : 0;

            // 设置颜色 - 零密度保持浅蓝色，其他从蓝色到红色渐变
            var color = getColorForDensity(normalizedDensity, density);
            cell.polygon.setFillColor(color);
            cell.polygon.setFillOpacity(0.7);
        }
    }

    // 更新时间显示
    updateTimeDisplay();
}

// 根据密度值获取对应的颜色
function getColorForDensity(normalizedDensity, actualDensity) {
    // 如果实际密度为零，返回浅蓝色
    if (actualDensity === 0) {
        return "#ADD8E6";
    }

    // 从蓝色(0,0,255)到红色(255,0,0)的渐变
    var r = Math.round(normalizedDensity * 255);
    var g = 0;
    var b = Math.round((1 - normalizedDensity) * 255);

    return `rgb(${r}, ${g}, ${b})`;
}

// 添加图例
function addLegend() {
    var legend = document.createElement('div');
    legend.id = 'density-legend';
    legend.style.position = 'absolute';
    legend.style.bottom = '20px';
    legend.style.right = '20px';
    legend.style.backgroundColor = 'white';
    legend.style.padding = '10px';
    legend.style.border = '1px solid #ccc';
    legend.style.borderRadius = '5px';
    legend.style.boxShadow = '0 2px 6px rgba(0,0,0,0.3)';

    var title = document.createElement('div');
    title.textContent = '车流密度';
    title.style.fontWeight = 'bold';
    title.style.marginBottom = '5px';
    legend.appendChild(title);

    // 添加零密度的颜色示例
    var zeroDensity = document.createElement('div');
    zeroDensity.style.display = 'flex';
    zeroDensity.style.alignItems = 'center';
    zeroDensity.style.marginBottom = '5px';

    var zeroColor = document.createElement('div');
    zeroColor.style.height = '15px';
    zeroColor.style.width = '15px';
    zeroColor.style.backgroundColor = '#ADD8E6'; // 浅蓝色
    zeroColor.style.marginRight = '5px';
    zeroColor.style.border = '1px solid #ccc';

    var zeroLabel = document.createElement('span');
    zeroLabel.textContent = '零密度';

    zeroDensity.appendChild(zeroColor);
    zeroDensity.appendChild(zeroLabel);
    legend.appendChild(zeroDensity);

    // 添加渐变颜色条
    var colorBarTitle = document.createElement('div');
    colorBarTitle.textContent = '密度渐变:';
    colorBarTitle.style.marginBottom = '3px';
    legend.appendChild(colorBarTitle);

    var colorBar = document.createElement('div');
    colorBar.style.height = '20px';
    colorBar.style.width = '150px';
    colorBar.style.background = 'linear-gradient(to right, blue, red)';
    legend.appendChild(colorBar);

    var labels = document.createElement('div');
    labels.style.display = 'flex';
    labels.style.justifyContent = 'space-between';
    labels.style.width = '150px';

    var lowLabel = document.createElement('span');
    lowLabel.textContent = '低';

    var highLabel = document.createElement('span');
    highLabel.textContent = '高';

    labels.appendChild(lowLabel);
    labels.appendChild(highLabel);
    legend.appendChild(labels);

    document.getElementById('container').appendChild(legend);
}

// 调整地图缩放级别以适应分析区域
function fitMapToArea(minLon, maxLat, maxLon, minLat) {
    var sw = new BMapGL.Point(minLon, minLat);
    var ne = new BMapGL.Point(maxLon, maxLat);
    var viewportPoints = [sw, ne];

    map.setViewport(viewportPoints, {
        margins: [50, 50, 50, 50]
    });
}

// 添加事件监听器
document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('densityAnalysisBtn').addEventListener('click', analyzeDensity);

    // 时间切换按钮事件
    document.getElementById('prevTimeSlot').addEventListener('click', function() {
        if (timeSlots.length > 0 && currentTimeIndex > 0) {
            updateHeatmap(currentTimeIndex - 1);
        }
    });

    document.getElementById('nextTimeSlot').addEventListener('click', function() {
        if (timeSlots.length > 0 && currentTimeIndex < timeSlots.length - 1) {
            updateHeatmap(currentTimeIndex + 1);
        }
    });
});