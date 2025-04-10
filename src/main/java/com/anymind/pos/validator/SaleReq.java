package com.anymind.pos.validator;

import com.anymind.pos.config.GlobalExceptionHandler;
import com.anymind.pos.grpc.SalesRequest;
import com.anymind.pos.utils.DateUtil;
import lombok.Data;

import java.util.Date;

@Data
public class SaleReq {
    private Date startDate;
    private Date endDate;


    public SaleReq(SalesRequest req) {
        try {
            startDate = DateUtil.parseDateTime(req.getStartDateTime());
            endDate = DateUtil.parseDateTime(req.getEndDateTime());
        } catch (GlobalExceptionHandler.UserNoticeException e) {
            throw e;
        }

        if (endDate.before(startDate)) {
            throw new GlobalExceptionHandler.UserNoticeException("End date must be after start date");
        }

    }
}
