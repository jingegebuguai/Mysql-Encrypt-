package com.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionMySQL {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionMySQL.class);

    private static final String USERNAME = PropertiesUtil.getProperties("jdbc.username", "root");
    private static final String PASSWORD = PropertiesUtil.getProperties("jdbc.password");
    private static final String DB_URL = PropertiesUtil.getProperties("jdbc.url");
    private static final String DRIVER = PropertiesUtil.getProperties("jdbc.driverClassName");


    public static Connection openConnection() {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LOGGER.error("未发现driver驱动", e);
        }
        try {
            return DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LOGGER.error("数据库连接失败", e);
        }
        return null;
    }
}
