package com.deparser;

import java.util.Iterator;
import java.util.List;
import java.util.Map;


import com.core.MetaDataManager;
import com.core.NameHide;
import com.demo.ClientDemo;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

public class SelectDeparserSSDB
        implements SelectVisitor, OrderByVisitor, SelectItemVisitor, FromItemVisitor, PivotVisitor {

    private StringBuilder buffer;
    private ExpressionVisitor expressionVisitor;
    private Map<String, MetaDataManager> metaOfTable;

    public SelectDeparserSSDB() {
        super();
    }

    public SelectDeparserSSDB(Map<String, MetaDataManager> metaOfTable) {
        this.metaOfTable = metaOfTable;
    }

    public SelectDeparserSSDB(Map<String, MetaDataManager> metaOfTable, ExpressionVisitor expressionVisitor,
            StringBuilder buffer) {
        this.buffer = buffer;
        this.expressionVisitor = expressionVisitor;
        this.metaOfTable = metaOfTable;
    }


    public void visit(Pivot pivot) {
        List<Column> forColumns = pivot.getForColumns();
        buffer.append(" PIVOT (").append(PlainSelect.getStringList(pivot.getFunctionItems())).append(" FOR ")
                .append(PlainSelect.getStringList(forColumns, true, forColumns != null && forColumns.size() > 1))
                .append(" IN ").append(PlainSelect.getStringList(pivot.getInItems(), true, true)).append(")");
    }

    public void visit(PivotXml pivot) {
        List<Column> forColumns = pivot.getForColumns();
        buffer.append(" PIVOT XML (").append(PlainSelect.getStringList(pivot.getFunctionItems())).append(" FOR ")
                .append(PlainSelect.getStringList(forColumns, true, forColumns != null && forColumns.size() > 1))
                .append(" IN (");
        if (pivot.isInAny()) {
            buffer.append("ANY");
        } else if (pivot.getInSelect() != null) {
            buffer.append(pivot.getInSelect());
        } else {
            buffer.append(PlainSelect.getStringList(pivot.getInItems()));
        }
        buffer.append("))");
    }


    /**
     * FromItem分为三种情况（有点类似于SelectItem）： 1.Table:select...from table1
     * 2.subJoin:select...from table1 join table2 3.subselect:select...from
     * (select...from) 这里的Table是FromItem的一种情况
     */


    public void visit(Table tableName) {
        buffer.append(tableName.getFullyQualifiedName());
        // ��������ӵ��ռ�����
        ClientDemo.tableNameList.add(tableName.getName());
        Pivot pivot = tableName.getPivot();
        if (pivot != null) {
            pivot.accept(this);
        }
        Alias alias = tableName.getAlias();
        if (alias != null) {
            buffer.append(alias);
        }
    }

    public void visit(SubSelect subSelect) {
        buffer.append("(");
        if (subSelect.getWithItemsList() != null && !subSelect.getWithItemsList().isEmpty()) {
            buffer.append("WITH ");
            for (Iterator<WithItem> iter = subSelect.getWithItemsList().iterator(); iter.hasNext();) {
                WithItem withItem = iter.next();
                withItem.accept(this);
                if (iter.hasNext()) {
                    buffer.append(",");
                }
                buffer.append(" ");
            }
        }
        // ����Select�������Ϊ��ǰ�Ľ�����
        subSelect.getSelectBody().accept(this);
        buffer.append(")");
        Pivot pivot = subSelect.getPivot();
        if (pivot != null) {
            pivot.accept(this);
        }
        Alias alias = subSelect.getAlias();
        if (alias != null) {
            buffer.append(alias.toString());
        }
    }

    public void visit(SubJoin subjoin) {
        buffer.append("(");
        subjoin.getLeft().accept(this);
        deparseJoin(subjoin.getJoin());
        buffer.append(")");

        if (subjoin.getPivot() != null) {
            subjoin.getPivot().accept(this);
        }
    }

    public void visit(LateralSubSelect lateralSubSelect) {
        buffer.append(lateralSubSelect.toString());

    }


    public void visit(ValuesList valuesList) {
        buffer.append(valuesList.toString());
    }

    public void visit(AllColumns allColumns) {
        // TODO Auto-generated method stub
        buffer.append("*");
    }

    public void visit(AllTableColumns allTableColumns) {
        // TODO Auto-generated method stub
        buffer.append(allTableColumns.getTable().getFullyQualifiedName()).append(".*");
    }

    public void visit(SelectExpressionItem selectExpressionItem) {
        // TODO Auto-generated method stub
        SelectExpressionItemSSDB selectExpressionItemSSDB = new SelectExpressionItemSSDB(metaOfTable, buffer);
        selectExpressionItem.getExpression().accept(selectExpressionItemSSDB);
        if (selectExpressionItem.getAlias() != null) {
            buffer.append(selectExpressionItem.getAlias().toString());
        }
    }

    // 改写OrderBy语句，将操作对象的名字改写为OPE类型
    public void visit(OrderByElement orderBy) {
        orderBy.getExpression().accept(expressionVisitor);
        if (!orderBy.isAsc()) {
            buffer.append(" DESC");
        } else if (orderBy.isAscDescPresent()) {
            buffer.append(" ASC");
        }
        if (orderBy.getNullOrdering() != null) {
            buffer.append(' ');
            buffer.append(orderBy.getNullOrdering() == OrderByElement.NullOrdering.NULLS_FIRST ? "NULLS FIRST"
                    : "NULLS LAST");
        }
    }

    public void visit(PlainSelect plainSelect) {
        // TODO Auto-generated method stub
        if (plainSelect.isUseBrackets()) {
            buffer.append("(");
        }
        buffer.append("SELECT ");

        Skip skip = plainSelect.getSkip();
        if (skip != null) {
            buffer.append(skip).append(" ");
        }

        First first = plainSelect.getFirst();
        if (first != null) {
            buffer.append(first).append(" ");
        }

        if (plainSelect.getDistinct() != null) {
            buffer.append("DISTINCT ");
            if (plainSelect.getDistinct().getOnSelectItems() != null) {
                buffer.append("ON (");
                for (Iterator<SelectItem> iter = plainSelect.getDistinct().getOnSelectItems().iterator(); iter
                        .hasNext();) {
                    SelectItem selectItem = iter.next();
                    selectItem.accept(this);
                    if (iter.hasNext()) {
                        buffer.append(", ");
                    }
                }
                buffer.append(") ");
            }

        }
        Top top = plainSelect.getTop();
        if (top != null) {
            buffer.append(top).append(" ");
        }

        for (Iterator<SelectItem> iter = plainSelect.getSelectItems().iterator(); iter.hasNext();) {
            SelectItem selectItem = iter.next();
            selectItem.accept(this);
            if (iter.hasNext()) {
                buffer.append(", ");
            }
        }

        if (plainSelect.getIntoTables() != null) {
            buffer.append(" INTO ");
            for (Iterator<Table> iter = plainSelect.getIntoTables().iterator(); iter.hasNext();) {
                visit(iter.next());
                if (iter.hasNext()) {
                    buffer.append(", ");
                }
            }
        }

        if (plainSelect.getFromItem() != null) {
            buffer.append(" FROM ");
            plainSelect.getFromItem().accept(this);
        }

        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                deparseJoin(join);
            }
        }

        // 获取where子句中条件，进行修改
        if (plainSelect.getWhere() != null) {
            buffer.append(" WHERE ");
            WhereExpressionSSDB whereDeparser = new WhereExpressionSSDB(metaOfTable, this, buffer);
            plainSelect.getWhere().accept(whereDeparser);
        }

        if (plainSelect.getOracleHierarchical() != null) {
            plainSelect.getOracleHierarchical().accept(expressionVisitor);
        }

        if (plainSelect.getGroupByColumnReferences() != null) {
            buffer.append(" GROUP BY ");
            for (Iterator<Expression> iter = plainSelect.getGroupByColumnReferences().iterator(); iter.hasNext();) {
                Expression columnReference = iter.next();
                columnReference.accept(expressionVisitor);
                if (iter.hasNext()) {
                    buffer.append(", ");
                }
            }
        }

        if (plainSelect.getHaving() != null) {
            buffer.append(" HAVING ");
            plainSelect.getHaving().accept(expressionVisitor);
        }

        if (plainSelect.getOrderByElements() != null) {
            deparseOrderBy(plainSelect.isOracleSiblings(), plainSelect.getOrderByElements());
        }

        if (plainSelect.getLimit() != null) {
            deparseLimit(plainSelect.getLimit());
        }
        if (plainSelect.getOffset() != null) {
            deparseOffset(plainSelect.getOffset());
        }
        if (plainSelect.getFetch() != null) {
            deparseFetch(plainSelect.getFetch());
        }
        if (plainSelect.isForUpdate()) {
            buffer.append(" FOR UPDATE");
            if (plainSelect.getForUpdateTable() != null) {
                buffer.append(" OF ").append(plainSelect.getForUpdateTable());
            }
        }
        if (plainSelect.isUseBrackets()) {
            buffer.append(")");
        }
    }

    /**
     * 该函数用于处理Join部分的，在我们的系统中没有隐藏表名，因此我们只需要修改ON语句之后的条件部分就可以了
     * 具体的方法是自定义一个ON表达式的解析器，在其中将列名隐藏起来
     *
     * @see Join
     * @param join
     */

    public void deparseJoin(Join join) {
        if (join.isSimple()) {
            buffer.append(", ");
        } else {

            if (join.isRight()) {
                buffer.append(" RIGHT");
            } else if (join.isNatural()) {
                buffer.append(" NATURAL");
            } else if (join.isFull()) {
                buffer.append(" FULL");
            } else if (join.isLeft()) {
                buffer.append(" LEFT");
            } else if (join.isCross()) {
                buffer.append(" CROSS");
            }

            if (join.isOuter()) {
                buffer.append(" OUTER");
            } else if (join.isInner()) {
                buffer.append(" INNER");
            }

            buffer.append(" JOIN ");

        }

        FromItem fromItem = join.getRightItem();
        fromItem.accept(this);
        if (join.getOnExpression() != null) {
            buffer.append(" ON ");
            /*
             * 我们目前仅支持等值连接，因此只需要修改其中的visit(EqualTo)函数即可 举例：on table1.id =
             * table2.id 改写后：on table1.di_DET = table2.di_DET
             */

            ExpressionDeParser onExpressionDeparser = new ExpressionDeParser(this, buffer) {

                @Override
                public void visit(EqualsTo equalsTo) {
                    // TODO Auto-generated method stub
                    try {
                        Column leftColumn = (Column) equalsTo.getLeftExpression();
                        String leftColumnName = leftColumn.getColumnName();
                        String detNameLeft = NameHide.getDETName(NameHide.getSecretName(leftColumnName));
                        leftColumn.setColumnName(detNameLeft);

                        Column rightColumn = (Column) equalsTo.getRightExpression();
                        String rightColumnName = rightColumn.getColumnName();
                        String detNameRight = NameHide.getDETName(NameHide.getSecretName(rightColumnName));
                        rightColumn.setColumnName(detNameRight);

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    super.visit(equalsTo);
                }

            };
            join.getOnExpression().accept(onExpressionDeparser);
        }
        if (join.getUsingColumns() != null) {
            buffer.append(" USING (");
            for (Iterator<Column> iterator = join.getUsingColumns().iterator(); iterator.hasNext();) {
                Column column = iterator.next();
                buffer.append(column.getFullyQualifiedName());
                if (iterator.hasNext()) {
                    buffer.append(", ");
                }
            }
            buffer.append(")");
        }

    }


    /**
     * 处理OrderBy元素的,我们的目标是将OrderBy后面的列名改写为OPE类型
     */
    public void deparseOrderBy(List<OrderByElement> orderByElements) {
        deparseOrderBy(false, orderByElements);
    }

    /**
     * 举例，输入： XXXX order by id,salary; 改写为：XXXX order by di_OPE,yralas_OPE;
     *
     *
     */

    public void deparseOrderBy(boolean oracleSiblings, List<OrderByElement> orderByElements) {
        if (oracleSiblings) {
            buffer.append(" ORDER SIBLINGS BY ");
        } else {
            buffer.append(" ORDER BY ");
        }

        for (Iterator<OrderByElement> iter = orderByElements.iterator(); iter.hasNext();) {
            try {
                OrderByElement orderByElement = iter.next();
                Column column = (Column) orderByElement.getExpression();
                String columnName = column.getColumnName();
                // System.out.println(columnName);
                column.setColumnName(NameHide.getOPEName(NameHide.getSecretName(columnName)));
                orderByElement.accept(this);
                if (iter.hasNext()) {
                    buffer.append(", ");
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void deparseLimit(Limit limit) {
        // LIMIT n OFFSET skip
        if (limit.isRowCountJdbcParameter()) {
            buffer.append(" LIMIT ");
            buffer.append("?");
        } else if (limit.getRowCount() >= 0) {
            buffer.append(" LIMIT ");
            buffer.append(limit.getRowCount());
        } else if (limit.isLimitNull()) {
            buffer.append(" LIMIT NULL");
        }

        if (limit.isOffsetJdbcParameter()) {
            buffer.append(" OFFSET ?");
        } else if (limit.getOffset() != 0) {
            buffer.append(" OFFSET ").append(limit.getOffset());
        }

    }

    public void deparseOffset(Offset offset) {
        // OFFSET offset
        // or OFFSET offset (ROW | ROWS)
        if (offset.isOffsetJdbcParameter()) {
            buffer.append(" OFFSET ?");
        } else if (offset.getOffset() != 0) {
            buffer.append(" OFFSET ");
            buffer.append(offset.getOffset());
        }
        if (offset.getOffsetParam() != null) {
            buffer.append(" ").append(offset.getOffsetParam());
        }

    }

    public void deparseFetch(Fetch fetch) {
        buffer.append(" FETCH ");
        if (fetch.isFetchParamFirst()) {
            buffer.append("FIRST ");
        } else {
            buffer.append("NEXT ");
        }
        if (fetch.isFetchJdbcParameter()) {
            buffer.append("?");
        } else {
            buffer.append(fetch.getRowCount());
        }
        buffer.append(" ").append(fetch.getFetchParam()).append(" ONLY");

    }

    public StringBuilder getBuffer() {
        return buffer;
    }

    public void setBuffer(StringBuilder buffer) {
        this.buffer = buffer;
    }

    public ExpressionVisitor getExpressionVisitor() {
        return expressionVisitor;
    }

    public void setExpressionVisitor(ExpressionVisitor visitor) {
        expressionVisitor = visitor;
    }

    public void visit(SetOperationList setOperationList) {
        // TODO Auto-generated method stub

    }

    public void visit(WithItem withItem) {
        // TODO Auto-generated method stub

    }

}
