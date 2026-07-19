package com.nikitagehlod.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class propertyReader {

    private static String key;

    public static String readKey(String key){
        propertyReader.key = key;

        try {
            FileInputStream fileInputStream = new FileInputStream(System.getProperty("user.dir")+"/src/main/java/Resources/data.properties");
            Properties p = new Properties();
            p.load(fileInputStream);
            return p.getProperty(key);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
