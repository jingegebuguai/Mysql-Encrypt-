package com.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlDetailData {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlDetailData.class);

    private static final String TABLENAME = PropertiesUtil.getProperties("jdbc.tableName");
    private static final String USERNAME = PropertiesUtil.getProperties("sql.username", "root");
    private static final String PASSWORD = PropertiesUtil.getProperties("sql.password", "");
    private static final String PORT = PropertiesUtil.getProperties("sql.port");
    private static final String HOST = PropertiesUtil.getProperties("sql.host");
    private static final String DATABASENAME = PropertiesUtil.getProperties("sql.databaseName");
    private static final String MYSQLPATH = PropertiesUtil.getProperties("sql.path");
    private static final String EXPORTPATH = PropertiesUtil.getProperties("export.path");


    private static String getExportCommand() throws IOException{

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(MYSQLPATH).append("mysqldump -u").append(USERNAME).append(" -p").append(PASSWORD).append(" -h").append(HOST)
                .append(" -P").append(PORT).append(" ").append(DATABASENAME).append(" -r ").append(EXPORTPATH);
        return stringBuffer.toString();

    }

    public static void export() {

        Runtime runtime = Runtime.getRuntime();
        try {
            String command = getExportCommand();
            runtime.exec(command);
        } catch(IOException e) {
            LOGGER.error("sql语句导出失败", e);
            e.printStackTrace();
        }

    }

    /**
     * 导出sql数据
     */
    public static void exportData() {

        Connection conn = ConnectionMySQL.openConnection();
        StringBuffer command = new StringBuffer();
        command = command.append("SELECT * FROM ").append(TABLENAME).append(" INTO OUTFILE ").append("'").append(EXPORTPATH).append("'");
        String sqlCommand = command.toString();
        try {
            Statement statement = conn.createStatement();
            statement.executeQuery(sqlCommand);
        } catch(SQLException e) {
            LOGGER.error("Statement对象创建失败",e);
            e.printStackTrace();
        }

    }

}
