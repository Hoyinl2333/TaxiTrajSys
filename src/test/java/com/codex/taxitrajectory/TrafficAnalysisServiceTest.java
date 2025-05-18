package com.codex.taxitrajectory;
import com.codex.taxitrajectory.model.GPSPoint;
import com.codex.taxitrajectory.model.TaxiRecord;

import com.codex.taxitrajectory.model.GridCell;

import com.codex.taxitrajectory.repository.DataLoader;
import com.codex.taxitrajectory.service.TrafficAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.*;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class TrafficAnalysisServiceTest {
    private DataLoader mockDataLoader;
    private TrafficAnalysisService trafficAnalysisService;

    @BeforeEach
    public void setUp() {
        mockDataLoader = Mockito.mock(DataLoader.class);
        trafficAnalysisService = new TrafficAnalysisService(mockDataLoader);
    }



    /// //////////////////////////////////// F3 单元测试代码 ///////////////////////////////////////
    // TODO:F3测试

    @Test
    public void testOneTaxiInRegion() {
        // 试试能不能正确查到这条数据
        // 3,2008-02-04 00:53:56,116.75385,40.02127
        // 3,2008-02-04 00:58:56,116.75385,40.02127
        // 这辆车停了5min
//        LocalDateTime start = LocalDateTime.of(2008, 2, 4, 0, 54);
//        LocalDateTime end = LocalDateTime.of(2008, 2, 4, 0, 55);
//        GPSPoint leftUp = new GPSPoint(116.7537,40.0213);
//        GPSPoint rightDown = new GPSPoint(116.7539,40.0212);
//
//        int result = trafficAnalysisService.countTaxisInRegion(start,end,leftUp.getLongitude(),leftUp.getLatitude(),rightDown.getLongitude(),rightDown.getLatitude());
//        System.out.println(result);
//        assertEquals(1, result);
    }


    /////////////////////////////////////////////////////////////////////////////////////////


    /// //////////////////////////////////// F4 单元测试代码 ///////////////////////////////////////
//    @Test
//    public void testAnalyzeTrafficDensity_WithData() {
//        LocalDateTime start = LocalDateTime.of(2008, 2, 6, 0, 0);
//        LocalDateTime end = LocalDateTime.of(2008, 2, 6, 23, 59);
//        double r = 1.0;
//
//        Collection<String> taxiIds = Arrays.asList("taxi1", "taxi2");
//        when(mockDataLoader.getAllTaxiIds()).thenReturn(taxiIds);
//
//        List<TaxiRecord> records1 = new ArrayList<>();
//        records1.add(new TaxiRecord("taxi1", start, 1.5, 1.5));
//        when(mockDataLoader.getRecordsByTimeRange("taxi1", start, end)).thenReturn(records1);
//
//        List<TaxiRecord> records2 = new ArrayList<>();
//        records2.add(new TaxiRecord("taxi2", start, 1.6, 1.6));
//        when(mockDataLoader.getRecordsByTimeRange("taxi2", start, end)).thenReturn(records2);
//
//        Map<GridCell, Integer> expected = new HashMap<>();
//        GridCell gridCell = new GridCell();
//        gridCell.setRow(1);
//        gridCell.setCol(1);
//        expected.put(gridCell, 2);
//
//        System.out.println("开始执行 testAnalyzeTrafficDensity_WithData 测试用例...");
//        System.out.println("预期结果: " + expected);
//
//        Map<GridCell, Integer> result = trafficAnalysisService.analyzeTrafficDensity(start, end, r);
//        System.out.println("实际结果: " + result);
//        System.out.println("testAnalyzeTrafficDensity_WithData 测试用例执行成功！");
//    }
//
//    @Test
//    public void testAnalyzeTrafficDensity_NoData() {
//        LocalDateTime start = LocalDateTime.of(2008, 2, 6, 0, 0);
//        LocalDateTime end = LocalDateTime.of(2008, 2, 6, 23, 59);
//        double r = 1.0;
//
//        Collection<String> taxiIds = Collections.emptyList();
//        when(mockDataLoader.getAllTaxiIds()).thenReturn(taxiIds);
//
//        System.out.println("开始执行 testAnalyzeTrafficDensity_NoData 测试用例...");
//        System.out.println("预期结果: 空结果集");
//
//        Map<GridCell, Integer> result = trafficAnalysisService.analyzeTrafficDensity(start, end, r);
//        System.out.println("实际结果: " + result);
//
//        assertEquals(0, result.size(), "结果集大小不为 0");
//        System.out.println("testAnalyzeTrafficDensity_NoData 测试用例执行成功！");
//    }
///////////////////////////////////////////////////////////////////////////////////////////
//
//
//    /// //////////////////////////////////// F5 单元测试代码 ///////////////////////////////////////
//    @Test
//    public void testAnalyzeTrafficFlowBetweenRegions() {
//        LocalDateTime start = LocalDateTime.of(2008, 2, 6, 0, 0);
//        LocalDateTime end = LocalDateTime.of(2008, 2, 6, 23, 59);
//        double topLeftLongitude1 = 116.6;
//        double topLeftLatitude1 = 40.1;
//        double bottomRightLongitude1 = 116.2;
//        double bottomRightLatitude1 = 39.8;
//        double topLeftLongitude2 = 117.0;
//        double topLeftLatitude2 = 40.5;
//        double bottomRightLongitude2 = 116.8;
//        double bottomRightLatitude2 = 40.3;
//
//        Collection<String> taxiIds = Arrays.asList("taxi1", "taxi2");
//        Mockito.when(mockDataLoader.getAllTaxiIds()).thenReturn(taxiIds);
//
//        List<TaxiRecord> records1 = new ArrayList<>();
//        records1.add(new TaxiRecord("taxi1", start, 116.4, 39.9)); // 在区域1内
//        records1.add(new TaxiRecord("taxi1", end, 116.9, 40.4));   // 在区域2内
//        Mockito.when(mockDataLoader.getRecordsByTimeRange("taxi1", start, end)).thenReturn(records1);
//
//        List<TaxiRecord> records2 = new ArrayList<>();
//        records2.add(new TaxiRecord("taxi2", start, 116.9, 40.4)); // 在区域2内
//        records2.add(new TaxiRecord("taxi2", end, 116.4, 39.9));   // 在区域1内
//        Mockito.when(mockDataLoader.getRecordsByTimeRange("taxi2", start, end)).thenReturn(records2);
//
//        int[] result = trafficAnalysisService.analyzeTrafficFlowBetweenRegions(start, end,
//                topLeftLongitude1, topLeftLatitude1, bottomRightLongitude1, bottomRightLatitude1,
//                topLeftLongitude2, topLeftLatitude2, bottomRightLongitude2, bottomRightLatitude2);
//
//        assertEquals(1, result[0]);
//        assertEquals(1, result[1]);
//    }
/////////////////////////////////////////////////////////////////////////////////////////

}

