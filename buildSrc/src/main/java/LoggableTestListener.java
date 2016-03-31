import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;

public class LoggableTestListener implements TestListener {

    private TestListener testListener;

    public LoggableTestListener(TestListener testListener) {
        this.testListener = testListener;
    }

    @Override
    public void beforeSuite(TestDescriptor suite) {
        begin("beforeSuite", suite);
        this.testListener.beforeSuite(suite);
        end("beforeSuite");
    }

    @Override
    public void afterSuite(TestDescriptor suite, TestResult result) {
        begin("afterSuite", suite, result);
        this.testListener.afterSuite(suite, result);
        end("afterSuite");
    }

    @Override
    public void beforeTest(TestDescriptor testDescriptor) {
        begin("beforeTest", testDescriptor);
        this.testListener.beforeTest(testDescriptor);
        end("beforeTest");
    }

    @Override
    public void afterTest(TestDescriptor testDescriptor, TestResult result) {
        begin("afterTest", testDescriptor, result);
        this.testListener.afterTest(testDescriptor, result);
        end("afterTest");
    }

    private void begin(String methodName, TestDescriptor descriptor) {
        System.out.printf("METHOD %s BEGIN (name=%s) (className=%s)%n",
                methodName, descriptor.getName(), descriptor.getClassName());
    }

    private void begin(String methodName, TestDescriptor descriptor, TestResult result) {
        System.out.printf("METHOD %s BEGIN (name=%s) (className=%s) (resultType=%s) (testCount=%s)%n",
                methodName, descriptor.getName(), descriptor.getClassName(), result.getResultType(), result.getTestCount());
    }

    private void end(String methodName) {
        System.out.printf("METHOD %s END%n", methodName);
    }

}
