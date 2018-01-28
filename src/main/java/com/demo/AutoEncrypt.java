package com.demo;

import com.common.ConnectionMySQL;
import com.common.PropertiesUtil;
import com.common.ReadFile;
import com.common.SqlFieldData;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.insert.Insert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AutoEncrypt {

    public static final Logger LOGGER = LoggerFactory.getLogger(AutoEncrypt.class);

    private static final String TABLENAME = PropertiesUtil.getProperties("jdbc.tableName");


    /**
     * 获取插入的sql语句
     *
     * @return
     */
    public static List<String> getInsertSql() throws SQLException {

        StringBuffer sqlString = new StringBuffer();
        StringBuffer columnNameString = new StringBuffer();
        String columnNameStr = "";
        Map<String, String> columnName = SqlFieldData.getTableInfo();
        Map<Integer, List<String>> columnData = ReadFile.getFileData();

        //存放sql语句
        List<String> insertSql = new LinkedList();

        Iterator<Map.Entry<Integer, List<String>>> dataEntries = columnData.entrySet().iterator();

        //遍历获取sql语句
        while (dataEntries.hasNext()) {

            Map.Entry<Integer, List<String>> entry = dataEntries.next();
            List<String> list = entry.getValue();
            StringBuffer dataValue = new StringBuffer();
            String dataStr = "";
            Iterator<String> iterator = list.iterator();
            Iterator<Map.Entry<String, String>> nameEntries = columnName.entrySet().iterator();

            while (iterator.hasNext() && nameEntries.hasNext()) {

                String str = iterator.next();
                Map.Entry<String, String> entries = nameEntries.next();

                columnNameString = columnNameString.append(entries.getKey()).append(",");
                String columnType = entries.getValue();
                if (columnType.equals("INT") || columnType.equals("DOUBLE") || columnType.equals("FLOAT")) {
                    dataValue = dataValue.append(str).append(",");
                } else if (columnType.equals("CHAR") || columnType.equals("VARCHAR") || columnType.equals("TEXT")) {
                    dataValue = dataValue.append("'").append(str).append("'").append(",");
                } else {
                    LOGGER.warn("当前数据类型无法进行加密");
                    return null;
                }
            }

            columnNameStr = columnNameString.toString();
            columnNameStr = columnNameStr.substring(0, columnNameStr.length() - 1);

            dataStr = dataValue.toString();
            dataStr = dataStr.substring(0, dataStr.length() - 1);
            sqlString = sqlString.append("INSERT INTO ").append("SSDB").append(TABLENAME).append("(").append(columnNameStr).append(") ").append("values")
                    .append("(").append(dataStr).append(")");
            String sql = sqlString.toString();
            insertSql.add(sql);
        }
        return insertSql;
    }


    /**
     * 获取创建表sql语句
     *
     * @return
     */
    public static String getCreateSql() throws SQLException {

        String newTableName = "SSDB" + TABLENAME;
        String sqlString = "";

        try {
            if (isExistTable(newTableName) == true) {
                Connection conn = ConnectionMySQL.openConnection();
                String dropSql = "DROP TABLE " + newTableName;
                PreparedStatement stmt = conn.prepareStatement(dropSql);
                stmt.executeUpdate();
                conn.close();
                sqlString = getCreateString();
                return sqlString;
            } else {
                sqlString = getCreateString();
                return sqlString;
            }
        } catch(SQLException e) {
            LOGGER.error("SQL异常错误", e);
            e.printStackTrace();
        }
        return sqlString;
    }

    /**
     * 获取创建表sql字符串
     * @return
     */
    private static String getCreateString() {

        Map<String, String> columnField = SqlFieldData.getTableInfo();
        StringBuffer createBuffer = new StringBuffer();
        String string = "";
        String newTableName = "SSDB" + TABLENAME;

        createBuffer = createBuffer.append("CREATE TABLE ").append(newTableName).append("(");
        Iterator<Map.Entry<String, String>> entryIterator = columnField.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<String, String> entries = entryIterator.next();
            String columnName = entries.getKey();
            String columnType = entries.getValue();
            createBuffer = createBuffer.append(columnName).append(" ").append(columnType).append(",");
        }
        string = createBuffer.toString();
        String createStr = string.substring(0, string.length() - 1) + ")";
        return createStr;

    }

    /**
     * 判断表是否存在
     * @param tableName
     * @return boolean
     */
    public static boolean isExistTable(String tableName) throws SQLException {
        boolean flag = false;
        String sql = "SELECT COUNT(*) FROM " + tableName;
        Connection conn = ConnectionMySQL.openConnection();
        try{
            PreparedStatement statement = conn.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            flag = true;
        } catch (SQLException e) {
            return flag;
        } finally {
            conn.close();
        }
        return flag;
    }

    /**
     * 自动生成加密表
     */
    private static void autoEncrypt() {

        Connection conn = ConnectionMySQL.openConnection();

        try {
            String createSql = getCreateSql();
            try {
                CCJSqlParserManager parserManager = new CCJSqlParserManager();
                Statement statement = parserManager.parse(new StringReader(createSql));
                if (statement instanceof CreateTable) {
                    ClientDemo.createTable(statement, conn);
                }
            } catch (JSQLParserException e) {
                LOGGER.error("数据表创建异常", e);
            }
        } catch(SQLException e) {
            LOGGER.warn("警告你别装逼", e);
        }

        try {
            List<String> insertSql = getInsertSql();
            Iterator<String> iterator = insertSql.iterator();
            while(iterator.hasNext()) {
                String insertStr = iterator.next();
                CCJSqlParserManager parserManager = new CCJSqlParserManager();
                Statement statement = parserManager.parse(new StringReader(insertStr));
                if(statement instanceof Insert) {
                    ClientDemo.insertTable(statement, conn);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("插入语句获取失败", e);
            e.printStackTrace();
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args) throws SQLException {
        //System.out.println(getInsertSql());
        autoEncrypt();
    }


}
