package com.sunx.constant;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;

/**
 * Created by sunxing on 16/6/17.
 *
 */
public class Configuration {
    private Properties properties = null;
    private static final String configurationFilePath = "conf/conf.properties";

    private static String HOST_NAME = null;

    /**
     */
    private Configuration() {
        try {
            properties = new Properties();
            InputStream is = new FileInputStream(new File(configurationFilePath));
            properties.load(is);

            //构造当前电脑的hostname
            HOST_NAME = InetAddress.getLocalHost().getHostName();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     */
    private static class SingletonHolder{
        public static final  Configuration config = new Configuration();
    }

    /**
     * @return
     */
    public static Configuration me(){
        return SingletonHolder.config;
    }

    /**
     * @param key
     * @return
     */
    public String getString(String key){
        if(properties.containsKey(key))return properties.getProperty(key);
        return null;
    }

    /**
     * @param key
     * @return
     */
    public int getInt(String key){
        int result = -1;
        if(properties.containsKey(key)){
            result = Integer.parseInt(properties.getProperty(key));
        }
        return result;
    }

    /**
     * @param key
     * @return
     */
    public long getLong(String key){
        long result = -1;
        if(properties.containsKey(key)){
            result = Long.parseLong(properties.getProperty(key));
        }
        return result;
    }

    /**
     * @param key
     * @return
     */
    public boolean getBoolean(String key){
        boolean flag = false;
        if(properties.containsKey(key)){
            flag = Boolean.parseBoolean(properties.getProperty(key));
        }
        return flag;
    }

    public Object get(String key){
        return properties.get(key);
    }

    public String host(){
        return HOST_NAME;
    }
}
