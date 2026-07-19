package com.nikitagehlod.driver;

import org.openqa.selenium.WebDriver;

public class driverManager_TL {
    public static final ThreadLocal<WebDriver> dr= new ThreadLocal<>();

    public static void setDriver(WebDriver driver){
        dr.set(driver);
    }

    public static WebDriver getDriver(){
       return dr.get();
    }


    public static void unload(){
        dr.remove();
    }


}
