package com.demo;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


import com.common.ConnectionMySQL;
import com.deparser.*;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDemo.class);

    /**
     * 这个变量用于收集语句中的DET类型的列名，用于处理RND层时使用 因为我们只对DET类型的列增加RND层，而在一条语句改写后含有
     */
    public static List<String> tableNameList = new ArrayList<String>();
    public static List<String> encColumnNameList = new ArrayList<String>();

    public static void createTable(Statement statement, Connection conn) {
        long start = System.currentTimeMillis();
        CreateTable createTable = (CreateTable) statement;
        CreateTableDeparserSSDB.handler(createTable, conn);
        long end = System.currentTimeMillis();
        System.out.println("CreateTime:" + (end - start));
    }

    public static void insertTable(Statement statement, Connection conn) {
        long startInsert = System.currentTimeMillis();
        Insert insert = (Insert) statement;
        InsertDeparserSSDB.handler(insert, conn);
        long endInsert = System.currentTimeMillis();
        System.out.println("InsertTime:" + (endInsert - startInsert));
    }

    public static void selectTable(Statement statement, Connection conn) {
        long startSelect = System.currentTimeMillis();
        Select select = (Select) statement;
        SelectSQLRewrite.handler(select, conn);
        long endSelect = System.currentTimeMillis();
        System.out.println("SelectTime:" + (endSelect - startSelect));
    }

    public static void deleteTable(Statement statement, Connection conn) {
        long startDelete = System.currentTimeMillis();
        Delete delete = (Delete) statement;
        DeleteDeparserSSDB.handler(delete, conn);
        long endDelete = System.currentTimeMillis();
        System.out.println("DeleteTime:" + (endDelete - startDelete));
    }

    public static void updateTable(Statement statement, Connection conn) {
        long startUpdate = System.currentTimeMillis();
        Update update = (Update) statement;
        UpdateDeparserSSDB.handler(update, conn);
        long endUpdate = System.currentTimeMillis();
        System.out.println("UpdateTime:" + (endUpdate - startUpdate));
    }

    public static void main(String[] args) throws InterruptedException, SQLException {
        // TODO Auto-generated method stub
        try {
            Connection conn = ConnectionMySQL.openConnection();
            Scanner sc = new Scanner(System.in);
            System.out.println("请输入SQL语句：");
            String inputSQL = sc.nextLine();
            while (true) {
                try {
                    CCJSqlParserManager parserManager = new CCJSqlParserManager();
                    Statement statement = parserManager.parse(new StringReader(inputSQL));
                    if (statement instanceof CreateTable) {
                        createTable(statement, conn);
                    } else {
                        if (statement instanceof Insert) {
                            insertTable(statement, conn);
                        } else {
                            if (statement instanceof Select) {
                                selectTable(statement, conn);
                            } else {
                                if (statement instanceof Delete) {
                                    deleteTable(statement, conn);
                                } else {
                                    if (statement instanceof Update) {
                                       updateTable(statement, conn);
                                    } else {
                                        System.out.println("不支持的语句类型");
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    // 在执行完一条语句后，需要将静态列表清空
                    tableNameList.clear();
                    encColumnNameList.clear();
                    System.out.println("是否继续使用?如需继续使用请直接输入下一条语句,否则输入exit：");
                    Scanner endSc = new Scanner(System.in);
                    String endStr = endSc.nextLine();
                    if (endStr.contains("exit")) {
                        endSc.close();
                        break;
                    } else {
                        inputSQL = endStr;
                        continue;
                    }

                } catch (JSQLParserException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            System.out.println("------------欢迎下次使用，再见！--------------");
            conn.close();
            sc.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
