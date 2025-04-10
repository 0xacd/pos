package com.anymind.pos.config.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.*;

import java.sql.*;
import java.util.*;

@Slf4j
@MappedTypes(Map.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class MapHandler implements TypeHandler<Map<String, String>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setParameter(PreparedStatement ps, int i, Map<String, String> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            try {
                ps.setString(i, objectMapper.writeValueAsString(parameter));
            } catch (Exception e) {
                throw new SQLException("Error converting Map<String, String> to String.", e);
            }
        }
    }

    @Override
    public Map<String, String> getResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return parseMap(value);
    }

    @Override
    public Map<String, String> getResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return parseMap(value);
    }

    @Override
    public Map<String, String> getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return parseMap(value);
    }

    private Map<String, String> parseMap(String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new SQLException("Error converting String to Map<String, String>.", e);
        }
    }
}
