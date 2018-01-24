package com.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SqlFieldData {

    private static  final Logger LOGGER = LoggerFactory.getLogger(SqlFieldData.class);

    private static final String TABLENAME = PropertiesUtil.getProperties("jdbc.tableName");

    /**
     * 获取表单列数据
     * @return
     */
    public static List getTableInfo() {
        List result = new ArrayList();

        Connection conn = null;
        DatabaseMetaData dbmd = null;

        try {
            conn = ConnectionMySQL.openConnection();

            dbmd = conn.getMetaData();
            ResultSet resultSet = dbmd.getTables(null, "%", TABLENAME, new String[] { "TABLE" });

            while (resultSet.next()) {
                String tableName=resultSet.getString("TABLE_NAME");
                System.out.println(tableName);

                if(tableName.equals(TABLENAME)){
                    ResultSet rs = conn.getMetaData().getColumns(null, getSchema(conn),tableName.toUpperCase(), "%");

                    while(rs.next()){
                        Map map = new HashMap();
                        String colName = rs.getString("COLUMN_NAME");
                        map.put("code", colName);

                        String remarks = rs.getString("REMARKS");
                        if(remarks == null || remarks.equals("")){
                            remarks = colName;
                        }
                        map.put("name",remarks);

                        String dbType = rs.getString("TYPE_NAME");
                        map.put("dbType",dbType);

                        //map.put("valueType", changeDbType(dbType));
                        result.add(map);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("sql发生错误", e);
            e.printStackTrace();
        } catch (Exception e) {
            LOGGER.error("其他异常错误", e);
            e.printStackTrace();
        }finally{
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.error("数据库连接关闭异常", e);
                e.printStackTrace();
            }
        }
        return result;

    }

    /**
     * oracle和db2的getSchema方法
     * @param conn
     * @return
     * @throws Exception
     */
    private static String getSchema(Connection conn) throws Exception {

        String schema;
        schema = conn.getMetaData().getUserName();
        if ((schema == null) || (schema.length() == 0)) {
            throw new Exception("ORACLE数据库模式不允许为空");
        }
        return schema.toUpperCase().toString();

    }
    /


}
