package com.core;

import java.util.List;

public class RNDOnion {

    public static String packOnRND(String tableName, List<String> columnNameList, String password) {

        StringBuilder udfBuffer = new StringBuilder();
        udfBuffer.append("update " + tableName + " set ");

        /*
         * 在这里我们考虑一种情况： select id from peach where id = 1;
         * 其中两次出现id，但我们只需要对id_DET执行一次包RND层，所以我们要去掉重复的列名
         */

        String columnName = "";
        for (int index = 0; index < columnNameList.size(); index++) {
            columnName = columnNameList.get(index);
            udfBuffer.append(
                    columnName + " = to_base64(aes_encrypt(" + columnName + ",concat(rowid," + "'" + password + "')))");
            if (index != (columnNameList.size() - 1)) {
                udfBuffer.append(",");
            }
        }
        return udfBuffer.toString();
    }

    public static String peelOffRND(String tableName, List<String> columnNameList, String password) {
        StringBuilder udfBuffer = new StringBuilder();
        udfBuffer.append("update " + tableName + " set ");
        String columnName = "";
        for (int index = 0; index < columnNameList.size(); index++) {
            columnName = columnNameList.get(index);
            udfBuffer.append(columnName + " = aes_decrypt(from_base64(" + columnName + "),concat(rowid," + "'"
                    + password + "'" + "))");
            if (index != (columnNameList.size() - 1)) {
                udfBuffer.append(",");
            }
        }
        return udfBuffer.toString();
    }
}
