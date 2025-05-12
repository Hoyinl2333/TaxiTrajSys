/**
 * F8: 频繁路径分析2
 * 分析两个矩形区域间的频繁路径，并在地图上显示
 */

// 全局变量存储路径数据
let f8PathFrequenciesData = []
let f8CurrentPathIndex = 0
let f8PathPolylines = [] // 存储路径折线对象
let f8AreaRectangles = [] // 存储区域矩形覆盖物

// 假设 BMapGL, map, convertCoordinates, addDotToMap, clearOverlays 这些变量已经在其他地方定义或引入
// 例如：
// import BMapGL from 'bmapgl'; // 如果是模块化的方式
// 或者在 HTML 中通过 <script> 标签引入百度地图 GL 的 JS 文件

document.addEventListener("DOMContentLoaded", () => {
  const frequentPath2Btn = document.getElementById("frequentPath2Btn")
  const pathSelector = document.getElementById("f8_path_selector")
  const pathSelectorContainer = document.getElementById("f8PathSelectorContainer")
  const pathDetails = document.getElementById("f8_path_details")

  // 初始隐藏路径选择器和详情
  if (pathSelectorContainer) pathSelectorContainer.style.display = "none"
  if (pathDetails) pathDetails.style.display = "none"

  if (frequentPath2Btn) {
    frequentPath2Btn.addEventListener("click", () => {
      const k = document.getElementById("f8_k").value
      const areaATopLeftLng = document.getElementById("f8_areaA_topLeftLng").value
      const areaATopLeftLat = document.getElementById("f8_areaA_topLeftLat").value
      const areaABottomRightLng = document.getElementById("f8_areaA_bottomRightLng").value
      const areaABottomRightLat = document.getElementById("f8_areaA_bottomRightLat").value
      const areaBTopLeftLng = document.getElementById("f8_areaB_topLeftLng").value
      const areaBTopLeftLat = document.getElementById("f8_areaB_topLeftLat").value
      const areaBBottomRightLng = document.getElementById("f8_areaB_bottomRightLng").value
      const areaBBottomRightLat = document.getElementById("f8_areaB_bottomRightLat").value

      // 验证输入
      if (
        !k ||
        !areaATopLeftLng ||
        !areaATopLeftLat ||
        !areaABottomRightLng ||
        !areaABottomRightLat ||
        !areaBTopLeftLng ||
        !areaBTopLeftLat ||
        !areaBBottomRightLng ||
        !areaBBottomRightLat
      ) {
        alert("请填写完整的分析条件")
        return
      }

      // 清除之前的覆盖物
      clearF8PathPolylines()
      clearF8AreaRectangles()

      // 绘制区域A和区域B的矩形
      drawAreaRectangles(
        areaATopLeftLng,
        areaATopLeftLat,
        areaABottomRightLng,
        areaABottomRightLat,
        areaBTopLeftLng,
        areaBTopLeftLat,
        areaBBottomRightLng,
        areaBBottomRightLat,
      )

      // 调用后端接口
      performFrequentPathAnalysis2(
        k,
        areaATopLeftLng,
        areaATopLeftLat,
        areaABottomRightLng,
        areaABottomRightLat,
        areaBTopLeftLng,
        areaBTopLeftLat,
        areaBBottomRightLng,
        areaBBottomRightLat,
      )
    })
  }

  // 添加路径选择器的变化事件
  if (pathSelector) {
    pathSelector.addEventListener("change", function () {
      const selectedIndex = Number.parseInt(this.value)
      f8CurrentPathIndex = selectedIndex
      displayF8PathOnMap(selectedIndex)
      updateF8PathDetails(selectedIndex)
    })
  }

  // 添加上一条/下一条路径按钮事件
  const prevPathBtn = document.getElementById("f8PrevPathBtn")
  const nextPathBtn = document.getElementById("f8NextPathBtn")

  if (prevPathBtn) {
    prevPathBtn.addEventListener("click", () => {
      if (f8PathFrequenciesData.length === 0) return

      f8CurrentPathIndex = (f8CurrentPathIndex - 1 + f8PathFrequenciesData.length) % f8PathFrequenciesData.length
      pathSelector.value = f8CurrentPathIndex
      displayF8PathOnMap(f8CurrentPathIndex)
      updateF8PathDetails(f8CurrentPathIndex)
    })
  }

  if (nextPathBtn) {
    nextPathBtn.addEventListener("click", () => {
      if (f8PathFrequenciesData.length === 0) return

      f8CurrentPathIndex = (f8CurrentPathIndex + 1) % f8PathFrequenciesData.length
      pathSelector.value = f8CurrentPathIndex
      displayF8PathOnMap(f8CurrentPathIndex)
      updateF8PathDetails(f8CurrentPathIndex)
    })
  }
})

// 绘制区域A和区域B的矩形
function drawAreaRectangles(
  areaATopLeftLng,
  areaATopLeftLat,
  areaABottomRightLng,
  areaABottomRightLat,
  areaBTopLeftLng,
  areaBTopLeftLat,
  areaBBottomRightLng,
  areaBBottomRightLat,
) {
  // 创建区域A的矩形
  const areaATopLeft = new BMapGL.Point(Number.parseFloat(areaATopLeftLng), Number.parseFloat(areaATopLeftLat))
  const areaABottomRight = new BMapGL.Point(
    Number.parseFloat(areaABottomRightLng),
    Number.parseFloat(areaABottomRightLat),
  )
  const areaAPolygon = createRectangle(areaATopLeft, areaABottomRight, "rgba(255, 0, 0, 0.3)", "区域A")

  // 创建区域B的矩形
  const areaBTopLeft = new BMapGL.Point(Number.parseFloat(areaBTopLeftLng), Number.parseFloat(areaBTopLeftLat))
  const areaBBottomRight = new BMapGL.Point(
    Number.parseFloat(areaBBottomRightLng),
    Number.parseFloat(areaBBottomRightLat),
  )
  const areaBPolygon = createRectangle(areaBTopLeft, areaBBottomRight, "rgba(0, 0, 255, 0.3)", "区域B")

  // 存储矩形覆盖物以便后续清除
  f8AreaRectangles.push(areaAPolygon, areaBPolygon)

  // 调整地图视野以包含两个区域
  const bounds = new BMapGL.Bounds(
    new BMapGL.Point(
      Math.min(Number.parseFloat(areaATopLeftLng), Number.parseFloat(areaBTopLeftLng)),
      Math.min(Number.parseFloat(areaABottomRightLat), Number.parseFloat(areaBBottomRightLat)),
    ),
    new BMapGL.Point(
      Math.max(Number.parseFloat(areaABottomRightLng), Number.parseFloat(areaBBottomRightLng)),
      Math.max(Number.parseFloat(areaATopLeftLat), Number.parseFloat(areaBTopLeftLat)),
    ),
  )
  map.setViewport(bounds)
}

// 创建矩形覆盖物
function createRectangle(topLeft, bottomRight, fillColor, label) {
  const points = [
    topLeft,
    new BMapGL.Point(bottomRight.lng, topLeft.lat),
    bottomRight,
    new BMapGL.Point(topLeft.lng, bottomRight.lat),
  ]

  const polygon = new BMapGL.Polygon(points, {
    strokeColor: "#000",
    strokeWeight: 2,
    strokeOpacity: 1,
    fillColor: fillColor,
    fillOpacity: 0.5,
  })

  map.addOverlay(polygon)

  // 添加标签
  const labelPoint = new BMapGL.Point((topLeft.lng + bottomRight.lng) / 2, (topLeft.lat + bottomRight.lat) / 2)

  const labelOpts = {
    position: labelPoint,
    offset: new BMapGL.Size(0, 0),
  }

  const labelMarker = new BMapGL.Label(label, labelOpts)
  labelMarker.setStyle({
    color: "#fff",
    backgroundColor: fillColor.replace("0.3", "0.8"),
    border: "none",
    fontSize: "14px",
    padding: "5px 10px",
    borderRadius: "3px",
  })

  map.addOverlay(labelMarker)

  return [polygon, labelMarker]
}

function performFrequentPathAnalysis2(
  k,
  areaATopLeftLng,
  areaATopLeftLat,
  areaABottomRightLng,
  areaABottomRightLat,
  areaBTopLeftLng,
  areaBTopLeftLat,
  areaBBottomRightLng,
  areaBBottomRightLat,
) {
  const resultDiv = document.getElementById("f8_result")
  const pathSelectorContainer = document.getElementById("f8PathSelectorContainer")
  const pathDetails = document.getElementById("f8_path_details")

  if (!resultDiv) {
    console.error("未找到 f8_result 元素")
    return
  }

  resultDiv.innerHTML = "<p>正在进行区域间频繁路径分析...</p>"

  // 隐藏路径选择器和详情
  if (pathSelectorContainer) pathSelectorContainer.style.display = "none"
  if (pathDetails) pathDetails.style.display = "none"

  // 清除之前的路径显示
  clearF8PathPolylines()

  // 构建请求参数
  const params = {
    k: Number.parseInt(k, 10),
    regionA: {
      minLat: Number.parseFloat(areaABottomRightLat),
      maxLat: Number.parseFloat(areaATopLeftLat),
      minLon: Number.parseFloat(areaATopLeftLng),
      maxLon: Number.parseFloat(areaABottomRightLng),
    },
    regionB: {
      minLat: Number.parseFloat(areaBBottomRightLat),
      maxLat: Number.parseFloat(areaBTopLeftLat),
      minLon: Number.parseFloat(areaBTopLeftLng),
      maxLon: Number.parseFloat(areaBBottomRightLng),
    },
  }

  console.log("发送的请求体:", params) // 调试信息，查看发送的请求体

  // 构建请求URL
  const apiUrl = `http://localhost:8080/paths/frequent/regional`

  // 发起POST请求
  fetch(apiUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(params),
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error(`网络响应异常，状态码: ${response.status}`)
      }
      return response.json()
    })
    .then((data) => {
      console.log("后端返回的数据:", data) // 调试信息，查看返回的数据
      processF8FrequentPathData(data)
    })
    .catch((error) => {
      resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`
      console.error("Error:", error)
    })
}

function processF8FrequentPathData(data) {
  const resultDiv = document.getElementById("f8_result")
  const pathSelector = document.getElementById("f8_path_selector")
  const pathSelectorContainer = document.getElementById("f8PathSelectorContainer")
  const pathDetails = document.getElementById("f8_path_details")

  if (data && data.pathFrequencies && data.pathFrequencies.length > 0) {
    // 保存数据到全局变量
    f8PathFrequenciesData = data.pathFrequencies
    f8CurrentPathIndex = 0

    // 更新结果显示
    resultDiv.innerHTML = `<p>区域间频繁路径分析结果：找到 ${data.pathFrequencies.length} 条频繁路径</p>`

    // 清空并填充路径选择器
    pathSelector.innerHTML = ""
    data.pathFrequencies.forEach((pathFrequency, index) => {
      const option = document.createElement("option")
      option.value = index
      option.textContent = `路径 ${index + 1} (频率: ${pathFrequency.frequency})`
      pathSelector.appendChild(option)
    })

    // 显示路径选择器和详情
    if (pathSelectorContainer) pathSelectorContainer.style.display = "flex"
    if (pathDetails) pathDetails.style.display = "block"

    // 显示第一条路径
    pathSelector.value = 0
    displayF8PathOnMap(0)
    updateF8PathDetails(0)
  } else {
    resultDiv.innerHTML = "<p>未获取到有效的分析结果。</p>"

    // 隐藏路径选择器和详情
    if (pathSelectorContainer) pathSelectorContainer.style.display = "none"
    if (pathDetails) pathDetails.style.display = "none"
  }
}

function displayF8PathOnMap(pathIndex) {
  if (!f8PathFrequenciesData || pathIndex >= f8PathFrequenciesData.length) {
    console.error("无效的路径索引或数据")
    return
  }

  // 清除之前的路径显示（但保留区域矩形）
  clearF8PathPolylines()

  // 获取选中的路径数据
  const pathData = f8PathFrequenciesData[pathIndex]
  const coordinates = pathData.pathCoordinates

  // 转换坐标
  const points = coordinates.map((coord) => new BMapGL.Point(coord.longitude, coord.latitude))

  // 使用坐标转换函数
  convertCoordinates(points, (data) => {
    if (data.status === 0) {
      // 创建折线
      const polyline = new BMapGL.Polyline(data.points, {
        strokeColor: getF8PathColor(pathIndex),
        strokeWeight: 5,
        strokeOpacity: 0.8,
      })

      // 添加到地图
      map.addOverlay(polyline)
      f8PathPolylines.push(polyline)

      // 添加起点和终点标记
      if (data.points.length > 0) {
        // 添加起点 - 绿色
        addDotToMap(data.points[0], "green")

        // 添加终点 - 红色
        addDotToMap(data.points[data.points.length - 1], "red")

        // 添加路径点 - 蓝色
        for (let i = 1; i < data.points.length - 1; i++) {
          addDotToMap(data.points[i], "blue")
        }
      }
    }
  })
}

function updateF8PathDetails(pathIndex) {
  const pathDetails = document.getElementById("f8_path_details")
  if (!pathDetails || !f8PathFrequenciesData || pathIndex >= f8PathFrequenciesData.length) return

  const pathData = f8PathFrequenciesData[pathIndex]

  let detailsHtml = `<h4>路径 ${pathIndex + 1} 详情 (频率: ${pathData.frequency})</h4>`
  detailsHtml += '<div class="path-details">'

  pathData.pathCoordinates.forEach((coord, idx) => {
    detailsHtml += `
            <div class="path-detail-item">
                <span class="path-detail-label">点 ${idx + 1}:</span>
                <span>经度: ${coord.longitude.toFixed(6)}, 纬度: ${coord.latitude.toFixed(6)}</span>
            </div>
        `
  })

  detailsHtml += "</div>"
  pathDetails.innerHTML = detailsHtml
}

function clearF8PathPolylines() {
  // 清除之前的路径折线
  f8PathPolylines.forEach((overlay) => {
    map.removeOverlay(overlay)
  })
  f8PathPolylines = []

  // 使用全局清除函数，但不清除区域矩形
  if (typeof clearOverlays === "function") {
    clearOverlays()
  }
}

function clearF8AreaRectangles() {
  f8AreaRectangles.forEach((rectangle) => {
    if (Array.isArray(rectangle)) {
      rectangle.forEach((overlay) => map.removeOverlay(overlay))
    } else {
      map.removeOverlay(rectangle)
    }
  })
  f8AreaRectangles = []
}

function getF8PathColor(index) {
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
    "#E91E63", // 粉色
  ]

  return colors[index % colors.length]
}
