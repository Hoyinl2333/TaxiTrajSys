// 文件: static/js/features/f7-FrequentPath1.js

// --- F7模块初始化：在全局覆盖物管理器中注册专属条目 ---
if (typeof window.allFeatureOverlays === 'undefined') {
  window.allFeatureOverlays = {};
}
// F7 主要绘制路径折线和可能的起终点标记
window.allFeatureOverlays["F7_路径覆盖物"] = [];
// console.log("F7-FrequentPath1: 已在 window.allFeatureOverlays 中初始化 'F7_路径覆盖物' 数组。");


// 全局变量存储路径数据 (可以考虑将其移入IIFE或特定对象以避免污染全局，但暂时保持)
window.pathFrequenciesDataF7 = []; // 重命名以区分
window.currentPathIndexF7 = 0;
// window.pathPolylines = []; // 不再需要这个独立的全局数组，将使用 allFeatureOverlays["F7_路径覆盖物"]

document.addEventListener("DOMContentLoaded", () => {
  const frequentPath1Btn = document.getElementById("frequentPath1Btn");
  const pathSelector = document.getElementById("path_selector"); // F7的路径选择器
  const pathSelectorContainer = document.getElementById("pathSelectorContainer"); // F7的路径选择器容器

  if (pathSelectorContainer) pathSelectorContainer.style.display = "none";

  if (frequentPath1Btn) {
    frequentPath1Btn.addEventListener("click", () => {
      const kValue = document.getElementById("f7_k").value;
      const minDistanceValue = document.getElementById("f7_distance").value;
      // 假设输入总是有效的
      performFrequentPathAnalysis1(kValue, minDistanceValue);
    });
  }

  if (pathSelector) {
    pathSelector.addEventListener("change", function () {
      const selectedIndex = Number.parseInt(this.value);
      if (!isNaN(selectedIndex) && selectedIndex >= 0 && selectedIndex < window.pathFrequenciesDataF7.length) {
        window.currentPathIndexF7 = selectedIndex;
        displayF7PathOnMap(selectedIndex);
        // updatePathDetails(selectedIndex); // 如果需要，此函数也应更新
      }
    });
  }

  const prevPathBtn = document.getElementById("prevPathBtn"); // F7的上一条按钮
  const nextPathBtn = document.getElementById("nextPathBtn"); // F7的下一条按钮

  if (prevPathBtn) {
    prevPathBtn.addEventListener("click", () => {
      if (window.pathFrequenciesDataF7.length === 0) return;
      window.currentPathIndexF7 = (window.currentPathIndexF7 - 1 + window.pathFrequenciesDataF7.length) % window.pathFrequenciesDataF7.length;
      if(pathSelector) pathSelector.value = window.currentPathIndexF7;
      displayF7PathOnMap(window.currentPathIndexF7);
      // updatePathDetails(window.currentPathIndexF7);
    });
  }

  if (nextPathBtn) {
    nextPathBtn.addEventListener("click", () => {
      if (window.pathFrequenciesDataF7.length === 0) return;
      window.currentPathIndexF7 = (window.currentPathIndexF7 + 1) % window.pathFrequenciesDataF7.length;
      if(pathSelector) pathSelector.value = window.currentPathIndexF7;
      displayF7PathOnMap(window.currentPathIndexF7);
      // updatePathDetails(window.currentPathIndexF7);
    });
  }

  // 初始化F7在allFeatureOverlays中的条目 (双重保险)
  if (typeof window.allFeatureOverlays !== 'object' || window.allFeatureOverlays === null) {
    window.allFeatureOverlays = {};
  }
  if (!window.allFeatureOverlays["F7_路径覆盖物"]) {
    window.allFeatureOverlays["F7_路径覆盖物"] = [];
  }
});

function performFrequentPathAnalysis1(k, minDistance) {
  const resultDiv = document.getElementById("f7_result");
  const pathSelectorContainer = document.getElementById("pathSelectorContainer");
  // const pathDetails = document.getElementById("path_details");

  resultDiv.innerHTML = "<p>正在进行全市频繁路径分析...</p>";
  if (pathSelectorContainer) pathSelectorContainer.style.display = "none";
  // if (pathDetails) pathDetails.style.display = "none";

  // --- 关键修改：在执行任何F7特定操作前，调用全局清除函数 ---
  if (typeof clearOverlays === "function") {
    // console.log("F7 (performFrequentPathAnalysis1): 调用全局 clearOverlays()。");
    clearOverlays();
  } else {
    console.warn("F7 (performFrequentPathAnalysis1): 全局 clearOverlays() 函数未定义!");
  }
  // 原先的 clearPathPolylines(); 不再需要，因为全局 clearOverlays 会处理 F7_路径覆盖物

  const params = {
    k: Number.parseInt(k, 10),
    minPathDistanceKM: Number.parseFloat(minDistance),
  };
  // console.log("F7 发送给后端的参数:", JSON.stringify(params, null, 2));

  const baseURL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
  const apiUrl = `${baseURL}/paths/frequent/citywide`;
  const featureName = "F7全市频繁路径分析";

  fetchApi(apiUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(params),
  }, featureName)
      .then(data => {
        const f7ResultDiv = document.getElementById("f7_result"); // 使用局部变量
        const pathSelector = document.getElementById("path_selector");
        const f7PathSelectorContainer = document.getElementById("pathSelectorContainer"); // 使用局部变量

        if (data && data.pathFrequencies && data.pathFrequencies.length > 0) {
          window.pathFrequenciesDataF7 = data.pathFrequencies;
          window.currentPathIndexF7 = 0;
          f7ResultDiv.innerHTML = `<p>频繁路径分析结果：找到 ${data.pathFrequencies.length} 条频繁路径。</p>`;
          if (pathSelector) {
            pathSelector.innerHTML = "";
            data.pathFrequencies.forEach((pathFrequency, index) => {
              const option = document.createElement("option");
              option.value = index;
              option.textContent = `路径 ${index + 1} (频率: ${pathFrequency.frequency})`;
              pathSelector.appendChild(option);
            });
            pathSelector.value = 0;
          }
          if (f7PathSelectorContainer) f7PathSelectorContainer.style.display = "flex";
          displayF7PathOnMap(0);
          // updatePathDetails(0);
        } else {
          f7ResultDiv.innerHTML = "<p>未获取到有效的全市频繁路径结果。</p>";
          if (f7PathSelectorContainer) f7PathSelectorContainer.style.display = "none";
          window.pathFrequenciesDataF7 = [];
          // displayF7PathOnMap会处理空数据情况（通过先调用全局clear然后不画任何东西）
        }
      })
      .catch(error => {
        displayFetchError(error, "f7_result", featureName);
        const f7PathSelectorContainer = document.getElementById("pathSelectorContainer");
        if (f7PathSelectorContainer) f7PathSelectorContainer.style.display = "none";
        window.pathFrequenciesDataF7 = [];
        if (typeof clearOverlays === "function") { clearOverlays(); } // 确保错误时也清理
      });
}

function displayF7PathOnMap(pathIndex) {
  // 全局清除已在 performFrequentPathAnalysis1 开头或切换路径时处理
  // 这里不需要再次调用全局 clearOverlays，但如果只想清除F7自己的上一次绘制，则需要。
  // 按照“每个任务开始时清除所有”的原则，切换路径也算一个小任务。
  if (typeof clearOverlays === "function") {
    // console.log("F7 (displayF7PathOnMap): 为显示新路径，调用全局 clearOverlays()。");
    clearOverlays(); // 清除所有，然后重绘F7的这条路径
  }

  if (!window.pathFrequenciesDataF7 || pathIndex < 0 || pathIndex >= window.pathFrequenciesDataF7.length) {
    // console.error("F7: 无效的路径索引或路径数据为空:", pathIndex);
    return;
  }

  const pathData = window.pathFrequenciesDataF7[pathIndex];
  if (!pathData || !pathData.pathCoordinates || pathData.pathCoordinates.length === 0) {
    // console.warn("F7: 选中的路径数据无效或没有坐标点:", pathData);
    return;
  }
  const coordinates = pathData.pathCoordinates;
  const bmapPoints = coordinates.map(coord => new BMapGL.Point(coord.longitude, coord.latitude));

  convertCoordinates(bmapPoints, (data) => {
    if (data.status === 0 && data.points && data.points.length > 0) {
      const polyline = new BMapGL.Polyline(data.points, {
        strokeColor: getF7PathColor(pathIndex), // 使用 F7 特定的颜色函数
        strokeWeight: 5,
        strokeOpacity: 0.8,
      });
      map.addOverlay(polyline);
      // --- 关键修改：将覆盖物添加到 F7 在全局管理器中的专属数组 ---
      window.allFeatureOverlays["F7_路径覆盖物"].push(polyline);

      if (data.points.length > 0) {
        const startDot = addDotToMap(data.points[0], "green"); // addDotToMap 返回覆盖物
        if(startDot) window.allFeatureOverlays["F7_路径覆盖物"].push(startDot);

        const endDot = addDotToMap(data.points[data.points.length - 1], "red");
        if(endDot) window.allFeatureOverlays["F7_路径覆盖物"].push(endDot);
      }
      map.setViewport(data.points);
    } else {
      // console.error("F7: 路径坐标转换失败或无有效点返回。", data);
    }
  });
}



function getF7PathColor(index) { // 重命名以区分
  const colors = ["#FF5252", "#4CAF50", "#2196F3", "#FFC107", "#9C27B0", "#00BCD4", "#FF9800", "#795548", "#607D8B", "#E91E63"];
  return colors[index % colors.length];
}

