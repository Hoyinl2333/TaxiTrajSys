# 出租车轨迹数据分析系统 v2.0

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase-jdk21-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9.9-orange.svg)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**出租车轨迹数据分析系统 (Taxi Trajectory Analysis System)** 是一个基于 Spring Boot 的综合性平台，用于处理、分析和可视化出租车的GPS轨迹数据。本系统是SCUT数据结构大作业的一个实现。v2.0版本在稳定性和性能上进行了显著优化，并对核心功能进行了增强。

## ✨ 主要特性 (v2.0)

系统提供以下核心分析功能 (F1-F9)：

* **F1: 轨迹查询与可视化:**
    * 根据出租车ID高效查询其历史轨迹。
    * 在百度地图上动态、清晰地展示轨迹点。
* **F2: 智能地图交互:**
    * 支持地图的平移、缩放等基本操作。
    * 轨迹点大小会根据地图缩放级别自适应调整，优化不同层级下的可视性。
* **F3: 区域车辆统计:**
    * 快速查询指定矩形地理区域和时间范围内出现过的出租车数量及代表性的GPS点位信息。
* **F4: 区域车流密度分析:**
    * 高效分析指定地理区域在不同时间槽内的车辆密度。
    * 通过优化的热力图在地图上直观展示密度分布，支持按时间槽流畅切换查看。
* **F5: 两区域间车流量关联分析:**
    * 分析两个指定区域（例如A区域与B区域）之间，在不同时间槽内的双向车流量（A到B，B到A）。
* **F6: 单区域进出车流量关联分析:**
    * 分析指定区域在不同时间槽内，与外部区域的车辆交互情况（进入该区域的车辆数，离开该区域的车辆数）。
* **F7: 全市频繁路径分析:**
    * 基于优化的网格化方法和并行处理，高效识别并挖掘在整个城市范围内出现频率最高的Top-K条行驶路径。
    * 路径结果以网格中心点坐标序列的形式提供，方便前端可视化。
* **F8: 区域间频繁路径分析:**
    * 专注于两个指定区域（起始区域A，目标区域B），高效识别从A到B的Top-K高频路径。
* **F9: 通行时间分析:**
    * 精确计算从一个指定区域A到另一个指定区域B的最短通行时间，并返回对应的优化轨迹段。

## 🚀 技术栈与优化 (v2.0)

* **后端:**
    * Java 21
    * Spring Boot 3.4.3 (Web, Validation)
    * Maven (项目管理和构建)
* **前端:**
    * HTML5, CSS3, JavaScript (ES6+)
    * 百度地图API v1.0 (WebGL) (用于地图展示和可视化)
* **数据存储:**
    * 本地文本文件 (`*.txt`) 作为原始数据源。

## 🛠️ 系统架构与关键改进 (v2.0.0)

* **分层设计:** 保持清晰的Controller-Service-Repository分层架构。
    * **Controller层:** 统一处理HTTP请求，利用Spring Validation进行参数校验，调用服务层，并返回结构化的JSON响应。
    * **Service层:** 实现核心业务逻辑，v2.0对分析算法进行了优化，提升了执行效率和内存管理。
    * **Repository层:** 负责数据的读取和访问。`LocalFileTaxiRepository` 结合 `InMemoryTaxiCache` 和流式解析器 `TxtTaxiRecordParser`，实现了高效的数据检索和缓存策略。
* **数据模型:** 核心数据对象 (如 `TaxiRecord`, `Region`, `Grid`, `Path`, `TravelTimeResult`) 结构清晰，并应用了Lombok注解。
* **健壮的输入校验:** 全面使用JSR 303 Bean Validation及自定义校验注解 (`@ValidGeoBoundingBox`, `@ValidTimeRange`) 对API输入参数进行严格校验，确保了API接口的稳定性和数据质量。
* **完善的异常处理:** 通过 `@ControllerAdvice` 实现的 `GlobalExceptionHandler` 能够捕获并处理各类异常，向客户端返回统一、友好的错误响应格式 (`ErrorResponse`)。
* **高效缓存机制:** `InMemoryTaxiCache` 基于Caffeine实现，有效缓存了出租车轨迹数据，避免了重复的文件解析，大幅提升了数据访问性能。
* **优化的地理编码与网格化:**
    * `GeoUtils`: 提供精确的地理坐标计算功能。
    * `Grid` & `GridCell`: 地图网格化实现得到优化，为密度分析和频繁路径挖掘提供了更高效的基础。
* **改进的前端交互与覆盖物管理:**
    * `taxi-trajectory.html`: 主交互页面，集成了所有功能模块的输入表单和结果展示区域。
    * `map-utils.js`: 封装了百度地图的初始化、**全局覆盖物管理器 (`window.allFeatureOverlays`)**、以及其他地图通用操作，确保了地图覆盖物的统一管理和按需清除。
    * `apiService.js`: 提供了统一的 `WorkspaceApi` 函数，简化了与后端API的异步通信，并集成了标准化的错误处理和信息展示逻辑。8-2-2
    * 各功能模块JS (`f1-trajectory.js` 至 `f9-TravelTime.js`): 实现了对应功能的客户端逻辑，能够清晰地调用API、解析结果并在地图上进行可视化。

## ⚙️ 环境配置与运行

### 先决条件

* JDK 21 或更高版本。
* Apache Maven 3.6.x 或更高版本 (项目使用 Maven Wrapper, `wrapperVersion=3.3.2` 指向 Maven 3.9.9)。
* 稳定的网络连接（用于下载依赖和访问百度地图API）。

### 数据准备

1.  **下载出租车轨迹数据：** 项目依赖 `.txt` 格式的出租车轨迹数据文件。每个文件代表一辆出租车的轨迹，文件名即为出租车ID (例如 `123.txt`)。[下载地址/数据源](http://research.microsoft.com/apps/pubs/?id=152883)
- 数据详情：10,375辆出租⻋的GPS轨迹数据，涵盖7天（2008.2.2-200.2.8），包含出租车ID、时间、经纬度信息。
- 每行数据格式：`出租车ID,时间戳(yyyy-MM-dd HH:mm:ss),经度,纬度`。例如：`295,2008-02-02 15:36:08,116.51172,39.92123`
3.  **存放数据：** 将所有轨迹数据文件放置到项目的 `src/main/resources/data/` 目录下。系统会根据 `application.properties` 中的 `taxi.data.path` 配置（默认为 `classpath:data/*.txt`）自动加载此目录下的所有 `.txt` 文件。

### 项目构建与运行

1.  **克隆仓库:**
    ```bash
    git clone <your-repository-url>
    cd TaxiTrajectory
    ```
2.  **构建项目 (使用 Maven Wrapper):**
    * 在Windows上:
        ```bash
        mvnw clean package
        ```
    * 在Linux/macOS上:
        ```bash
        ./mvnw clean package
        ```
    这将编译代码、运行测试并打包成一个可执行的JAR文件，位于 `target/TaxiTrajectory-0.0.1-SNAPSHOT.jar`。

3.  **运行应用:**
    ```bash
    java -jar target/TaxiTrajectory-0.0.1-SNAPSHOT.jar
    ```
    （可选）您可以根据需要调整JVM参数，例如内存分配：
    ```bash
    java -Xms512m -Xmx4096m -jar target/TaxiTrajectory-0.0.1-SNAPSHOT.jar
    ```
4.  **访问应用:**
    应用启动后，默认监听端口为 `8080` (可在 `application.properties` 中通过 `server.port` 修改)。 打开浏览器访问：
    `http://localhost:8080/`
    这将自动跳转到 `http://localhost:8080/static/taxi-trajectory.html` (由 `RootController` 配置)。

### 配置文件

主要的应用程序配置位于 `src/main/resources/application.properties`。您可以根据需要调整以下关键参数：

* `server.port`: 应用监听端口。
* `taxi.data.path`: 出租车数据文件路径。
* `logging.service.enabled`, `logging.repository.enabled`: 控制服务层和仓库层详细日志的输出开关。
* `trajectory.analysis.min_trip_points`: 通行时间分析 (F9) 所需的最小行程点数。
* `trajectory.analysis.segmentation.max_time_gap_minutes`: 频繁路径分析中用于分割行程段的最大时间间隔。
* `cache.taxi.maxSize`, `cache.taxi.expireAfterAccessMinutes`: Caffeine缓存配置，用于优化数据读取。
* `map.bounds.*`, `map.gridSizeKM`: 地图边界和网格大小配置，影响密度分析和频繁路径分析的范围与粒度。

## 🧪 测试

项目包含了一套 JUnit 5 测试，覆盖了核心服务和数据仓库的功能，确保代码质量和功能正确性。

* **测试组件:**
    * `TravelTimeServiceTest`: 针对F9通行时间分析服务的集成测试，包括不同时间跨度的性能评估。
    * `TaxiRepositoryTest`: 测试出租车数据的加载、缓存命中/失效以及按ID和时间范围查询的准确性。
    * `RegionQueryServiceTest`: 验证区域查询服务 (F3) 在不同区域和时间条件下的正确性。
    * `FrequentPathServiceTest`: 对F7（全市）和F8（区域间）频繁路径分析模块进行功能和性能测试，考虑了内存占用。
    * `DensityAnalysisServiceTest`: 测试F4车流密度分析服务，覆盖不同时间槽、网格大小和地理边界的场景。
    * `CorrelationServiceTest`: 对F5和F6区域关联分析服务进行测试，验证车流量统计的逻辑。
* **前端简单UI测试:** `TaxiTrajectoryApplicationTests` 使用 Selenium WebDriver 自动化执行一个基础的页面加载和交互测试，验证前端页面的可访问性。

运行所有测试：
```bash
mvnw test
````

## 🤝 贡献

欢迎对本项目进行贡献！如果您有任何建议、发现bug或希望添加新功能，请通过以下方式参与：

1.  Fork 本仓库。
2.  创建您的特性分支 (`git checkout -b feature/YourAmazingFeature`)。
3.  提交您的更改 (`git commit -m 'Add some YourAmazingFeature'`)。
4.  将您的更改推送到分支 (`git push origin feature/YourAmazingFeature`)。
5.  开启一个 Pull Request。

## 📜 开源许可

本项目采用 **MIT 许可证**。详情请参阅 `LICENSE.txt` 文件 (如果项目中尚未包含，请添加标准的MIT许可证文本到该文件)。

## 📞 联系与支持

如果您在使用过程中遇到任何问题或有任何疑问，请通过项目Issue进行反馈。

-----
