package com.anymind.pos.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anymind.pos.config.annotation.ReadOnly;
import com.anymind.pos.dto.HourlySaleSummary;
import com.anymind.pos.entity.Payments;
import com.anymind.pos.mapper.PaymentsMapper;
import com.anymind.pos.redis.RedisDao;
import com.anymind.pos.service.PaymentsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Noe
 * @since 2025/04/09 20:59
 */
@Service
public class PaymentsServiceImpl extends ServiceImpl<PaymentsMapper, Payments> implements PaymentsService {
    @Resource
    private PaymentsMapper paymentsMapper;

    @Resource
    private RedisDao redisDao;

    private static final String CACHE_PREFIX = "sales:hourly:";
    private static final long CACHE_TTL_HOURS = 24L;


    @Override
    @ReadOnly
    public List<HourlySaleSummary> getSales(Date start, Date end) {
        // Generate a unique cache key based on the date range
        String cacheKey = generateCacheKey(start, end);

        // Try to fetch from Redis cache
        List<HourlySaleSummary> cachedSales = fetchFromCache(cacheKey);
        if (cachedSales != null && !cachedSales.isEmpty()) {
            return cachedSales; // Cache hit
        }

        // Cache miss: Fetch from database
        List<Payments> payments = paymentsMapper.selectByDateRange(start, end);
        Map<String, HourlySaleSummary> hourlySales = aggregateSalesByHour(payments);

        // Convert to sorted list
        List<HourlySaleSummary> result = hourlySales.values().stream()
                .sorted(Comparator.comparing(HourlySaleSummary::getDatetime))
                .collect(Collectors.toList());

        // Store in Redis cache
        cacheSales(cacheKey, result);

        return result;
    }

    private Map<String, HourlySaleSummary> aggregateSalesByHour(List<Payments> payments) {
        Map<String, HourlySaleSummary> hourlySales = new ConcurrentHashMap<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH':00:00Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        payments.parallelStream().forEach(payment -> {
            String hourKey = sdf.format(payment.getDatetime());
            hourlySales.compute(hourKey, (key, summary) -> {
                if (summary == null) {
                    summary = new HourlySaleSummary(hourKey);
                }
                summary.addSale(payment.getFinalPrice());
                summary.addPoints(payment.getPoints());
                return summary;
            });
        });

        return hourlySales;
    }

    private String generateCacheKey(Date start, Date end) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return CACHE_PREFIX + sdf.format(start) + "_" + sdf.format(end);
    }

    private List<HourlySaleSummary> fetchFromCache(String cacheKey) {
        Object cached = redisDao.get(cacheKey);
        if (cached instanceof List) {
            return (List<HourlySaleSummary>) cached;
        }
        return null;
    }

    private void cacheSales(String cacheKey, List<HourlySaleSummary> sales) {
        if (!sales.isEmpty()) {
            redisDao.save(cacheKey, sales, CACHE_TTL_HOURS, TimeUnit.HOURS);
        }
    }
}