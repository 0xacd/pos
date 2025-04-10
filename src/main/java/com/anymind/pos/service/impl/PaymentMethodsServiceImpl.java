package com.anymind.pos.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anymind.pos.entity.PaymentMethods;
import com.anymind.pos.mapper.PaymentMethodsMapper;
import com.anymind.pos.service.PaymentMethodsService;
import jakarta.annotation.PostConstruct;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Noe
 * @since 2025/04/09 20:59
 */
@Service
public class PaymentMethodsServiceImpl extends ServiceImpl<PaymentMethodsMapper, PaymentMethods> implements PaymentMethodsService {


    // In-memory cache for quick access
    private final Map<String, PaymentMethods> paymentMethodsCache = new ConcurrentHashMap<>();

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
    @CacheEvict(value = "paymentMethods", allEntries = true)
    public void refreshCache() {
        List<PaymentMethods> methods = this.list(new LambdaQueryWrapper<>());
        Map<String, PaymentMethods> newCache = methods.stream()
                .collect(Collectors.toMap(PaymentMethods::getName, method -> method));
        paymentMethodsCache.clear();
        paymentMethodsCache.putAll(newCache);
    }

    // Get payment method by name with cache
    @Cacheable(value = "paymentMethods", key = "#name")
    @Override
    public PaymentMethods getPaymentMethodByName(String name) {
        // First check in-memory cache
        PaymentMethods method = paymentMethodsCache.get(name);
        if (method == null) {
            // If not in cache, query DB and update cache
            method = this.getOne(new LambdaQueryWrapper<PaymentMethods>().eq(PaymentMethods::getName, name));
            if (method != null) {
                paymentMethodsCache.put(name, method);
            }
        }
        return method;
    }

}