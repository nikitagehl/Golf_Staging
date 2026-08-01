package com.nikitagehlod.basetest;

import com.nikitagehlod.testPages.RabbitHoleTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import com.nikitagehlod.utils.propertyReader;

import java.time.Duration;

public class commonToAllTest {
//    protected WebDriver driver;



    protected WebDriver driver;

    public WebDriver getDriver() {
        return driver;
    }

    @BeforeClass
    public void setup() {
//        logger.info("Starting the test");
        driver = new ChromeDriver();

        driver.manage().window().maximize();


        driver.manage().timeouts()
                .implicitlyWait(Duration.ofSeconds(10));

        driver.get(propertyReader.readKey("URL"));

        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript("document.body.style.zoom='80%'");
    }

    @AfterClass
    public void tearDown() {

        if(driver != null) {
            driver.quit();
        }
    }





}
