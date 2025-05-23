// 在 f1-trajectory.js 文件中

/**
 * F1: 出租车轨迹查询
 */
function fetchTaxiTrajectory() {
    var taxiId = document.getElementById("taxiId").value;


    var resultDiv = document.getElementById("f1_result");
    resultDiv.innerHTML = "<p>正在加载数据...</p>";
    clearOverlays();

    const baseURL = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
    var apiUrl = `${baseURL}/taxi/${taxiId}`;
    const featureName = "F1出租车轨迹查询"; // 定义功能名称

    // 使用通用的 fetchApi 函数
    fetchApi(apiUrl, { method: 'GET' }, featureName) // F1是GET请求，所以options可以简化或省略method
        .then((data) => { // data 是成功时解析后的业务数据
            const resultDiv = document.getElementById("f1_result"); // 重新获取，确保作用域
            if (resultDiv) {
                resultDiv.innerHTML = `<p>成功获取出租车 ${taxiId} 的轨迹数据，共 ${data.length} 个点</p>`;
            }

            var points = [];
            data.forEach((record) => {
                points.push(new BMapGL.Point(record.longitude, record.latitude));
            });

            convertCoordinates(points, (convertedData) => {
                if (convertedData.status === 0) {
                    convertedData.points.forEach((point) => {
                        addDotToMap(point);
                    });
                    if (convertedData.points.length > 0 && map) {
                        map.setCenter(convertedData.points[0]);
                    }
                } else {
                    console.error(`${featureName} 坐标转换失败:`, convertedData);
                    if(resultDiv) resultDiv.innerHTML += "<p>但坐标转换失败。</p>";
                }
            });
        })
        .catch((error) => { // 捕获由 fetchApi 抛出的Error对象
            // 使用通用的 displayFetchError 函数
            displayFetchError(error, "f1_result", featureName);
            clearOverlays(); // 出错时也清除地图
        });
}


document.addEventListener("DOMContentLoaded", () => {
    const queryBtn = document.getElementById("queryTrajectoryBtn");
    if (queryBtn) {
        queryBtn.addEventListener("click", fetchTaxiTrajectory);
    }
});