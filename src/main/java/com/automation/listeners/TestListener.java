package com.automation.listeners;

import io.qameta.allure.Attachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG Listener for test execution lifecycle events.
 * Provides logging and Allure report attachments.
 */
public class TestListener implements ITestListener {

    private static final Logger logger = LogManager.getLogger(TestListener.class);

    @Override
    public void onStart(ITestContext context) {
        logger.info("========================================");
        logger.info("Test Suite Started: {}", context.getName());
        logger.info("========================================");
    }

    @Override
    public void onFinish(ITestContext context) {
        logger.info("========================================");
        logger.info("Test Suite Finished: {}", context.getName());
        logger.info("Passed: {}, Failed: {}, Skipped: {}",
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
        logger.info("========================================");
    }

    @Override
    public void onTestStart(ITestResult result) {
        logger.info("----------------------------------------");
        logger.info("Test Started: {}", getTestMethodName(result));
        logger.info("----------------------------------------");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        logger.info("Test PASSED: {} (Duration: {}ms)",
                getTestMethodName(result),
                result.getEndMillis() - result.getStartMillis());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        logger.error("Test FAILED: {}", getTestMethodName(result));
        logger.error("Failure Reason: {}", result.getThrowable().getMessage());
        saveFailureLog(result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        logger.warn("Test SKIPPED: {}", getTestMethodName(result));
        if (result.getThrowable() != null) {
            logger.warn("Skip Reason: {}", result.getThrowable().getMessage());
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        logger.warn("Test FAILED but within success percentage: {}", getTestMethodName(result));
    }

    private String getTestMethodName(ITestResult result) {
        return result.getTestClass().getName() + "." + result.getMethod().getMethodName();
    }

    @Attachment(value = "Failure Stack Trace", type = "text/plain")
    private String saveFailureLog(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getMessage()).append("\n\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    @Attachment(value = "{name}", type = "text/plain")
    public static String saveLog(String name, String content) {
        return content;
    }
}
