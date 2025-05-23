/**
 * F9: 通信时间分析
 */

// 全局变量，F9功能专属
window.f9AreaOverlays = window.f9AreaOverlays || [];       // 存储区域A、B的矩形和标签
window.f9PathRelatedOverlays = window.f9PathRelatedOverlays || []; // 存储路径线、起点和终点标记

/**
 * 绘制F9功能的区域A和区域B的矩形及标签。
 * 覆盖物会被添加到 window.f9AreaOverlays 数组。
 */
function drawF9AreaRectangles(
    areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat,
    areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat
) {

  const createRectAndLabel = (topLeftLngStr, topLeftLatStr, bottomRightLngStr, bottomRightLatStr, color, labelText) => {
    try {
      const topLeftLng = Number.parseFloat(topLeftLngStr);
      const topLeftLat = Number.parseFloat(topLeftLatStr);
      const bottomRightLng = Number.parseFloat(bottomRightLngStr);
      const bottomRightLat = Number.parseFloat(bottomRightLatStr);

      const tlPoint = new BMapGL.Point(topLeftLng, topLeftLat);
      const brPoint = new BMapGL.Point(bottomRightLng, bottomRightLat);

      const rectPoints = [
        tlPoint, new BMapGL.Point(brPoint.lng, tlPoint.lat),
        brPoint, new BMapGL.Point(tlPoint.lng, brPoint.lat)
      ];
      const polygon = new BMapGL.Polygon(rectPoints, {
        strokeColor: color, strokeWeight: 2, strokeOpacity: 1,
        fillColor: color, fillOpacity: 0.3,
      });
      if (map) map.addOverlay(polygon);
      window.f9AreaOverlays.push(polygon);

      const label = new BMapGL.Label(labelText, {
        position: new BMapGL.Point((topLeftLng + bottomRightLng) / 2, (topLeftLat + bottomRightLat) / 2),
        offset: new BMapGL.Size(-20, -10)
      });
      label.setStyle({
        color: "#fff", backgroundColor: color === "#FF0000" ? "rgba(255,0,0,0.8)" : "rgba(0,0,255,0.8)",
        border: "none", fontSize: "14px", padding: "5px 10px", borderRadius: "3px",
      });
      if (map) map.addOverlay(label);
      window.f9AreaOverlays.push(label);
      return rectPoints;
    } catch (e) {
      console.error(`F9: 绘制区域 ${labelText} 出错:`, e);
      return [];
    }
  };

  const pointsA = createRectAndLabel(areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat, "#FF0000", "区域A");
  const pointsB = createRectAndLabel(areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat, "#0000FF", "区域B");

  if (map && pointsA.length > 0 && pointsB.length > 0) {
    map.setViewport([...pointsA, ...pointsB]);
  } else if (map && pointsA.length > 0) {
    map.setViewport(pointsA);
  } else if (map && pointsB.length > 0) {
    map.setViewport(pointsB);
  }
}

/**
 * 绘制F9功能的最短路径及其起点终点标记。
 * 覆盖物会被添加到 window.f9PathRelatedOverlays 数组。
 */
function drawF9ShortestPath(pathPoints) {
  if (!pathPoints || pathPoints.length < 2 || !map || !BMapGL) return;

  // 先清除旧的路径相关覆盖物
  if (window.f9PathRelatedOverlays && Array.isArray(window.f9PathRelatedOverlays)) {
    window.f9PathRelatedOverlays.forEach(overlay => {
      if (map.removeOverlay && overlay) try { map.removeOverlay(overlay); } catch(e) {/* मौन */}
    });
  }
  window.f9PathRelatedOverlays = [];

  const bmapPoints = pathPoints.map(
      (p) => new BMapGL.Point(Number.parseFloat(p.longitude), Number.parseFloat(p.latitude))
  );

  const pathPolyline = new BMapGL.Polyline(bmapPoints, {
    strokeColor: "#00FF00", strokeWeight: 4, strokeOpacity: 0.8,
  });
  map.addOverlay(pathPolyline);
  window.f9PathRelatedOverlays.push(pathPolyline);

  const startMarker = new BMapGL.Marker(bmapPoints[0]);
  map.addOverlay(startMarker);
  window.f9PathRelatedOverlays.push(startMarker);

  const endMarker = new BMapGL.Marker(bmapPoints[bmapPoints.length - 1]);
  map.addOverlay(endMarker);
  window.f9PathRelatedOverlays.push(endMarker);
}

/**
 * 清除F9功能绘制的所有特定覆盖物（区域和路径）。
 * 这个函数会被全局的 clearOverlays (来自map-utils.js)调用，
 * 也会在F9功能开始一次新分析前被调用。
 */
window.clearF9Overlays = () => {
  // console.log("F9: 清除所有F9特定覆盖物调用。"); // 可按需保留或移除日志
  if (window.f9AreaOverlays && Array.isArray(window.f9AreaOverlays)) {
    window.f9AreaOverlays.forEach((overlay) => {
      if (map && map.removeOverlay && overlay) {
        try { map.removeOverlay(overlay); } catch(e) { console.warn("F9: 移除区域覆盖物出错", e, overlay); }
      }
    });
  }
  window.f9AreaOverlays = [];

  if (window.f9PathRelatedOverlays && Array.isArray(window.f9PathRelatedOverlays)) {
    window.f9PathRelatedOverlays.forEach((overlay) => {
      if (map && map.removeOverlay && overlay) {
        try { map.removeOverlay(overlay); } catch(e) { console.warn("F9: 移除路径相关覆盖物出错", e, overlay); }
      }
    });
  }
  window.f9PathRelatedOverlays = [];
};

// 主分析函数
function performCommunicationTimeAnalysis(
    startTime, endTime,
    valAreaATopLeftLng, valAreaATopLeftLat, valAreaABottomRightLng, valAreaABottomRightLat,
    valAreaBTopLeftLng, valAreaBTopLeftLat, valAreaBBottomRightLng, valAreaBBottomRightLat
) {
  const resultDivF9 = document.getElementById("f9_result");
  if (!resultDivF9) { console.error("F9: 未找到 f9_result 元素"); return; }
  resultDivF9.innerHTML = "<p>正在进行通信时间分析...</p>";

  // 在开始新的分析前，清除之前的路径 (区域已在按钮点击时重新绘制或清除)
  if (window.f9PathRelatedOverlays && Array.isArray(window.f9PathRelatedOverlays)) {
    window.f9PathRelatedOverlays.forEach(overlay => {
      if (map && map.removeOverlay && overlay) try { map.removeOverlay(overlay); } catch(e) {}
    });
  }
  window.f9PathRelatedOverlays = [];

  const params = {
    startTime: startTime,
    endTime: endTime,
    regionA: {
      minLon: Number.parseFloat(valAreaATopLeftLng),
      maxLat: Number.parseFloat(valAreaATopLeftLat),
      maxLon: Number.parseFloat(valAreaABottomRightLng),
      minLat: Number.parseFloat(valAreaABottomRightLat)
    },
    regionB: {
      minLon: Number.parseFloat(valAreaBTopLeftLng),
      maxLat: Number.parseFloat(valAreaBTopLeftLat),
      maxLon: Number.parseFloat(valAreaBBottomRightLng),
      minLat: Number.parseFloat(valAreaBBottomRightLat)
    }
  };
  console.log("F9 发送给后端的参数:", JSON.stringify(params, null, 2));

  const baseURL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
  const apiUrl = `${baseURL}/travelTime/analyze`;
  const featureName = "F9通行时间分析";

  fetch(apiUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(params),
  })
      .then(response => { // 应用简化的错误处理逻辑
        console.log(`${featureName} 响应状态:`, response.status, "OK状态:", response.ok);
        if (!response.ok) {
          return response.json()
              .then(errorData => {
                let errorMessage = `网络响应异常，状态码: ${response.status}`;
                console.log(`${featureName} 成功解析后端错误JSON:`, errorData);
                if (errorData && errorData.details && errorData.details.length > 0) {
                  const detailsText = errorData.details.map(detail => escapeHtml(detail)).join('; ');
                  errorMessage += `<br/>详情: ${detailsText}`;
                } else if (errorData && errorData.message) {
                  errorMessage += `<br/>服务器消息: ${escapeHtml(errorData.message)}`;
                } else {
                  errorMessage += `。无法获取详细错误信息（非JSON或无内容）。`;
                }
                throw new Error(errorMessage);
              })
              .catch(async (parsingErrorOrThrownError) => {
                if (parsingErrorOrThrownError.message.includes("网络响应异常，状态码:")) {
                  throw parsingErrorOrThrownError;
                }
                console.warn(`${featureName} 解析JSON错误或处理错误数据时出错:`, parsingErrorOrThrownError);
                let responseTextForDebug = "";
                try {
                  const textResponse = response.clone();
                  responseTextForDebug = await textResponse.text();
                  console.warn(`${featureName} 后端返回的原始文本内容:`, responseTextForDebug);
                } catch (e) { /* 忽略 */ }
                let finalErrorMessage = `网络响应异常，状态码: ${response.status}`;
                if (responseTextForDebug) {
                  finalErrorMessage += `<br/>服务器原始响应 (部分): ${escapeHtml(responseTextForDebug.substring(0,200))}`;
                } else {
                  finalErrorMessage += `。无法获取服务器返回的详细错误内容。`;
                }
                throw new Error(finalErrorMessage);
              });
        }
        return response.json();
      })
      .then(data => {
        const resultDiv = document.getElementById("f9_result");
        if (resultDiv) {
          if (data && data.found) {
            let resultHtml = `<p>通信时间分析结果：</p><ul>`;
            resultHtml += `<li>最短通行时间: ${escapeHtml(data.minTravelTimeFormatted || "N/A")}</li>`;
            if (data.shortestPath && data.shortestPath.length > 0) {
              resultHtml += `<li>出租车ID: ${escapeHtml(data.shortestPath[0].taxiId)}</li>`;
              drawF9ShortestPath(data.shortestPath);
            } else {
              resultHtml += `<li>未找到具体路径点。</li>`;
              // 如果没有路径点，也确保清除旧的路径线
              if (window.f9PathRelatedOverlays && Array.isArray(window.f9PathRelatedOverlays)) {
                window.f9PathRelatedOverlays.forEach(overlay => { if (map && map.removeOverlay && overlay) try { map.removeOverlay(overlay); } catch(e) {} });
                window.f9PathRelatedOverlays = [];
              }
            }
            resultHtml += "</ul>";
            resultDiv.innerHTML = resultHtml;
          } else if (data && data.message) {
            resultDiv.innerHTML = `<p>分析提示：${escapeHtml(data.message)}</p>`;
            clearOldPath(); // 清除可能存在的旧路径
          } else {
            resultDiv.innerHTML = "<p>未找到符合条件的通行路径，或未获取到有效分析结果。</p>";
            clearOldPath(); // 清除可能存在的旧路径
          }
        }
      })
      .catch(error => {
        const resultDiv = document.getElementById("f9_result");
        if (resultDiv) {
          resultDiv.innerHTML = `<p class="error-message">查询出错：<br/>${error.message}</p>`;
        }
        console.error(`${featureName} 查询出错详情 (Error Object):`, error);
        clearOldPath(); // 出错时也清除旧路径
      });

  function clearOldPath() {
    if (window.f9PathRelatedOverlays && Array.isArray(window.f9PathRelatedOverlays)) {
      window.f9PathRelatedOverlays.forEach(overlay => { if (map && map.removeOverlay && overlay) try { map.removeOverlay(overlay); } catch(e) {} });
      window.f9PathRelatedOverlays = [];
    }
  }
}

// --- DOMContentLoaded 事件监听器 ---
document.addEventListener("DOMContentLoaded", () => {
  const communicationTimeBtn = document.getElementById("communicationTimeBtn");
  if (communicationTimeBtn) {
    communicationTimeBtn.addEventListener("click", () => {
      const startTime = document.getElementById("f9_startTime").value;
      const endTime = document.getElementById("f9_endTime").value;
      const areaATopLeftLng = document.getElementById("f9_areaA_topLeftLng").value;
      const areaATopLeftLat = document.getElementById("f9_areaA_topLeftLat").value;
      const areaABottomRightLng = document.getElementById("f9_areaA_bottomRightLng").value;
      const areaABottomRightLat = document.getElementById("f9_areaA_bottomRightLat").value;
      const areaBTopLeftLng = document.getElementById("f9_areaB_topLeftLng").value;
      const areaBTopLeftLat = document.getElementById("f9_areaB_topLeftLat").value;
      const areaBBottomRightLng = document.getElementById("f9_areaB_bottomRightLng").value;
      const areaBBottomRightLat = document.getElementById("f9_areaB_bottomRightLat").value;


      if (!startTime || !endTime || !areaATopLeftLng || !areaATopLeftLat || /* ... */ !areaBBottomRightLat ) {
        alert("F9: 请填写所有区域坐标和时间段。");
        return;
      }

      if (typeof window.clearF9Overlays === "function") { // 先清除所有F9相关的旧覆盖物
        window.clearF9Overlays();
      }
      drawF9AreaRectangles(
          areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat,
          areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat
      );

      performCommunicationTimeAnalysis(
          startTime,endTime,
          areaATopLeftLng, areaATopLeftLat, areaABottomRightLng, areaABottomRightLat,
          areaBTopLeftLng, areaBTopLeftLat, areaBBottomRightLng, areaBBottomRightLat
      );
    });
  }
});
