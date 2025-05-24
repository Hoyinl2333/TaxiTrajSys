package com.codex.taxitrajectory.model.result;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * F3 功能（区域范围查找）的分析结果。
 * <p>
 * 封装了在指定区域和时间范围内找到的出租车数量以及这些出租车的代表性GPS点位信息。
 * </p>
 */
@Data
@NoArgsConstructor    // 提供无参构造函数
@AllArgsConstructor   // 提供全参构造函数
public class RegionQueryResult {

    /**
     * 在指定区域和时间内找到的唯一出租车数量。
     */
    private int taxiCount;

    /**
     * 在指定区域和时间内找到的出租车的GPS轨迹点集合。
     * 通常每个匹配到的出租车只包含其在该区域和时间内的第一个出现点，
     * 具体取决于服务层 {@link com.codex.taxitrajectory.service.RegionQueryService} 的实现。
     */
    private Set<TaxiRecord> gpsPoints;
}