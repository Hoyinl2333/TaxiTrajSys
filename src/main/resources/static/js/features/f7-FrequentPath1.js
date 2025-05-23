/**
 * F7: 频繁路径分析1
 * 分析全市范围内的频繁路径，并在地图上显示
 */

// 全局变量存储路径数据 (保持不变)
window.pathFrequenciesData = [];
window.currentPathIndex = 0;
window.pathPolylines = []; // 存储路径折线对象



document.addEventListener("DOMContentLoaded", () => {
  const frequentPath1Btn = document.getElementById("frequentPath1Btn");
  const pathSelector = document.getElementById("path_selector");
  const pathSelectorContainer = document.getElementById("pathSelectorContainer");
  const pathDetails = document.getElementById("path_details"); // 虽然被注释掉了，但保留引用以防未来使用

  if (pathSelectorContainer) pathSelectorContainer.style.display = "none";
  // if (pathDetails) pathDetails.style.display = "none"; // 原来就有，保持

  if (frequentPath1Btn) {
    frequentPath1Btn.addEventListener("click", () => {
      const kValue = document.getElementById("f7_k").value;
      const minDistanceValue = document.getElementById("f7_distance").value;

      if (!kValue || !minDistanceValue) { // 基本的前端非空检查
        alert("请填写参数k和距离参数x");
        return;
      }
      // 清除之前的路径显示，但不清除地图上的其他覆盖物（如区域边界，如果F7也需要的话）
      clearPathPolylines(); // 这个函数只清除F7相关的路径线

      performFrequentPathAnalysis1(kValue, minDistanceValue);
    });
  }

  if (pathSelector) {
    pathSelector.addEventListener("change", function () {
      const selectedIndex = Number.parseInt(this.value);
      window.currentPathIndex = selectedIndex;
      displayPathOnMap(selectedIndex);
      updatePathDetails(selectedIndex); // 如果pathDetails启用，这个函数需要实现
    });
  }

  const prevPathBtn = document.getElementById("prevPathBtn");
  const nextPathBtn = document.getElementById("nextPathBtn");

  if (prevPathBtn) {
    prevPathBtn.addEventListener("click", () => {
      if (window.pathFrequenciesData.length === 0) return

      window.currentPathIndex =
          (window.currentPathIndex - 1 + window.pathFrequenciesData.length) % window.pathFrequenciesData.length
      pathSelector.value = window.currentPathIndex
      displayPathOnMap(window.currentPathIndex)
      updatePathDetails(window.currentPathIndex)
    })
  }

  if (nextPathBtn) {
    nextPathBtn.addEventListener("click", () => {
      if (window.pathFrequenciesData.length === 0) return

      window.currentPathIndex = (window.currentPathIndex + 1) % window.pathFrequenciesData.length
      pathSelector.value = window.currentPathIndex
      displayPathOnMap(window.currentPathIndex)
      updatePathDetails(window.currentPathIndex)
    })
  }
});

function performFrequentPathAnalysis1(k, minDistance) {
  const resultDiv = document.getElementById("f7_result");
  const pathSelectorContainer = document.getElementById("pathSelectorContainer");
  // const pathDetails = document.getElementById("path_details"); // 如果需要操作它

  if (!resultDiv) {
    console.error("未找到 f7_result 元素");
    return;
  }
  resultDiv.innerHTML = "<p>正在进行全市频繁路径分析...</p>";

  if (pathSelectorContainer) pathSelectorContainer.style.display = "none";
  // if (pathDetails) pathDetails.style.display = "none";

  clearPathPolylines(); // 再次确保清除旧路径

  const params = {
    k: Number.parseInt(k, 10),
    minPathDistanceKM: Number.parseFloat(minDistance),
    // F7 通常不包含 startTime, endTime, regionA, regionB
    // 如果您的 FrequentPathQuery DTO 中这些字段不是 @NotNull，则不传即可
  };
  // 调试日志
  console.log("F7 发送给后端的参数:", JSON.stringify(params, null, 2));


  const baseURL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
  const apiUrl = `${baseURL}/paths/frequent/citywide`;
  const featureName = "F7全市频繁路径分析";

  fetch(apiUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(params),
  })
      .then(response => {
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
      .then(data => { // 处理成功的数据
        const resultDiv = document.getElementById("f7_result"); // 确保获取正确的div
        const pathSelector = document.getElementById("path_selector");
        const pathSelectorContainer = document.getElementById("pathSelectorContainer");
        // const pathDetails = document.getElementById("path_details");

        if (resultDiv) {
          if (data && data.pathFrequencies && data.pathFrequencies.length > 0) {
            window.pathFrequenciesData = data.pathFrequencies; // 更新全局数据
            window.currentPathIndex = 0;

            resultDiv.innerHTML = `<p>频繁路径分析结果：找到 ${data.pathFrequencies.length} 条频繁路径。</p>`;

            if (pathSelector) {
              pathSelector.innerHTML = ""; // 清空旧选项
              data.pathFrequencies.forEach((pathFrequency, index) => {
                const option = document.createElement("option");
                option.value = index;
                option.textContent = `路径 ${index + 1} (频率: ${pathFrequency.frequency})`;
                pathSelector.appendChild(option);
              });
              pathSelector.value = 0; // 默认选中第一条
            }

            if (pathSelectorContainer) pathSelectorContainer.style.display = "flex";
            // if (pathDetails) pathDetails.style.display = "block"; // 如果启用

            displayPathOnMap(0); // 显示第一条路径
            updatePathDetails(0);    // 更新路径详情（如果启用）
          } else {
            resultDiv.innerHTML = "<p>未获取到有效的频繁路径结果。</p>";
            if (pathSelectorContainer) pathSelectorContainer.style.display = "none";
            // if (pathDetails) pathDetails.style.display = "none";
            window.pathFrequenciesData = []; // 清空数据
          }
        }
      })
      .catch(error => {
        const resultDiv = document.getElementById("f7_result");
        if (resultDiv) {
          resultDiv.innerHTML = `<p class="error-message">查询出错：<br/>${error.message}</p>`;
        }
        console.error(`${featureName} 查询出错详情 (Error Object):`, error);
        // 清理UI状态
        const pathSelectorContainer = document.getElementById("pathSelectorContainer");
        if (pathSelectorContainer) pathSelectorContainer.style.display = "none";
        window.pathFrequenciesData = [];
      });
}

function displayPathOnMap(pathIndex) {
  if (!window.pathFrequenciesData || pathIndex < 0 || pathIndex >= window.pathFrequenciesData.length) {
    console.error("F7: 无效的路径索引或路径数据为空: ", pathIndex);
    clearPathPolylines(); // 清除地图上可能存在的旧路径
    return;
  }

  clearPathPolylines(); // 清除之前的路径显示

  const pathData = window.pathFrequenciesData[pathIndex];
  if (!pathData || !pathData.pathCoordinates || pathData.pathCoordinates.length === 0) {
    console.warn("F7: 选中的路径数据无效或没有坐标点: ", pathData);
    return;
  }
  const coordinates = pathData.pathCoordinates;
  const bmapPoints = coordinates.map(coord => new BMapGL.Point(coord.longitude, coord.latitude));

  if (typeof convertCoordinates === "function") { // 确保坐标转换函数存在
    convertCoordinates(bmapPoints, (data) => { // 假设 convertCoordinates 是异步回调风格
      if (data.status === 0 && data.points && data.points.length > 0) {
        const polyline = new BMapGL.Polyline(data.points, {
          strokeColor: getPathColor(pathIndex), // 使用 getPathColor (需要定义)
          strokeWeight: 5,
          strokeOpacity: 0.8,
        });
        map.addOverlay(polyline);
        window.pathPolylines.push(polyline); // 添加到管理列表

        // 可选：添加起点和终点标记
        addDotToMap(data.points[0], "green"); // 假设 addDotToMap 来自 map-utils.js
        addDotToMap(data.points[data.points.length - 1], "red");

        // 调整视野以包含路径
        map.setViewport(data.points);
      } else {
        console.error("F7: 路径坐标转换失败或无有效点返回。", data);
      }
    });
  } else {
    console.error("F7: 坐标转换函数 (convertCoordinates) 未定义。");
    // 如果没有坐标转换，可以直接使用 bmapPoints 绘制，但可能存在坐标系问题
    // const polyline = new BMapGL.Polyline(bmapPoints, { /* ... */ });
    // map.addOverlay(polyline); window.pathPolylines.push(polyline);
    // map.setViewport(bmapPoints);
  }
}

/**
 * 更新路径详情显示（如果HTML中有对应区域）。
 * @param {number} pathIndex 路径索引。
 */
function updatePathDetails(pathIndex) {
  const pathDetailsDiv = document.getElementById("path_details"); // HTML中此div被注释
  if (!pathDetailsDiv || !window.pathFrequenciesData || pathIndex < 0 || pathIndex >= window.pathFrequenciesData.length) {
    // if (pathDetailsDiv) pathDetailsDiv.style.display = "none"; // 确保隐藏
    return;
  }

  // pathDetailsDiv.style.display = "block"; // 显示详情区域
  const pathData = window.pathFrequenciesData[pathIndex];
  let detailsHtml = `<h4>路径 ${pathIndex + 1} 详情 (频率: ${pathData.frequency})</h4>`;
  if (pathData.pathCoordinates && pathData.pathCoordinates.length > 0) {
    detailsHtml += '<div class="path-details-content"><ul>'; // 使用一个新类以便更好地控制样式
    pathData.pathCoordinates.forEach((coord, idx) => {
      detailsHtml += `<li>点 ${idx + 1}: (经:${coord.longitude.toFixed(5)}, 纬:${coord.latitude.toFixed(5)})</li>`;
    });
    detailsHtml += "</ul></div>";
  } else {
    detailsHtml += "<p>此路径无坐标点数据。</p>";
  }
  pathDetailsDiv.innerHTML = detailsHtml;
}

/**
 * 清除地图上由F7功能绘制的所有路径折线和相关点。
 */
function clearPathPolylines() {
  if (window.pathPolylines && Array.isArray(window.pathPolylines)) {
    window.pathPolylines.forEach((overlay) => {
      if (map && map.removeOverlay && overlay) {
        map.removeOverlay(overlay);
      }
    });
  }
  window.pathPolylines = []; // 清空数组

  // 如果 addDotToMap 是将点添加到全局 overlays 数组，需要额外清除
  // 或者让 addDotToMap 返回 overlay 对象，也加入到 pathPolylines
  // 为了简单，假设 addDotToMap 返回的对象已包含在 pathPolylines 中，或者有单独清除机制
  // 如果 addDotToMap 是将点添加到全局的 overlays 数组，并且希望在这里清除它们：
  // （这需要 map-utils.js 中的 overlays 是可访问的，并且 addDotToMap 确实添加到那里）
  // if (window.overlays && typeof clearSpecificOverlays === 'function') { // 假设有这样一个函数
  //    clearSpecificOverlays( (overlay) => overlay.type === "dot_f7" ); // 例如，给F7的点加个标记
  // }
}

/**
 * 为不同路径提供不同的颜色。
 * @param {number} index 路径的索引。
 * @returns {string} 表示颜色的十六进制字符串。
 */
function getPathColor(index) {
  const colors = ["#FF5252", "#4CAF50", "#2196F3", "#FFC107", "#9C27B0", "#00BCD4", "#FF9800", "#795548", "#607D8B", "#E91E63"];
  return colors[index % colors.length];
}
