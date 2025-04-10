package com.anymind.pos.utils;

import com.anymind.pos.config.GlobalExceptionHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {
    public static Date parseDateTime(String dateTimeStr) {
        try {
            // Using ISO 8601 format (e.g., "2022-09-01T00:00:00Z")
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(dateTimeStr);
        } catch (ParseException e) {
            throw new GlobalExceptionHandler.UserNoticeException(
                    "Invalid datetime format: " + dateTimeStr + ". Expected format: yyyy-MM-dd'T'HH:mm:ss'Z'");
        }
    }
}
