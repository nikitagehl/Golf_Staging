package com.nikitagehlod.basetest;

import com.nikitagehlod.driver.driverManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;

public class commonToAllTest {

//    @BeforeMethod
//    public void setUp(){
//      driverManager.init();
//    }
//
//    @AfterMethod
//    public void tearDown(){
//        driverManager.down();
//
//    }

    protected WebDriver driver;

    @BeforeMethod
    public void setup() {

        driver = new ChromeDriver();

        driver.manage().window().maximize();


        driver.manage().timeouts()
                .implicitlyWait(Duration.ofSeconds(10));

        driver.get("https://devbeta.betspertsgolf.com/");

        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript("document.body.style.zoom='80%'");
    }

    @AfterMethod
    public void tearDown() {

        if(driver != null) {
            driver.quit();
        }
    }
}
