/**
 * F4: 区域车流密度分析
 * 分析指定区域和时间段内的车流密度
 */

// 存储密度分析相关的覆盖物
var densityOverlays = [];

// 分析区域车流密度
function analyzeDensity() {
    var startTime = document.getElementById('f4_startTime').value;
    var endTime = document.getElementById('f4_endTime').value;
    var topLeftLng = parseFloat(document.getElementById('f4_topLeftLng').value);
    var topLeftLat = parseFloat(document.getElementById('f4_topLeftLat').value);
    var bottomRightLng = parseFloat(document.getElementById('f4_bottomRightLng').value);
    var bottomRightLat = parseFloat(document.getElementById('f4_bottomRightLat').value);
    var gridRadius = parseFloat(document.getElementById('gridRadius').value);
    var timeInterval = parseInt(document.getElementById('timeInterval').value);

    // 验证输入
    if (!startTime || !endTime || isNaN(topLeftLng) || isNaN(topLeftLat) ||
        isNaN(bottomRightLng) || isNaN(bottomRightLat) || isNaN(gridRadius) || isNaN(timeInterval)) {
        alert('请填写完整且有效的分析条件');
        return;
    }

    var resultDiv = document.getElementById('result');
    resultDiv.innerHTML = '<p>正在分析区域车流密度...</p>';

    // 清除之前的密度分析覆盖物
    clearDensityOverlays();

    // 绘制分析区域矩形
    drawAnalysisArea(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat);

    // 根据网格半径划分网格
    drawGrid(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat, gridRadius);

    // 更新结果信息
    resultDiv.innerHTML = `
        <p>已绘制分析区域和网格</p>
        <p>网格半径: ${gridRadius}km, 时间跨度: ${timeInterval}分钟</p>
        <p>时间范围: ${formatDateTime(startTime)} 至 ${formatDateTime(endTime)}</p>
    `;

    // 将地图中心设置到分析区域中心
    var centerLng = (topLeftLng + bottomRightLng) / 2;
    var centerLat = (topLeftLat + bottomRightLat) / 2;
    map.setCenter(new BMapGL.Point(centerLng, centerLat));

    // 调整地图缩放级别以适应分析区域
    fitMapToArea(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat);
}

// 格式化日期时间显示
function formatDateTime(dateTimeString) {
    if (!dateTimeString) return '';
    var date = new Date(dateTimeString);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// 清除密度分析相关的覆盖物
function clearDensityOverlays() {
    for (var i = 0; i < densityOverlays.length; i++) {
        map.removeOverlay(densityOverlays[i]);
    }
    densityOverlays = [];
}

// 绘制分析区域矩形
function drawAnalysisArea(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat) {
    var points = [
        new BMapGL.Point(topLeftLng, topLeftLat),
        new BMapGL.Point(bottomRightLng, topLeftLat),
        new BMapGL.Point(bottomRightLng, bottomRightLat),
        new BMapGL.Point(topLeftLng, bottomRightLat)
    ];

    var polygon = new BMapGL.Polygon(points, {
        strokeColor: "#1E90FF",
        strokeWeight: 2,
        strokeOpacity: 1,
        fillColor: "#1E90FF",
        fillOpacity: 0.1
    });

    map.addOverlay(polygon);
    densityOverlays.push(polygon);
}

// 根据网格半径划分网格
function drawGrid(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat, gridRadius) {
    // 将网格半径从千米转换为经纬度（近似值）
    // 1度纬度约等于111千米，1度经度在北京地区（约40度纬度）约等于85千米
    var latStep = gridRadius / 111.0;
    var lngStep = gridRadius / (111.0 * Math.cos(topLeftLat * Math.PI / 180));

    // 计算网格行列数
    var rows = Math.ceil((topLeftLat - bottomRightLat) / latStep);
    var cols = Math.ceil((bottomRightLng - topLeftLng) / lngStep);

    // 绘制水平网格线
    for (var i = 0; i <= rows; i++) {
        var lat = topLeftLat - i * latStep;
        var points = [
            new BMapGL.Point(topLeftLng, lat),
            new BMapGL.Point(bottomRightLng, lat)
        ];

        var polyline = new BMapGL.Polyline(points, {
            strokeColor: "#5CACEE",
            strokeWeight: 1,
            strokeOpacity: 0.7
        });

        map.addOverlay(polyline);
        densityOverlays.push(polyline);
    }

    // 绘制垂直网格线
    for (var j = 0; j <= cols; j++) {
        var lng = topLeftLng + j * lngStep;
        var points = [
            new BMapGL.Point(lng, topLeftLat),
            new BMapGL.Point(lng, bottomRightLat)
        ];

        var polyline = new BMapGL.Polyline(points, {
            strokeColor: "#5CACEE",
            strokeWeight: 1,
            strokeOpacity: 0.7
        });

        map.addOverlay(polyline);
        densityOverlays.push(polyline);
    }

    // 创建网格单元格（可选，用于后期热力图显示）
    for (var i = 0; i < rows; i++) {
        for (var j = 0; j < cols; j++) {
            var cellTopLeftLng = topLeftLng + j * lngStep;
            var cellTopLeftLat = topLeftLat - i * latStep;
            var cellBottomRightLng = Math.min(cellTopLeftLng + lngStep, bottomRightLng);
            var cellBottomRightLat = Math.max(cellTopLeftLat - latStep, bottomRightLat);

            var cellPoints = [
                new BMapGL.Point(cellTopLeftLng, cellTopLeftLat),
                new BMapGL.Point(cellBottomRightLng, cellTopLeftLat),
                new BMapGL.Point(cellBottomRightLng, cellBottomRightLat),
                new BMapGL.Point(cellTopLeftLng, cellBottomRightLat)
            ];

            var cellPolygon = new BMapGL.Polygon(cellPoints, {
                strokeColor: "#5CACEE",
                strokeWeight: 1,
                strokeOpacity: 0.5,
                fillColor: "#FFFFFF",
                fillOpacity: 0.1
            });

            // 存储网格单元格信息，用于后期热力图显示
            cellPolygon.gridInfo = {
                row: i,
                col: j,
                topLeftLng: cellTopLeftLng,
                topLeftLat: cellTopLeftLat,
                bottomRightLng: cellBottomRightLng,
                bottomRightLat: cellBottomRightLat
            };

            map.addOverlay(cellPolygon);
            densityOverlays.push(cellPolygon);
        }
    }
}

// 调整地图缩放级别以适应分析区域
function fitMapToArea(topLeftLng, topLeftLat, bottomRightLng, bottomRightLat) {
    var sw = new BMapGL.Point(topLeftLng, bottomRightLat);
    var ne = new BMapGL.Point(bottomRightLng, topLeftLat);
    var viewportPoints = [sw, ne];

    map.setViewport(viewportPoints, {
        margins: [50, 50, 50, 50]
    });
}

// 添加事件监听器
document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('densityAnalysisBtn').addEventListener('click', analyzeDensity);
});