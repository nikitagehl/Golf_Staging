package listeners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.openqa.selenium.TimeoutException;
import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;

import com.nikitagehlod.utils.propertyReader;

/**
 * Re-runs a test ONLY when it failed because of a Selenium timeout (slow page / slow server),
 * up to "retry.count" times (data.properties, default 1).
 * Real failures - wrong text, wrong page, assertion errors - are NOT retried, so bugs still show up.
 *
 * Registered in testng-sanity.xml as a listener; it attaches itself to every @Test automatically.
 */
public class RetryOnTimeout implements IRetryAnalyzer, IAnnotationTransformer {

    private int attempts = 0;

    private static int maxRetries() {
        String v = propertyReader.readKey("retry.count");
        try {
            return v == null ? 1 : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // ------------------------------------------------------------- IRetryAnalyzer

    @Override
    public boolean retry(ITestResult result) {
        if (attempts >= maxRetries() || !isTimeout(result.getThrowable())) {
            return false;
        }
        attempts++;
        System.out.println("Retrying " + result.getMethod().getMethodName()
                + " after a timeout (attempt " + attempts + " of " + maxRetries() + ")");
        return true;
    }

    private static boolean isTimeout(Throwable t) {
        while (t != null) {
            if (t instanceof TimeoutException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    // ------------------------------------------------------- IAnnotationTransformer

    @Override
    @SuppressWarnings("rawtypes")
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryOnTimeout.class);
    }
}
