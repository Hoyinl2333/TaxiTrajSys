// 全局变量
var map;
var overlays = [];

// 自定义覆盖物类
function CustomOverlay(point, color = 'blue'){
    this._point = point;
    this._color = color;
}

CustomOverlay.prototype = new BMapGL.Overlay();

CustomOverlay.prototype.initialize = function(map){
    this._map = map;
    var div = document.createElement("div");
    div.className = "dot";
    div.style.position = "absolute";
    div.style.backgroundColor = this._color;
    map.getPanes().labelPane.appendChild(div);
    this._div = div;
    return div;
};

CustomOverlay.prototype.draw = function(){
    var position = this._map.pointToOverlayPixel(this._point);
    this._div.style.left = position.x - 4 + "px";
    this._div.style.top = position.y - 4 + "px";
};

CustomOverlay.prototype.setSize = function(size){
    this._div.style.width = size + "px";
    this._div.style.height = size + "px";
    this.draw();
};

// 初始化地图
function initMap() {
    map = new BMapGL.Map("container", {enableAutoResize: false,  // 禁止窗口resize触发重绘
        trackResize: false        // 禁止跟踪窗口大小变化
    });

    // 修改：设置北京市中心点坐标
    var beijingCenter = new BMapGL.Point(116.404, 39.915); // 北京市中心坐标

    // 修改：设置适合查看北京市的缩放级别（12比较适合查看整个北京市区）
    map.centerAndZoom(beijingCenter, 12);
    map.enableScrollWheelZoom(true);

    map.addControl(new BMapGL.ScaleControl());
    map.addControl(new BMapGL.ZoomControl());
    map.addControl(new BMapGL.NavigationControl());


    // 监听地图缩放结束事件，限制缩放级别
    map.addEventListener("zoomend", function() {
        var zoom = map.getZoom();
        if (zoom < 9) {
            map.setZoom(9);
        }
        adjustDotsSize(map.getZoom());
    });

    // 添加北京市区边界
    addBeijingBoundary();
}

// 添加北京市边界
function addBeijingBoundary() {
    var bdary = new BMapGL.Boundary();
    bdary.get("北京市", function(rs) {
        // 获取行政区域
        var count = rs.boundaries.length;
        if (count === 0) {
            return;
        }

        for (var i = 0; i < count; i++) {
            var ply = new BMapGL.Polygon(rs.boundaries[i], {
                strokeWeight: 2,
                strokeColor: "#ff0000",
                fillOpacity: 0.05,
                fillColor: "#cccccc"
            });
            map.addOverlay(ply);
        }
    });
}

// 坐标转换
function convertCoordinates(points, callback) {
    if (!points || points.length === 0) {
        callback({ status: -1, points: [] });
        return;
    }

    var convertor = new BMapGL.Convertor();
    var batchPoints = [];
    var results = [];

    for (let i = 0; i < points.length; i += 10) {
        batchPoints.push(points.slice(i, Math.min(i + 10, points.length)));
    }

    function processBatch(index) {
        if (index >= batchPoints.length) {
            callback({ status: 0, points: results });
            return;
        }

        convertor.translate(batchPoints[index], 3, 5, function(data) {
            if (data.status === 0) {
                results = results.concat(data.points);
            } else {
                results = results.concat(batchPoints[index]);
            }

            processBatch(index + 1);
        });
    }

    processBatch(0);
}

// 清除覆盖物
function clearOverlays() {
    for (var i = 0; i < overlays.length; i++) {
        map.removeOverlay(overlays[i]);
    }
    overlays = [];
}

// 添加点到地图
function addDotToMap(point, color = 'red') {
    var dotOverlay = new CustomOverlay(point, color);
    map.addOverlay(dotOverlay);
    overlays.push(dotOverlay);
}