package com.anymind.pos.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Noe
 * @since 2025/04/09 20:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment_methods")
public class PaymentMethods implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private BigDecimal minModifier;

    private BigDecimal maxModifier;

    private BigDecimal pointRate;

    private Boolean requiresLast4;

    private Boolean requiresBankInfo;

    private Boolean requiresChequeInfo;

    private Boolean requiresCourier;

    private Date createdAt;

    private Date updatedAt;
}