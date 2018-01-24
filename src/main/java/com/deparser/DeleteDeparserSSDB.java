package com.deparser;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;


import com.core.MetaDataManager;
import com.core.RNDOnion;
import com.demo.ClientDemo;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.statement.delete.Delete;

public class DeleteDeparserSSDB {

    public DeleteDeparserSSDB() {
    }

    public String deleteReconstruct(Delete delete, Map<String, MetaDataManager> metaOfTable)
            throws JSQLParserException {
        MetaDataManager metaManager = metaOfTable.values().iterator().next();
        String tableName = delete.getTable().getName();
        metaManager.fetchMetaData(tableName);
        StringBuilder buffer = new StringBuilder();
        // 我们把where子句的解析统一交给WhereExpressionDeparser类完成。
        WhereExpressionSSDB whereExpressionSSDB = new WhereExpressionSSDB(metaOfTable, null, buffer);
        if (delete.getWhere() != null) {
            delete.getWhere().accept(whereExpressionSSDB);
        }
        return delete.toString() + ";";
    }

    public static void handler(Delete delete, Connection conn) {
        try {
            Statement smt = conn.createStatement();
            DeleteDeparserSSDB deleteRec = new DeleteDeparserSSDB();
            String tableName = delete.getTable().getName();
            Map<String, MetaDataManager> metaOfTable = new HashMap<String, MetaDataManager>();
            MetaDataManager metaManager = new MetaDataManager();
            metaManager.fetchMetaData(tableName);
            metaOfTable.put(tableName, metaManager);
            String outputSQL = deleteRec.deleteReconstruct(delete, metaOfTable);
            // 如果当前存在where语句，就必须先将where语句中涉及到的DET型列剥去RND层
            if (ClientDemo.encColumnNameList.size() > 0) {
                // 1.如果当前where子句中需要匹配DET列，则先剥去RND层，再删除
                String peelOff = RNDOnion.peelOffRND(tableName, ClientDemo.encColumnNameList, "123456");
                smt.executeUpdate(peelOff);

                // 2.下面将处理后的密文SQL语句提交给数据库执行，但是在此之前我们要剥去RND层
                smt.executeUpdate(outputSQL);


                /*
                 * 3.比如我们的语句是delete from peach where id = 3;
                 * 我们把id=3的那条记录删除了，但是其他记录中的id列处在DET层，我们必须将id_DET重新加密为RND
                 */


                String packOn = RNDOnion.packOnRND(tableName, ClientDemo.encColumnNameList, "123456");
                smt.executeUpdate(packOn);
                smt.close();
            } else {

                /*
                 * 如果当前没有where子句，则不需要剥去RND层。直接删除即可
                 */
                // 1. 执行delete语句

                smt.executeUpdate(outputSQL);
                smt.close();

            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSQLParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
