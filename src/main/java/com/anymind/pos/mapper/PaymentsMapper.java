package com.anymind.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.anymind.pos.entity.Payments;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * @author Noe
 * @since 2025/04/09 20:59
 */
@Mapper
public interface PaymentsMapper extends BaseMapper<Payments> {

    @Select("SELECT * FROM payments WHERE datetime BETWEEN #{start} AND #{end} ORDER BY datetime")
    List<Payments> selectByDateRange(Date start, Date end);

}