package com.anymind.pos.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.anymind.pos.config.handler.MapHandler;
import com.anymind.pos.utils.DateUtil;
import com.anymind.pos.validator.PaymentReq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * @author Noe
 * @since 2025/04/09 20:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "payments", autoResultMap = true)
public class Payments implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private String id;

    private String customerId;

    private Integer paymentMethodId;

    private BigDecimal originalPrice;

    private BigDecimal priceModifier;

    private Date datetime;

    private BigDecimal finalPrice;

    private BigDecimal points;

    @TableField(typeHandler = MapHandler.class)
    private Map<String, String> info;

    private Date createdAt;


    public static Payments build(PaymentReq req, PaymentMethods paymentMethod) {
        return Payments.builder()
                .id(UUID.randomUUID().toString())
                .customerId(req.getCustomerId())
                .originalPrice(req.getPrice())
                .priceModifier(req.getPriceModifier())
                .paymentMethodId(paymentMethod.getId())
                .finalPrice(calcFinalPrice(req.getPrice(), req.getPriceModifier()))
                .points(calcPoints(req.getPrice(), paymentMethod.getPointRate()))
                .datetime(DateUtil.parseDateTime(req.getDatetime()))
                .info(req.getAdditionalItem())
                .createdAt(new Date())
                .build();
    }

    public static BigDecimal calcFinalPrice(BigDecimal price, BigDecimal priceModifier) {
        return price.multiply(priceModifier).setScale(2, RoundingMode.DOWN);
    }
    public static BigDecimal calcPoints(BigDecimal price, BigDecimal points) {
        return price.multiply(points).setScale(0, RoundingMode.DOWN);
    }

}