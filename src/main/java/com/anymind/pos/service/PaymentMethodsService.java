package com.anymind.pos.service;

import com.anymind.pos.entity.PaymentMethods;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.cache.annotation.Cacheable;

/**
 * @author Noe
 * @since 2025/04/09 20:59
 */
public interface PaymentMethodsService extends IService<PaymentMethods> {

    // Get payment method by name with cache
    @Cacheable(value = "paymentMethods", key = "#name")
    PaymentMethods getPaymentMethodByName(String name);
}