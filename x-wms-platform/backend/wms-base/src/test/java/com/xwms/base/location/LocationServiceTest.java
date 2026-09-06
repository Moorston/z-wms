package com.xwms.base.location;

import com.xwms.base.location.entity.Location;
import com.xwms.base.location.mapper.LocationMapper;
import com.xwms.base.location.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 库位服务单元测试
 * 核心测试：空库位查询/状态更新/S型路径排序
 */
class LocationServiceTest {

    @Mock
    private LocationMapper locationMapper;

    @InjectMocks
    private LocationService locationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindEmptyLocations() {
        Location loc1 = new Location();
        loc1.setLocationCode("A-01-01");
        loc1.setSortNo(1);
        Location loc2 = new Location();
        loc2.setLocationCode("A-01-02");
        loc2.setSortNo(2);
        when(locationMapper.selectList(any())).thenReturn(List.of(loc1, loc2));

        List<Location> result = locationService.findEmptyLocations("WH01", "A", "NORMAL", "STORAGE");

        assertEquals(2, result.size());
        assertEquals("A-01-01", result.get(0).getLocationCode());
    }

    @Test
    void testUpdateStatus() {
        Location loc = new Location();
        loc.setLocationCode("A-01-01");
        loc.setStatus("EMPTY");
        when(locationMapper.selectOne(any())).thenReturn(loc);
        when(locationMapper.updateById(any())).thenReturn(1);

        locationService.updateStatus("A-01-01", "NORMAL");

        assertEquals("NORMAL", loc.getStatus());
    }

    @Test
    void testUpdateStatus_notFound() {
        when(locationMapper.selectOne(any())).thenReturn(null);
        assertThrows(RuntimeException.class,
                () -> locationService.updateStatus("NOT_EXIST", "NORMAL"));
    }

    @Test
    void testListByRouteOrder_sShape() {
        // 奇数排正序，偶数排倒序
        Location loc1 = createLocation("A-01-01", "01", "01", "01");
        Location loc2 = createLocation("A-01-02", "01", "02", "01");
        Location loc3 = createLocation("A-02-01", "02", "01", "01");
        Location loc4 = createLocation("A-02-02", "02", "02", "01");
        when(locationMapper.selectList(any())).thenReturn(List.of(loc1, loc2, loc3, loc4));

        List<Location> result = locationService.listByRouteOrder("WH01",
                List.of("A-01-01", "A-01-02", "A-02-01", "A-02-02"));

        // 排01正序：01->02，排02倒序：02->01
        assertEquals("A-01-01", result.get(0).getLocationCode());
        assertEquals("A-01-02", result.get(1).getLocationCode());
        assertEquals("A-02-02", result.get(2).getLocationCode());
        assertEquals("A-02-01", result.get(3).getLocationCode());
    }

    @Test
    void testListByRouteOrder_empty() {
        List<Location> result = locationService.listByRouteOrder("WH01", List.of());
        assertTrue(result.isEmpty());
    }

    private Location createLocation(String code, String row, String col, String level) {
        Location loc = new Location();
        loc.setLocationCode(code);
        loc.setRowNo(row);
        loc.setColumnNo(col);
        loc.setLevelNo(level);
        return loc;
    }
}
