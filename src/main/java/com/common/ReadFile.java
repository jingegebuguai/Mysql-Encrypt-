package com.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.SQLException;
import java.util.*;

public class ReadFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadFile.class);

    private static final String FILEPATH = PropertiesUtil.getProperties("export.path");

    /**
     * 获取txt文件数据
     * @return
     */
    public static HashMap<Integer, List<String>> getFileData() throws SQLException {

        SqlDetailData.exportData();

        File file = new File(FILEPATH);
        List<String> stringList = new LinkedList();
        HashMap<Integer, List<String>> hashMap = new LinkedHashMap<Integer, List<String>>();
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file), "utf-8");
            BufferedReader bufferedReader = new BufferedReader(reader);
            String lineData = "";
            int index = 1;
            while((lineData = bufferedReader.readLine())!= null) {
                String[] str = lineData.split("\\s+");
                stringList = Arrays.asList(str);
                hashMap.put(index, stringList);
                index ++;
            }
            bufferedReader.close();
            reader.close();
        } catch(IOException e) {
            LOGGER.error("文件流读取失败", e);
            e.printStackTrace();
        }
        return hashMap;
    }

}
