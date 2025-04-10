package com.anymind.pos.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anymind.pos.entity.CourierPaymentMap;
import com.anymind.pos.mapper.CourierPaymentMapMapper;
import com.anymind.pos.service.CourierPaymentMapService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CourierPaymentMapServiceImpl extends ServiceImpl<CourierPaymentMapMapper, CourierPaymentMap>
        implements CourierPaymentMapService {


    // In-memory cache: payment_method_id -> List of CourierPaymentMap
    private final Map<Integer, List<String>> courierMapCache = new ConcurrentHashMap<>();

    // Initialize cache on startup
    @PostConstruct
    public void init() {
        refreshCache();
    }

    // Periodic refresh (every 5 minutes)
    @Scheduled(fixedRate = 300000)
    public void scheduledRefresh() {
        refreshCache();
    }

    // Manual refresh method for hot updates
    @CacheEvict(value = "courierPaymentMap", allEntries = true)
    public void refreshCache() {
        List<CourierPaymentMap> mappings = this.list(new LambdaUpdateWrapper<>());
        Map<Integer, List<String>> newCache = mappings.stream()
                .collect(Collectors.groupingBy(
                        CourierPaymentMap::getPaymentMethodId,
                        Collectors.mapping(CourierPaymentMap::getCourier, Collectors.toList())
                ));
        courierMapCache.clear();
        courierMapCache.putAll(newCache);
    }

    // Get couriers by payment method ID with cache
    @Cacheable(value = "courierPaymentMap", key = "#paymentMethodId")
    @Override
    public List<String> getCouriersByPaymentMethodId(Integer paymentMethodId) {
        if (paymentMethodId == null) {
            return List.of();
        }

        // First check in-memory cache
        List<String> couriers = courierMapCache.get(paymentMethodId);
        if (couriers == null) {
            // If not in cache, query DB and update cache
            List<CourierPaymentMap> temp = this.list(new LambdaUpdateWrapper<CourierPaymentMap>().eq(CourierPaymentMap::getPaymentMethodId, paymentMethodId));
            if (temp != null && !temp.isEmpty()) {
                courierMapCache.put(paymentMethodId, temp.stream().map(String::valueOf).toList());
            }
        }
        return couriers != null ? couriers : Collections.emptyList();
    }

}

