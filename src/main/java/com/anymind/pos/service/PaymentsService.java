package com.anymind.pos.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.anymind.pos.dto.HourlySaleSummary;
import com.anymind.pos.entity.Payments;

import java.util.Date;
import java.util.List;

/**
 * @author Noe
 * @since 2025/04/09 20:59
 */
public interface PaymentsService extends IService<Payments> {


    List<HourlySaleSummary> getSales(Date start, Date end);
}