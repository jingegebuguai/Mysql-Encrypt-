package com.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;


public class SqlFieldData {

    private static  final Logger LOGGER = LoggerFactory.getLogger(SqlFieldData.class);

    private static final String TABLENAME = PropertiesUtil.getProperties("jdbc.tableName");

    /**
     * 获取表单列数据
     * @return
     */
    public static Map<String, String> getTableInfo() {

        Map<String,String> columnNameMap = new LinkedHashMap<String, String>();

        Connection conn = ConnectionMySQL.openConnection();
        String sql = "SELECT * FROM " + TABLENAME;
        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(sql);
            ResultSet resutlSet = statement.executeQuery();
            ResultSetMetaData data = resutlSet.getMetaData();
            while(resutlSet.next()) {
                for(int i = 1; i<=data.getColumnCount(); i++) {
//                    int columnCount = data.getColumnCount();
                    String columnName = data.getColumnName(i);
//                    String columnValue = resutlSet.getString(i);
//                    int columnType = data.getColumnType(i);
                    String columnTypeName = data.getColumnTypeName(i);
                    columnNameMap.put(columnName, columnTypeName);
                }
                break;
            }
        } catch (SQLException e) {
            LOGGER.error("sql语句查询失败",e);
            e.printStackTrace();
        }
        return columnNameMap;
    }



}
