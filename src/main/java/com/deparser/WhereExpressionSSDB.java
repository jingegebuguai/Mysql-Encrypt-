package com.deparser;

import java.security.Key;
import java.util.Map;


import com.core.*;
import com.demo.ClientDemo;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

/**
 * 继承自Expression类，并重写了其中的方法。这个类是用来处理语句中expression部分的,专门用于处理where子句的解析。
 * 构造函数中的buffer和上下文的buffer是同步的，它们都指向同一块内存空间。
 */

public class WhereExpressionSSDB extends ExpressionDeParser {
    Map<String, MetaDataManager> metaOfTable;

    /**
     *
     * @param metaOfTable
     *            通过构造函数获取密钥
     * @param selectVisitor
     *            设置在where子句中出现select嵌套语句时，应该交给谁来处理
     * @param buffer
     *            输出字符串
     */
    public WhereExpressionSSDB(Map<String, MetaDataManager> metaOfTable, SelectVisitor selectVisitor,
                               StringBuilder buffer) {
        // 调用子类的构造函数前，必须先调用父类的构造函数
        super(selectVisitor, buffer);
        this.metaOfTable = metaOfTable;

    }

    /**
     * 这个函数用于处理where语句中 诸如" a > 5"的情况
     *
     * @param greaterThan
     *            大于表达式
     */
    @Override
    public void visit(GreaterThan greaterThan) {
        Column c = (Column) greaterThan.getLeftExpression();
        String columnName = c.getColumnName();
        // 根据tableName.columnName来查找密钥
        /*
         * 如果当前只涉及一个表，那么我们允许where子句中的列名不需要写成：tableName.columnName的形式
         * 否则，如果当前查询涉及到多表，我们强制要求where子句的格式
         */
        double[] opeKey = new double[3];
        if (metaOfTable.size() == 1 && c.getTable().getName() == null) {
            opeKey = metaOfTable.values().iterator().next().getOpeKey(columnName);
        } else {
            if (metaOfTable.size() != 1 && c.getTable().getName() == null) {
                System.out.println("在涉及多个表的操作时，必须在where子句中指明表名，如employee(表名).salary(列名)");
                System.exit(0);
            } else {
                String tableName = c.getTable().getName();
                opeKey = metaOfTable.get(tableName).getOpeKey(columnName);
            }
        }

        // 右表达式中包含了ope加密所需要的明文
        Expression rightExpression = greaterThan.getRightExpression();
        double rightValue = 0.0;
        if (rightExpression instanceof DoubleValue) {
            rightValue = ((DoubleValue) rightExpression).getValue();
        } else {
            if (rightExpression instanceof LongValue) {
                rightValue = ((LongValue) rightExpression).getValue();
            } else {
                System.out.println("a > b的比较条件中存在不支持的数据类型");
            }
        }
        // 当前我们的测试时，默认不等式的右边是整数，因此sens为1

        OPEAlgorithm ope = new OPEAlgorithm(opeKey[0], opeKey[1], opeKey[2]);
        DoubleValue encDoubleValue = new DoubleValue(Double.toString(ope.nindex(rightValue + 1, false)));
        String secretName;
        try {
            secretName = NameHide.getSecretName(c.getColumnName());
            /*
             * 注意这里不能这样：buffer.append(NameHide.getOPEName(secretName));
             * 不然会导致重复输出。
             */
            c.setColumnName(NameHide.getOPEName(secretName));
            greaterThan.setRightExpression(encDoubleValue);
            visitOldOracleJoinBinaryExpression(greaterThan, " >= ");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("替换不等式左项失败!");
        }
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        // TODO Auto-generated method stub
        Column c = (Column) greaterThanEquals.getLeftExpression();
        String columnName = c.getColumnName();

        // 根据tableName.columnName来查找密钥
        /*
         * 如果当前只涉及一个表，那么我们允许where子句中的列名不需要写成：tableName.columnName的形式
         * 否则，如果当前查询涉及到多表，我们强制要求where子句的格式
         */
        double[] opeKey = new double[3];
        if (metaOfTable.size() == 1 && c.getTable().getName() == null) {
            opeKey = metaOfTable.values().iterator().next().getOpeKey(columnName);
        } else {
            if (metaOfTable.size() != 1 && c.getTable().getName() == null) {
                System.out.println("在涉及多个表的操作时，必须在where子句中指明表名，如employee(表名).salary(列名)");
                return;
            } else {
                String tableName = c.getTable().getName();
                opeKey = metaOfTable.get(tableName).getOpeKey(columnName);
            }
        }

        Expression rightExpression = greaterThanEquals.getRightExpression();
        double rightValue = 0.0;
        if (rightExpression instanceof DoubleValue) {
            rightValue = ((DoubleValue) rightExpression).getValue();
        } else {
            if (rightExpression instanceof LongValue) {
                rightValue = ((LongValue) rightExpression).getValue();
            } else {
                System.out.println("a >= b中右操作数存在不支持的数据类型");
            }
        }
        OPEAlgorithm ope = new OPEAlgorithm(opeKey[0], opeKey[1], opeKey[2]);
        DoubleValue encDoubleValue = new DoubleValue(Double.toString(ope.nindex(rightValue, false)));
        String secretName;
        try {
            secretName = NameHide.getSecretName(c.getColumnName());
            c.setColumnName(NameHide.getOPEName(secretName));
            greaterThanEquals.setRightExpression(encDoubleValue);
            super.visit(greaterThanEquals);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("替换不等式左项失败!");
        }
    }

    @Override
    public void visit(MinorThan minorThan) {
        Column c = (Column) minorThan.getLeftExpression();
        String columnName = c.getColumnName();

        // 根据tableName.columnName来查找密钥
        /*
         * 如果当前只涉及一个表，那么我们允许where子句中的列名不需要写成：tableName.columnName的形式
         * 否则，如果当前查询涉及到多表，我们强制要求where子句的格式
         */
        double[] opeKey = new double[3];
        if (metaOfTable.size() == 1 && c.getTable().getName() == null) {
            opeKey = metaOfTable.values().iterator().next().getOpeKey(columnName);
        } else {
            if (metaOfTable.size() != 1 && c.getTable().getName() == null) {
                System.out.println("在涉及多个表的操作时，必须在where子句中指明表名，如employee(表名).salary(列名)");
                return;
            } else {
                String tableName = c.getTable().getName();
                opeKey = metaOfTable.get(tableName).getOpeKey(columnName);
            }
        }

        // 右表达式中包含了ope加密所需要的明文
        Expression rightExpression = minorThan.getRightExpression();
        double rightValue = 0.0;
        if (rightExpression instanceof DoubleValue) {
            rightValue = ((DoubleValue) rightExpression).getValue();
        } else {
            if (rightExpression instanceof LongValue) {
                rightValue = ((LongValue) rightExpression).getValue();
            } else {
                System.out.println("a < b中右操作数存在不支持的数据类型");
            }
        }
        // 当前我们的测试时，默认不等式的右边是整数，因此sens为1

        OPEAlgorithm ope = new OPEAlgorithm(opeKey[0], opeKey[1], opeKey[2]);
        DoubleValue encDoubleValue = new DoubleValue(Double.toString(ope.nindex(rightValue, false)));
        String secretName;
        try {
            secretName = NameHide.getSecretName(c.getColumnName());
            /*
             * 注意这里不能这样：buffer.append(NameHide.getOPEName(secretName));
             * 不然会导致重复输出。
             */
            c.setColumnName(NameHide.getOPEName(secretName));
            minorThan.setRightExpression(encDoubleValue);
            super.visit(minorThan);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("替换不等式左项失败!");
        }
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        // TODO Auto-generated method stub
        Column c = (Column) minorThanEquals.getLeftExpression();
        String columnName = c.getColumnName();

        // 根据tableName.columnName来查找密钥
        /*
         * 如果当前只涉及一个表，那么我们允许where子句中的列名不需要写成：tableName.columnName的形式
         * 否则，如果当前查询涉及到多表，我们强制要求where子句的格式
         */
        double[] opeKey = new double[3];
        if (metaOfTable.size() == 1 && c.getTable().getName() == null) {
            opeKey = metaOfTable.values().iterator().next().getOpeKey(columnName);
        } else {
            if (metaOfTable.size() != 1 && c.getTable().getName() == null) {
                System.out.println("在涉及多个表的操作时，必须在where子句中指明表名，如employee(表名).salary(列名)");
                return;
            } else {
                String tableName = c.getTable().getName();
                opeKey = metaOfTable.get(tableName).getOpeKey(columnName);
            }
        }

        Expression rightExpression = minorThanEquals.getRightExpression();
        double rightValue = 0.0;
        if (rightExpression instanceof DoubleValue) {
            rightValue = ((DoubleValue) rightExpression).getValue();
        } else {
            if (rightExpression instanceof LongValue) {
                rightValue = ((LongValue) rightExpression).getValue();
            } else {
                System.out.println("a <= b中右操作数存在不支持的数据类型");
            }
        }
        OPEAlgorithm ope = new OPEAlgorithm(opeKey[0], opeKey[1], opeKey[2]);
        DoubleValue encDoubleValue = new DoubleValue(Double.toString(ope.nindex(rightValue + 1, false)));
        String secretName;
        try {
            secretName = NameHide.getSecretName(c.getColumnName());
            c.setColumnName(NameHide.getOPEName(secretName));
            minorThanEquals.setRightExpression(encDoubleValue);
            super.visit(minorThanEquals);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("替换不等式左项失败!");
        }
    }

    /**
     * 这个函数用于应对诸如"a = ?"的情况
     *
     * @param equalsTo
     */
    @Override
    public void visit(EqualsTo equalsTo) {
        // TODO Auto-generated method stub
        try {
            Column c = (Column) equalsTo.getLeftExpression();
            String columnName = c.getColumnName();
            // 等值的加解密的密钥不需要表名
            Key key = KeyManager.generateDETKey("123456", columnName, "det");
            Expression rightExp = equalsTo.getRightExpression();
            String rightToStr = new String();
            if (rightExp instanceof LongValue) {
                rightToStr = ((LongValue) rightExp).getStringValue();
            } else {
                if (rightExp instanceof DoubleValue) {
                    rightToStr = String.valueOf(((DoubleValue) rightExp).getValue());
                } else {
                    if (rightExp instanceof StringValue) {
                        rightToStr = ((StringValue) rightExp).getValue();
                    }
                }
            }

            /*
             * 注意在JSQLParser源码中，StringValue的构造函数有些奇怪。 public StringValue(String
             * escapedValue) { // romoving "'" at the start and at the end value
             * = escapedValue.substring(1, escapedValue.length() - 1); }
             * 注意看，将字符串传递给构造函数的时候，构造函数默认去掉开头和结尾的一对字符
             * 这是因为对于select语句中StringValue是包括单引号的，在构造函数中需要先去掉这对单引号。
             * 但是StringValue.getValue()/setValue()则是不包括单引号的，纯粹获取单引号中间的值。
             * 因此如果我们使用： StringValue encryptedRightValue = new
             * StringValue("test");
             * 实际保存的是"es",而不是"test"，因为即使没有单引号，构造函数仍然会去掉开头和结尾的一对字符，
             * 因此如果我们想要StringValue中保存的"test"的话，我们需要传递给构造函数的参数是"'test'"，即手动加上单引号。
             */

            String value = DETAlgorithm.encrypt(rightToStr, key);
            equalsTo.setRightExpression(new StringValue("'" + value + "'"));
            String secretName;
            secretName = NameHide.getSecretName(c.getColumnName());
            String detName = NameHide.getDETName(secretName);
            ClientDemo.encColumnNameList.add(detName);
            c.setColumnName(detName);
            super.visit(equalsTo);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("替换等式项失败!");
        }

    }
}
