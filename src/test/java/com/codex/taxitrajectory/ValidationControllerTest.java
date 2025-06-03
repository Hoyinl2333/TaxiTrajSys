package com.codex.taxitrajectory;

import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.query.RegionQuery;
import com.codex.taxitrajectory.model.query.TravelTimeQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ValidationControllerTest 负责测试Controller层中DTO的错误检查
 */
@SpringBootTest
@AutoConfigureMockMvc
class ValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- 测试 @ValidTimeRange ---

    @Test
    @DisplayName("测试 @ValidTimeRange - 当startTime晚于endTime时应校验失败")
    void testValidTimeRange_whenStartTimeAfterEndTime_shouldFailValidation() throws Exception {
        Region validRegion = new Region(116.0, 36.0, 117.0, 40.0);
        TravelTimeQuery invalidQuery = new TravelTimeQuery(
                validRegion,
                validRegion,
                LocalDateTime.of(2024, 1, 1, 10, 0, 0), // startTime
                LocalDateTime.of(2024, 1, 1, 8, 0, 0)   // endTime (早于startTime)
        );

        ResultActions resultActions = mockMvc.perform(post("/travelTime/analyze") // 替换为你的实际端点
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidQuery)))
                .andExpect(status().isBadRequest()) // 预期400错误
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details[0]").value("travelTimeQuery: 结束时间必须在开始时间之后或与开始时间相同")); // 验证具体的错误信息

        System.out.println("testValidTimeRange_whenStartTimeAfterEndTime_shouldFailValidation 响应: " + resultActions.andReturn().getResponse().getContentAsString());
    }



    // --- 测试 @ValidGeoBoundingBox ---

    @Test
    @DisplayName("测试 @ValidGeoBoundingBox (应用于RegionQuery) - 当minLon >= maxLon时应校验失败")
    void testValidGeoBoundingBox_onRegionQuery_whenMinLonGreaterOrEqualMaxLon_shouldFail() throws Exception {
        // RegionQuery 同时使用了 @ValidGeoBoundingBox 和 @ValidTimeRange
        // 这里我们主要测试 @ValidGeoBoundingBox
        RegionQuery invalidQuery = new RegionQuery(
                117.0, // minLongitude (大于maxLongitude)
                39.0,  // minLatitude
                116.0, // maxLongitude
                40.0,  // maxLatitude
                LocalDateTime.of(2024, 1, 1, 8, 0, 0),
                LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        );

        ResultActions resultActions = mockMvc.perform(post("/region/count") // 假设RegionQuery用在 /region/count 端点
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidQuery)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                // 消息来自 @ValidGeoBoundingBox 的默认 message
                .andExpect(jsonPath("$.details[0]").value("regionQuery: 无效的地理边界框：最小经纬度必须小于对应的最大经纬度"));

        System.out.println("testValidGeoBoundingBox_onRegionQuery_whenMinLonGreaterOrEqualMaxLon_shouldFail 响应: " + resultActions.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("测试 @ValidGeoBoundingBox (应用于RegionQuery) - 当minLat >= maxLat时应校验失败")
    void testValidGeoBoundingBox_onRegionQuery_whenMinLatGreaterOrEqualMaxLat_shouldFail() throws Exception {
        RegionQuery invalidQuery = new RegionQuery(
                116.0, // minLongitude
                40.0,  // minLatitude (大于maxLatitude)
                117.0, // maxLongitude
                39.0,  // maxLatitude
                LocalDateTime.of(2024, 1, 1, 8, 0, 0),
                LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        );

        mockMvc.perform(post("/region/count")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidQuery)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("regionQuery: 无效的地理边界框：最小经纬度必须小于对应的最大经纬度"));
    }


    @Test
    @DisplayName("测试 @ValidGeoBoundingBox (应用于TravelTimeQuery中的Region) - Region内部校验失败")
    void testValidGeoBoundingBox_onNestedRegion_shouldFail() throws Exception {
        Region invalidRegionA = new Region(118.0,39.0,116.0,120.0);
        Region invalidRegionB = new Region(115.0, 100.0, 117.0, 80.0);

        TravelTimeQuery query = new TravelTimeQuery(
                invalidRegionA,
                invalidRegionB,
                LocalDateTime.of(2024, 1, 1, 8, 0, 0),
                LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        );

        ResultActions resultActions = mockMvc.perform(post("/travelTime/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "regionA: 无效的地理边界框：最小经纬度必须小于对应的最大经纬度", // invalidRegionA: minLon > maxLon
                        "regionA.maxLat: 最大纬度必须是有效的地理坐标值 [-90, 90]",      // invalidRegionA: maxLat=120
                        "regionB.minLat: 最小纬度必须是有效的地理坐标值 [-90, 90]",      // invalidRegionB: minLat=100
                        "regionB: 无效的地理边界框：最小经纬度必须小于对应的最大经纬度" // invalidRegionB: minLat > maxLat
                )))
                .andExpect(jsonPath("$.details", hasSize(4))); // 预期4个错误

        System.out.println("testValidGeoBoundingBox_onNestedRegion_shouldFail 响应: " + resultActions.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("测试所有参数均有效的情况 - RegionQuery")
    void testAllValid_RegionQuery_shouldPass() throws Exception {
        RegionQuery validQuery = new RegionQuery(
                116.0, 39.0, 117.0, 40.0,
                LocalDateTime.of(2024, 1, 1, 8, 0, 0),
                LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        );

        mockMvc.perform(post("/region/count")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validQuery)))
                .andExpect(status().isOk());
    }
}