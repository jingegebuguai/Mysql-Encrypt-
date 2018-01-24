package com.deparser;

import java.util.List;
import java.util.Map;

import com.core.MetaDataManager;
import com.core.NameHide;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

/**
 * 我把对于select...where部分的解析功能放在了一个单独的类中：SelectExpressionItemSSDB
 * 专门用来处理select...from中间的部分
 *
 *
 */
public class SelectExpressionItemSSDB extends ExpressionDeParser {

    /*
     * 除了AllColumn和AllTableColumn的情况外，selectItem都会被当成一个ExpressionItem来对待，其中的
     * 元素最终由ExpressionDeparser来处理。
     */
    private StringBuilder buffer;
    private Map<String, MetaDataManager> metaOfTable;

    public SelectExpressionItemSSDB(Map<String, MetaDataManager> metaOfTable, StringBuilder buffer) {
        this.metaOfTable = metaOfTable;
        this.buffer = buffer;
    }

    public void visit(Column tableColumn) {
        // TODO Auto-generated method stub
        try {
            // 如果是单表，并且没有提供表名的情况下
            if (metaOfTable.size() == 1 && tableColumn.getTable().getName() == null) {
                String secretName = NameHide.getSecretName(tableColumn.getColumnName());
                // buffer.append(NameHide.getDETName(secretName));
                String detName = NameHide.getDETName(secretName);
                buffer.append(detName);
            } else {
                if ((metaOfTable.size() != 1 && tableColumn.getTable().getName() == null)) {
                    System.out.println("在涉及多个表的操作时，列名需要您提供对应的表名，如employee.salary");
                    System.exit(0);
                } else {
                    // 如果提供了表名，需要将表名一并输出
                    String tableName = tableColumn.getTable().getName();
                    String secretName = NameHide.getSecretName(tableColumn.getColumnName());
                    // buffer.append(NameHide.getDETName(secretName));
                    String detName = NameHide.getDETName(secretName);
                    buffer.append(tableName + "." + detName);
                }

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     * 为了能够在密文上使用加法同态，我们需要对sum和avg函数进行重写 例如：select sum(id) from test where id =
     * 1; 我们需要改写为:SELECT
     * sum(di_HOM1),sum(di_HOM2),sum(di_HOM3),sum(di_HOM4),sum(di_HOM5) FROM
     * test WHERE di_DET = '8uNsSDheptE=';
     * 这包括，将一个sum函数扩展成5个，另外我们还要对原来函数括号中的列名修改为相应的HOM列名。
     */
    public void visit(Function function) {
        // TODO Auto-generated method stub
        try {
            if (function.getName().toLowerCase().equals("sum") || function.getName().toLowerCase().equals("avg")) {
                ExpressionList parameters = function.getParameters();
                List<Expression> listExp = parameters.getExpressions();
                /*
                 * 在我们的程序中，我们假设SQL语句中的有sum(id)和avg(id)的函数，对此我们需要获取其中的列名，
                 * 进行改写：sum(di_HOM1)...sum(di_HOM5) 如果是多表查询，我们需要判断
                 */
                Column columnPara = (Column) listExp.get(0);
                /*
                 * 在解析函数的时候，我们只需要考虑如果多表却没有提供表名的情况，这个时候输出提示信息并退出程序 其他情况我们不需要特殊处理
                 */
                if ((metaOfTable.size() != 1 && columnPara.getTable().getName() == null)) {
                    System.out.println("在涉及多个表的操作时，sum()和avg()函数中需要您提供表名，如sum(employee.salary)");
                    System.exit(0);
                }
                String secretColumnName = NameHide.getHOMName(NameHide.getSecretName(columnPara.getColumnName()));
                for (int index_fun = 0; index_fun < 5; index_fun++) {
                    Function felement = new Function();
                    // 设置当前的function元素的函数名字
                    felement.setName(function.getName());
                    // 将列名改成di_HOM1、di_HOME2...
                    columnPara.setColumnName(secretColumnName + (index_fun + 1));
                    listExp.set(0, columnPara);
                    parameters.setExpressions(listExp);
                    // 将function元素中的parameter部分，也就是sum()的括号中的部分，设置为我们修改后的值：如di_HOM1
                    felement.setParameters(parameters);
                    // 将当前的function元素添加到List<function>中,注意5个sum函数，需要添加4个逗号。
                    if (index_fun == 4) {
                        buffer.append(felement);
                    } else {
                        buffer.append(felement).append(",");
                    }
                }
            } else {
                super.visit(function);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}