package com.anymind.pos.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Noe
 * @since 2025/04/09 20:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("courier_payment_map")
public class CourierPaymentMap implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String courier;

    private Integer paymentMethodId;

    private Date createdAt;
}