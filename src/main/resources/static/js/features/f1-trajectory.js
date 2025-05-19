/**
 * F1: 出租车轨迹查询
 * 根据出租车ID获取轨迹数据，并在地图上显示
 */
function fetchTaxiTrajectory() {
    // 获取用户输入的出租车ID
    var taxiId = document.getElementById("taxiId").value
    if (taxiId === "") {
        alert("请输入出租车ID")
        return
    }

    // 获取功能区域内的结果显示div，而不是使用共用的result div
    var resultDiv = document.getElementById("f1_result")
    resultDiv.innerHTML = "<p>正在加载数据...</p>"

    // 清除地图上之前的覆盖物
    clearOverlays()

    // 构建API请求URL
    const baseURL = window.location.hostname === "localhost" ? "http://localhost:8080" : ""
    var url = `${baseURL}/taxi/${taxiId}`

    // 发送请求获取轨迹数据
    fetch(url)
        .then((response) => {
            if (!response.ok) throw new Error("网络响应异常")
            return response.json()
        })
        .then((data) => {
            // 在功能区域内显示结果，而不是在共用的result div中
            resultDiv.innerHTML = `<p>成功获取出租车 ${taxiId} 的轨迹数据，共 ${data.length} 个点</p>`

            // 清空共用的result div，确保结果只显示在功能区域内
            document.getElementById("result").innerHTML = ""

            // 处理轨迹点数据
            var points = []
            data.forEach((record) => {
                points.push(new BMapGL.Point(record.longitude, record.latitude))
            })

            // 坐标转换并在地图上显示
            convertCoordinates(points, (data) => {
                if (data.status === 0) {
                    data.points.forEach((point) => {
                        addDotToMap(point)
                    })

                    if (data.points.length > 0) {
                        map.setCenter(data.points[0])
                    }
                }
            })
        })
        .catch((error) => {
            // 在功能区域内显示错误信息
            resultDiv.innerHTML = `<p>查询出错：${error.message}</p>`

            // 清空共用的result div
            document.getElementById("result").innerHTML = ""

            // 清除地图上的覆盖物
            clearOverlays()
        })
}

// 添加事件监听器
document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("queryTrajectoryBtn").addEventListener("click", fetchTaxiTrajectory)
})