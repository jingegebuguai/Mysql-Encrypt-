package com.deparser;

import java.io.StringReader;
import java.security.Key;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.core.*;
import com.demo.ClientDemo;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

/**
 * 这个类用于处理查询类型的SQL语句，单独定义这个类的原因是，select语句与其他DML语句不同之处在于，
 * 它有返回值，因此还需要处理返回的密文，将其解密成明文的形式，
 * 其中的语句改写使用到SelectDeparserSSDB和SelectExpressionItemSSDB
 *
 */

public class SelectSQLRewrite {
    public void print(Map<String, MetaDataManager> metaOfTable, ResultSet rs, List<SelectItem> plainItemList)
            throws Exception {
        Map<String, Key> detKeyMap = new HashMap<String, Key>();
        Map<String, double[][]> homKeyMap = new HashMap<String, double[][]>();
        int size = plainItemList.size();

        for (int index_list = 0; index_list < size; index_list++) {
            Expression item = ((SelectExpressionItem) plainItemList.get(index_list)).getExpression();
            /*
             * 判断selectItem是函数：sum()、avg()，还是普通的列
             */
            if (item instanceof Function) {
                Function functionItem = (Function) item;
                // 我们默认sum()或者avg()内部只有一个参数
                Column columnOfFunction = (Column) functionItem.getParameters().getExpressions().get(0);
                String columnName = columnOfFunction.getColumnName();
                double[][] homKey = new double[5][3];
                /*
                 * 这里的判断条件为： 1.当前为单表查询：metaOfTable.size() == 1，并且没有使用
                 * "select 表名.列名 from ..."; 情况1：允许 2.当前为多表查询：metaOfTable.size()
                 * != 1，并且没有使用 "select 表名.列名 from ..."; 情况2：不允许
                 * 3.当前为单表查询，且使用用了select 表名.列名 from
                 * ...，或者多表查询，且使用了"select 表名.列名 from ..." 情况3：允许
                 *
                 * 以上的情况可以概述为：多表查询时必须在列名前指定相应的表名：表名.列名
                 */
                if (metaOfTable.size() == 1 && columnOfFunction.getTable().getName() == null) {
                    homKey = metaOfTable.values().iterator().next().getHomKey(columnName);
                    homKeyMap.put(columnName, homKey);
                } else {
                    if (metaOfTable.size() != 1 && columnOfFunction.getTable().getName() == null) {
                        System.out.println("在涉及多个表的操作时，sum()和avg()函数中需要您提供表名，如sum(employee.salary)");
                        System.exit(0);
                    } else {
                        // 这个语句执行的条件是列名前指定了表名:tableName.columnName
                        String tableName = columnOfFunction.getTable().getName();
                        /*
                         * 如果查询语句中多个列中存在相同的列名，怎么办 比如:select
                         * sum(employee.salary),avg(manager.salary) ...
                         * 如果只是按照列名的方式存储hom密钥，必然出现覆盖的现象。
                         * 所以对于多表操作，必须将homKeyMap的键设置为（表名+列名） 单表操作可以不这么做（第一个if语句）
                         */
                        homKey = metaOfTable.get(tableName).getHomKey(columnName);
                        homKeyMap.put(tableName + columnName, homKey);
                    }
                }

            } else {
                /*
                 * 如果当前的SelectItem是普通的列，因为不需要向元数据表中查找密钥，只要通过列名生成det密钥即可.
                 * 我们需要考虑这样一种情况，如果是多个表，存在列名相同的情况，必须指定对应的表名，如select
                 * employee.id，manager.id...
                 * 同时这里获取密钥的时候，也需要将键设置为tableName+columnName
                 * 对于单表，键只需要设置为columnName即可
                 */
                Column columnItem = (Column) item;
                String columnName = columnItem.getColumnName();
                Key key = KeyManager.generateDETKey("123456", columnName, "det");
                if (metaOfTable.size() == 1 && columnItem.getTable().getName() == null) {
                    detKeyMap.put(columnName, key);
                } else {
                    if (metaOfTable.size() != 1 && columnItem.getTable().getName() == null) {
                        System.out.println("在涉及多个表的操作时，列名需要您提供对应的表名，如sum(employee.salary)");
                        System.exit(0);
                    } else {
                        // 这个语句执行的条件是列名前指定了表名:tableName.columnName
                        String tableName = columnItem.getTable().getName();
                        detKeyMap.put(tableName + columnName, key);
                    }
                }
            }
        }

        /*
         * 我们以select sum(grade),sum(age) from grade为例
         * rs中的数据为sum(grade_HOM1),sum(grade_HOM2),sum(grade_HOM3),sum(grade_HOM4
         * ),sum(grade_HOM5),
         * sum(age_HOM1),sum(age_HOM2),sum(age_HOM3),sum(age_HOM4),sum(age_HOM5)
         * 当我们想用rs.getDouble来分别获取grade和age的sum结果时，必须要指定字段的名字，
         * 可是我们怎么确定获取的是grade_HOM1,还是age_HOM1呢？
         * 这里的解决方法是，根据明文的selectItem信息，先将列名改写为我们需要的形式，再从rs中获取对应的值
         */
        while (rs.next()) {
            for (int index_list = 0; index_list < size; index_list++) {
                // 如果当前的selectItem是一个函数的话(SUM或者AVG)
                Expression item = ((SelectExpressionItem) plainItemList.get(index_list)).getExpression();
                if (item instanceof Function) {
                    Function functionItem = (Function) item;
                    // 我们默认sum()或者avg()内部只有一个参数
                    Column columnOfFunction = (Column) functionItem.getParameters().getExpressions().get(0);
                    String columnName = columnOfFunction.getColumnName();
                    String secretColumnName = NameHide.getHOMName(NameHide.getSecretName(columnName));
                    double[] secretShare = new double[5];
                    // 这里我们需要判断表名
                    /*
                     * 这里我们不在判断是多表还是单表，因为之前的for语句中已经做了这样的工作，如果不满足执行条件，
                     * 之前就会退出程序，既然能执行到这里，必然是满足执行条件的，我们只需要判断是不是含有表名就可以了。
                     * 现在只有两种情况，一种是带表名，一种是不带表名\
                     * 对于不带表名的，我们通过列名的HOM形式查找即可，并且homKeyMap的键也将是列名
                     * 对于带表名的，我们需要表名+列名的形式进行查找以及获密钥
                     */
                    if (columnOfFunction.getTable().getName() == null) {
                        for (int hom_index = 0; hom_index < 5; hom_index++) {
                            secretShare[hom_index] = rs
                                    .getDouble(functionItem.getName() + "(" + secretColumnName + (hom_index + 1) + ")");
                        }
                        AddHomAlgorithm homAlg = new AddHomAlgorithm(homKeyMap.get(columnName), 5);
                        double result = homAlg.decrypt(secretShare);
                        System.out.print(result + "\t");
                    } else {
                        if (columnOfFunction.getTable().getName() != null) {
                            String tableName = columnOfFunction.getTable().getName();
                            for (int hom_index = 0; hom_index < 5; hom_index++) {
                                secretShare[hom_index] = rs.getDouble(functionItem.getName() + "(" + tableName + "."
                                        + secretColumnName + (hom_index + 1) + ")");
                            }
                            AddHomAlgorithm homAlg = new AddHomAlgorithm(homKeyMap.get(tableName + columnName), 5);
                            double result = homAlg.decrypt(secretShare);
                            System.out.print(result + "\t");
                        } else {
                            System.out.println("查询出错：SelectSQLRewrite.print函数");
                        }
                    }

                } else {
                    Column columnItem = (Column) item;
                    String columnName = columnItem.getColumnName();
                    String detColumnName = NameHide.getDETName(NameHide.getSecretName(columnName));
                    if (metaOfTable.size() == 1 && columnItem.getTable().getName() == null) {
                        byte[] resultDET = DETAlgorithm.decrypt(rs.getString(detColumnName), detKeyMap.get(columnName));
                        System.out.print(new String(resultDET) + "\t");
                    } else {
                        if (metaOfTable.size() != 1 && columnItem.getTable().getName() == null) {
                            System.out.println("在涉及多个表的操作时，列名需要您提供对应的表名，如sum(employee.salary)");
                            System.exit(0);
                        } else {
                            // 这个语句执行的条件是列名前指定了表名:tableName.columnName
                            String tableName = columnItem.getTable().getName();
                            byte[] resultDET = DETAlgorithm.decrypt(rs.getString(detColumnName),
                                    detKeyMap.get(tableName + columnName));
                            System.out.print(new String(resultDET) + "\t");
                        }
                    }

                }
            }
            System.out.print("\n");
        }
    }

    public String selectReconstruct(Select select, Map<String, MetaDataManager> metaOfTable)
            throws JSQLParserException {

        // buffer是用来重构SQL的临时字符串。
        StringBuilder buffer = new StringBuilder();
        /*
         * 这条语句就是对SQL语句进行解析，将语句中的各个部分解析成为一个个对象 Select --> SelectBody -->
         * PlainSelect
         */
        // 以下的两行是将select中的SelectItem（就是select和from之间的部分），也就是要查询的那些列提取到list中，可以通过getItemsList()获取这个list
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        SelectDeparserSSDB selectDeparser = new SelectDeparserSSDB(metaOfTable);
        ExpressionDeParser expressionDeparser = new ExpressionDeParser(selectDeparser, buffer);
        selectDeparser.setBuffer(buffer);
        selectDeparser.setExpressionVisitor(expressionDeparser);
        plainSelect.accept(selectDeparser);
        return buffer.toString() + ";";

    }

    public static void handler(Select select, Connection conn) {
        try {
            // TODO Auto-generated method stub
            Statement smt = conn.createStatement();
            SelectSQLRewrite selectDep = new SelectSQLRewrite();
            CCJSqlParserManager parserManager = new CCJSqlParserManager();
            Select orgSelect = (Select) parserManager.parse(new StringReader(select.toString()));
            /*
             * 以下的两行是将select中的SelectItem（就是select和from之间的部分），也就是要查询的那些列提取到list中，
             * 可以通过getItemsList()获取这个list 我们获取明文的SelectItem的目的是为了在print函数中使用。
             */
            PlainSelect plainSelectOrg = (PlainSelect) orgSelect.getSelectBody();
            List<SelectItem> plainItemList = plainSelectOrg.getSelectItems();

            /*
             * 我们自定义一个fromitem的解析器，作用是获取from...中的所有表名，用于构建metaOfTable
             * 不过在完成这项功能后，需要清空ClientDemo.tableNameList，因为在正式的select语句的解析的过程中
             * 还要用到这个列表，不清空会造成列表中的数据重复。
             * 但是还会存在一个隐患，如果where子句中还有select嵌套语句，那么只从fromitem中获取表名就不够了
             * 所以当前我们默认where子句中不存在嵌套select语句
             */
            FromItem fromItem = plainSelectOrg.getFromItem();
            FromItemDeparserSSDB fromItemDeparserSSDB = new FromItemDeparserSSDB();
            fromItem.accept(fromItemDeparserSSDB);
            if (plainSelectOrg.getJoins() != null) {
                if (plainSelectOrg.getJoins().size() == 1) {
                    String joinTableName = ((Table) plainSelectOrg.getJoins().get(0).getRightItem()).getName();
                    ClientDemo.tableNameList.add(joinTableName);
                } else {
                    System.out.println("join语句的右表达式有问题,或者语句中存在超过一个的join操作");
                    System.exit(0);
                }
            }
            // 考虑到语句中涉及的表不止一个，我们需要构建一个Map集合<表名，表名对应的元数据>
            Map<String, MetaDataManager> metaOfTable = new HashMap<String, MetaDataManager>();

            for (int tableIndex = 0; tableIndex < ClientDemo.tableNameList.size(); tableIndex++) {
                MetaDataManager metaManager = new MetaDataManager();
                metaManager.fetchMetaData(ClientDemo.tableNameList.get(tableIndex));
                metaOfTable.put(ClientDemo.tableNameList.get(tableIndex), metaManager);
            }

            // 清空列表
            ClientDemo.tableNameList.clear();
            // 开始正式select语句解析
            String encSQL = selectDep.selectReconstruct(select, metaOfTable);

            for (int tableIndex = 0; tableIndex < ClientDemo.tableNameList.size(); tableIndex++) {
                // 如果当前需要查询DET列，则先剥去RND层，再查询，最后再包上RND层
                String tableName = ClientDemo.tableNameList.get(tableIndex);
                String peelOff = RNDOnion.peelOffRND(tableName, metaOfTable.get(tableName).getAllDETColumnName(),
                        "123456");
                smt.executeUpdate(peelOff);
            }
            ResultSet rs = null;
            try {
                // 下面将处理后的密文SQL语句提交给数据库执行，但是在此之前我们要剥去RND层
                rs = smt.executeQuery(encSQL);
                System.out.println("查询结果为：");
                selectDep.print(metaOfTable, rs, plainItemList);
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return;
            }
            // 完成查询功能后，用下面这条语句包上RND层
            for (int tableIndex = 0; tableIndex < ClientDemo.tableNameList.size(); tableIndex++) {
                // 如果当前需要查询DET列，则先剥去RND层，再查询，最后再包上RND层
                String tableName = ClientDemo.tableNameList.get(tableIndex);
                String packOn = RNDOnion.packOnRND(tableName, metaOfTable.get(tableName).getAllDETColumnName(),
                        "123456");
                smt.executeUpdate(packOn);
            }
            smt.close();
            rs.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
