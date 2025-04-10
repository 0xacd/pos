package com.anymind.pos.service;

import com.anymind.pos.entity.CourierPaymentMap;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * @author Noe
 * @since 2025/04/09 20:59
 */
public interface CourierPaymentMapService extends IService<CourierPaymentMap> {

    // Get couriers by payment method ID with cache
    @Cacheable(value = "courierPaymentMap", key = "#paymentMethodId")
    List<String> getCouriersByPaymentMethodId(Integer paymentMethodId);
}