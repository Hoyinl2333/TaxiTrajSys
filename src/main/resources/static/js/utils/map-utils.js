// 文件: static/js/utils/map-utils.js

// --- 全局变量 ---
var map; // 地图实例
var beijingBoundaryOverlays = []; // 存储北京市边界覆盖物, 这个通常是静态的，不参与常规清除

// window.allFeatureOverlays 将由各个 featureX.js 文件在加载时向其添加自己的键和空数组
if (typeof window.allFeatureOverlays === 'undefined') {
  window.allFeatureOverlays = {}; // 初始化，如果尚未定义
  console.log("MAP-UTILS: window.allFeatureOverlays 初始化完毕。");
} else {
  console.log("MAP-UTILS: window.allFeatureOverlays 已存在。");
}

// 全局的 'overlays' 数组不再由 map-utils 直接主要管理，而是由各模块通过 allFeatureOverlays 间接管理
// 为了兼容旧的直接使用 overlays 的代码（如果有），可以保留它，但理想情况下它应该为空
var overlays = [];


// --- 自定义覆盖物类 (CustomOverlay) ---
function CustomOverlay(point, color = "blue") {
  this._point = point;
  this._color = color;
}
CustomOverlay.prototype = new BMapGL.Overlay();
CustomOverlay.prototype.initialize = function (map) {
  this._map = map;
  var div = document.createElement("div");
  div.className = "dot";
  div.style.position = "absolute";
  div.style.backgroundColor = this._color;
  map.getPanes().labelPane.appendChild(div);
  this._div = div;
  return div;
};
CustomOverlay.prototype.draw = function () {
  var position = this._map.pointToOverlayPixel(this._point);
  this._div.style.left = position.x - 4 + "px";
  this._div.style.top = position.y - 4 + "px";
};
CustomOverlay.prototype.setSize = function (size) {
  this._div.style.width = size + "px";
  this._div.style.height = size + "px";
  this.draw();
};

// --- 初始化地图 ---
function initMap() {
  map = new BMapGL.Map("container", {
    enableAutoResize: false,
    trackResize: false,
  });
  var beijingCenter = new BMapGL.Point(116.404, 39.915);
  map.centerAndZoom(beijingCenter, 12);
  map.enableScrollWheelZoom(true);
  map.addControl(new BMapGL.ScaleControl());
  map.addControl(new BMapGL.ZoomControl());
  map.addControl(new BMapGL.NavigationControl());
  map.addEventListener("zoomend", () => {
    var zoom = map.getZoom();
    if (zoom < 9) {
      map.setZoom(9);
    }
    if (typeof adjustDotsSize === "function") {
      adjustDotsSize(map.getZoom());
    }
  });
  addBeijingBoundary();
}

// --- 添加北京市边界 ---
function addBeijingBoundary() {
  var bdary = new BMapGL.Boundary();
  bdary.get("北京市", (rs) => {
    var count = rs.boundaries.length;
    if (count === 0) {
      console.warn("MAP-UTILS: 未能获取到北京市的边界数据。");
      return;
    }
    // 清除旧的北京边界 (如果需要重新绘制的话)
    beijingBoundaryOverlays.forEach(overlay => map.removeOverlay(overlay));
    beijingBoundaryOverlays = [];

    for (var i = 0; i < count; i++) {
      var ply = new BMapGL.Polygon(rs.boundaries[i], {
        strokeWeight: 2,
        strokeColor: "#ff0000",
        fillOpacity: 0.05,
        fillColor: "#cccccc",
      });
      map.addOverlay(ply);
      beijingBoundaryOverlays.push(ply);
    }
  });
}

// --- 坐标转换 ---
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
    let fromCoordType = 3; // 假设 GCJ02
    convertor.translate(batchPoints[index], fromCoordType, 5, (data) => {
      if (data.status === 0) {
        results = results.concat(data.points);
      } else {
        console.warn("MAP-UTILS: 一批坐标转换失败，状态码:", data.status);
        results = results.concat(batchPoints[index]); // 转换失败则使用原始点
      }
      processBatch(index + 1);
    });
  }
  processBatch(0);
}

// 该函数创建并返回一个点覆盖物，但不将其添加到任何全局数组。
// 调用者（例如 F1 模块）负责管理返回的覆盖物。
function addDotToMap(point, color = "red") {
  if (!map) {
    console.error("MAP-UTILS: 地图实例 (map) 未初始化，无法添加点。");
    return null;
  }
  if (!point || typeof point.lng === 'undefined' || typeof point.lat === 'undefined') {
    console.error("MAP-UTILS: 无效的点对象 (point) 提供给 addDotToMap。", point);
    return null;
  }
  var dotOverlay = new CustomOverlay(point, color);
  try {
    map.addOverlay(dotOverlay);
  } catch (e) {
    console.error("MAP-UTILS: addOverlay 时发生错误: ", e, dotOverlay);
    return null;
  }
  return dotOverlay; // 返回创建的覆盖物对象
}


// 这个函数负责清除所有通过 window.allFeatureOverlays 管理的覆盖物。
function clearOverlays() {
  try {
    console.log("MAP-UTILS: clearOverlays() 被调用。开始清除所有已注册功能模块的覆盖物...");

    if (!map || typeof map.removeOverlay !== 'function') {
      console.error("MAP-UTILS: 地图实例 (map) 无效或 removeOverlay 方法不存在，无法清除覆盖物。");
      return;
    }

    if (typeof window.allFeatureOverlays === 'object' && window.allFeatureOverlays !== null) {
      for (const featureKey in window.allFeatureOverlays) {
        if (window.allFeatureOverlays.hasOwnProperty(featureKey)) {
          const featureSpecificOverlays = window.allFeatureOverlays[featureKey];

          if (Array.isArray(featureSpecificOverlays)) { // 确保是数组
            // console.log(`MAP-UTILS: 正在清除模块 "${featureKey}" 的 ${featureSpecificOverlays.length} 个覆盖物...`);
            for (let i = 0; i < featureSpecificOverlays.length; i++) {
              if (featureSpecificOverlays[i]) { // 确保覆盖物对象存在
                try {
                  map.removeOverlay(featureSpecificOverlays[i]);
                } catch (e) {
                  console.warn(`MAP-UTILS: 从模块 "${featureKey}" 移除覆盖物时出错:`, e, featureSpecificOverlays[i]);
                }
              }
            }
            window.allFeatureOverlays[featureKey] = []; // 清空该模块的覆盖物数组
          } else if (featureSpecificOverlays && typeof map.removeOverlay === 'function') {
            // 处理非数组类型的单个覆盖物（例如图例）
            // console.log(`MAP-UTILS: 正在清除模块 "${featureKey}" 的单个覆盖物...`);
            try {
              // 特殊处理F4的图例，因为它可能直接是DOM元素
              if (featureKey === "F4_legend" && featureSpecificOverlays.parentNode) {
                featureSpecificOverlays.parentNode.removeChild(featureSpecificOverlays);
              } else {
                map.removeOverlay(featureSpecificOverlays);
              }
            } catch (e) {
              console.warn(`MAP-UTILS: 从模块 "${featureKey}" 移除单个覆盖物时出错:`, e, featureSpecificOverlays);
            }
            window.allFeatureOverlays[featureKey] = null; // 或 undefined，表示已清除
          }
        }
      }
      console.log("MAP-UTILS: 所有已注册功能模块的覆盖物均已尝试清除。");
    } else {
      console.warn("MAP-UTILS: window.allFeatureOverlays 未定义或不是对象，无法进行模块化清除。");
    }

    // 为了安全起见，也清除一下旧的全局 overlays 数组（如果还有代码在用它）
    // 理想情况下，这个数组应该在所有模块迁移完毕后不再被使用。
    if (Array.isArray(window.overlays) && window.overlays.length > 0) {
      console.warn("MAP-UTILS: 旧的全局 'overlays' 数组中仍有覆盖物，正在清除。请确保所有模块已迁移到 allFeatureOverlays 管理。");
      for (let i = 0; i < window.overlays.length; i++) {
        if (window.overlays[i]) {
          try {
            map.removeOverlay(window.overlays[i]);
          } catch (e) {
            console.warn("MAP-UTILS: 从旧的全局 'overlays' 数组移除覆盖物时出错:", e, window.overlays[i]);
          }
        }
      }
      window.overlays = [];
    }

  } catch (error) {
    console.error("MAP-UTILS: clearOverlays 执行过程中发生错误:", error);
  }
}