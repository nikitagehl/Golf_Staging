package listeners;

import java.io.ByteArrayInputStream;

import com.nikitagehlod.Utils.DriverManager;
import com.nikitagehlod.basetest.commonToAllTest;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import io.qameta.allure.Allure;

import static com.nikitagehlod.driver.driverManager.getDriver;

public class TestListener implements ITestListener {



        @Override
        public void onTestStart(ITestResult result) {
            System.out.println(result.getMethod().getMethodName() + " Started");
        }

        @Override
        public void onTestSuccess(ITestResult result) {
            System.out.println(result.getMethod().getMethodName() + " Passed");
        }

        @Override
        public void onTestFailure(ITestResult result) {

            System.out.println(result.getMethod().getMethodName() + " Failed");

            TakesScreenshot ts = (TakesScreenshot) getDriver();

            byte[] screenshot = ts.getScreenshotAs(OutputType.BYTES);

            Allure.addAttachment(
                    "Failure Screenshot",
                    new ByteArrayInputStream(screenshot));

        }

        @Override
        public void onTestSkipped(ITestResult result) {

        }

        @Override
        public void onStart(ITestContext context) {

        }

        @Override
        public void onFinish(ITestContext context) {

        }



}
