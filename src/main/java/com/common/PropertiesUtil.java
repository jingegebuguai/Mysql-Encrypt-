package com.common;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class PropertiesUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(Properties.class);

    private static Properties properties;

    static {
        String fileName = "database.properties";
        properties = new Properties();
        try{
            properties.load(new InputStreamReader(PropertiesUtil.class.getClassLoader().getResourceAsStream(fileName), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("配置文件异常",e);
        }
    }

    /**
     * 获取键值
     * @param key
     * @return
     */
    public static String getProperties(String key) {

        String value = properties.getProperty(key.trim());
        if(StringUtils.isBlank(value)) {
            return null;
        }
        return value.trim();

    }

    /**
     * 获取键值，否则设置为默认值
     * @param key
     * @param defaultValue
     * @return
     */
    public static String getProperties(String key, String defaultValue) {

        String value = properties.getProperty(key.trim());
        if(StringUtils.isBlank(value)) {
            value = defaultValue;
        }
        return value.trim();
    }


}
