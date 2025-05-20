// 全局变量
var map
var overlays = []
var beijingBoundaryOverlays = [] // 存储北京市边界覆盖物

// 自定义覆盖物类
function CustomOverlay(point, color = "blue") {
  this._point = point
  this._color = color
}

CustomOverlay.prototype = new BMapGL.Overlay()

CustomOverlay.prototype.initialize = function (map) {
  this._map = map
  var div = document.createElement("div")
  div.className = "dot"
  div.style.position = "absolute"
  div.style.backgroundColor = this._color
  map.getPanes().labelPane.appendChild(div)
  this._div = div
  return div
}

CustomOverlay.prototype.draw = function () {
  var position = this._map.pointToOverlayPixel(this._point)
  this._div.style.left = position.x - 4 + "px"
  this._div.style.top = position.y - 4 + "px"
}

CustomOverlay.prototype.setSize = function (size) {
  this._div.style.width = size + "px"
  this._div.style.height = size + "px"
  this.draw()
}

// 初始化地图
function initMap() {
  map = new BMapGL.Map("container", {
    enableAutoResize: false, // 禁止窗口resize触发重绘
    trackResize: false, // 禁止跟踪窗口大小变化
  })

  // 修改：设置北京市中心点坐标
  var beijingCenter = new BMapGL.Point(116.404, 39.915) // 北京市中心坐标

  // 修改：设置适合查看北京市的缩放级别（12比较适合查看整个北京市区）
  map.centerAndZoom(beijingCenter, 12)
  map.enableScrollWheelZoom(true)

  map.addControl(new BMapGL.ScaleControl())
  map.addControl(new BMapGL.ZoomControl())
  map.addControl(new BMapGL.NavigationControl())

  // 监听地图缩放结束事件，限制缩放级别
  map.addEventListener("zoomend", () => {
    var zoom = map.getZoom()
    if (zoom < 9) {
      map.setZoom(9)
    }
    if (typeof adjustDotsSize === "function") {
      adjustDotsSize(map.getZoom())
    }
  })

  // 添加北京市区边界
  addBeijingBoundary()
}

// 添加北京市边界
function addBeijingBoundary() {
  var bdary = new BMapGL.Boundary()
  bdary.get("北京市", (rs) => {
    // 获取行政区域
    var count = rs.boundaries.length
    if (count === 0) {
      return
    }

    for (var i = 0; i < count; i++) {
      var ply = new BMapGL.Polygon(rs.boundaries[i], {
        strokeWeight: 2,
        strokeColor: "#ff0000",
        fillOpacity: 0.05,
        fillColor: "#cccccc",
      })
      map.addOverlay(ply)
      beijingBoundaryOverlays.push(ply) // 将北京市边界覆盖物添加到专门的数组中
    }
  })
}

// 坐标转换
function convertCoordinates(points, callback) {
  if (!points || points.length === 0) {
    callback({ status: -1, points: [] })
    return
  }

  var convertor = new BMapGL.Convertor()
  var batchPoints = []
  var results = []

  for (let i = 0; i < points.length; i += 10) {
    batchPoints.push(points.slice(i, Math.min(i + 10, points.length)))
  }

  function processBatch(index) {
    if (index >= batchPoints.length) {
      callback({ status: 0, points: results })
      return
    }

    convertor.translate(batchPoints[index], 3, 5, (data) => {
      if (data.status === 0) {
        results = results.concat(data.points)
      } else {
        results = results.concat(batchPoints[index])
      }

      processBatch(index + 1)
    })
  }

  processBatch(0)
}

// 清除所有覆盖物（包括北京市边界）
function clearAllOverlays() {
  for (let i = 0; i < overlays.length; i++) {
    map.removeOverlay(overlays[i])
  }
  overlays = []

  for (let i = 0; i < beijingBoundaryOverlays.length; i++) {
    map.removeOverlay(beijingBoundaryOverlays[i])
  }
  beijingBoundaryOverlays = []
}

// 修改 clearOverlays 函数，使其也能清除 f5 和 f6 中的覆盖物
function clearOverlays() {
  try {
    console.log("开始清除覆盖物...")

    // 清除常规覆盖物
    for (let i = 0; i < overlays.length; i++) {
      if (map && map.removeOverlay && overlays[i]) {
        map.removeOverlay(overlays[i])
      }
    }
    overlays = []

    // 检查并调用 f4-density.js 中的清除方法
    if (window.mapVisualizer && typeof window.mapVisualizer.clearAllOverlays === "function") {
      window.mapVisualizer.clearAllOverlays()
    }

    // 清除 f7.js 中的路径折线
    if (window.pathPolylines && Array.isArray(window.pathPolylines)) {
      window.pathPolylines.forEach((overlay) => {
        if (map && map.removeOverlay && overlay) {
          map.removeOverlay(overlay)
        }
      })
      window.pathPolylines = []
    }

    // 清除 f8.js 中的路径折线
    if (window.f8PathPolylines && Array.isArray(window.f8PathPolylines)) {
      window.f8PathPolylines.forEach((overlay) => {
        if (map && map.removeOverlay && overlay) {
          map.removeOverlay(overlay)
        }
      })
      window.f8PathPolylines = []
    }

    // 清除 f9.js 中的覆盖物
    if (typeof window.clearF9Overlays === "function") {
      window.clearF9Overlays()
    }

    // 清除 f5.js 中的覆盖物
    if (typeof window.clearF5Overlays === "function") {
      window.clearF5Overlays()
    }

    // 清除 f6.js 中的覆盖物
    if (typeof window.clearF6Overlays === "function") {
      window.clearF6Overlays()
    }

    // 清除 f8.js 中的区域矩形
    if (window.f8AreaRectangles && Array.isArray(window.f8AreaRectangles)) {
      window.f8AreaRectangles.forEach((rectangle) => {
        if (Array.isArray(rectangle)) {
          rectangle.forEach((overlay) => {
            if (map && map.removeOverlay && overlay) {
              map.removeOverlay(overlay)
            }
          })
        } else if (map && map.removeOverlay && rectangle) {
          map.removeOverlay(rectangle)
        }
      })
      window.f8AreaRectangles = []
    }

    // 通用清除方法，用于清除所有 Polygon 类型覆盖物
    // 这是一个备用方法，以防特定的清除函数不可用
    if (map && map.getOverlays && typeof map.getOverlays === "function") {
      try {
        const allOverlays = map.getOverlays()
        if (allOverlays && Array.isArray(allOverlays)) {
          allOverlays.forEach((overlay) => {
            // 检查是否是 Polygon 类型（矩形是 Polygon 的一种）
            // 但不清除北京市边界（它们存储在 beijingBoundaryOverlays 中）
            if (
              overlay instanceof BMapGL.Polygon &&
              !beijingBoundaryOverlays.includes(overlay) &&
              !(
                window.f8AreaRectangles &&
                Array.isArray(window.f8AreaRectangles) &&
                window.f8AreaRectangles.some((rect) =>
                  Array.isArray(rect) ? rect.includes(overlay) : rect === overlay,
                )
              )
            ) {
              map.removeOverlay(overlay)
            }
            // 清除 Label 类型覆盖物（f5 和 f6 可能添加的标签）
            if (overlay instanceof BMapGL.Label) {
              map.removeOverlay(overlay)
            }
          })
        }
      } catch (error) {
        console.error("清除通用覆盖物时出错:", error)
      }
    }

    console.log("覆盖物清除完成")
  } catch (error) {
    console.error("清除覆盖物时出错:", error)
  }
}

// 添加点到地图
function addDotToMap(point, color = "red") {
  var dotOverlay = new CustomOverlay(point, color)
  map.addOverlay(dotOverlay)
  overlays.push(dotOverlay)
}
