import groovy.lang.GroovyClassLoader;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;
import org.junit.internal.AssumptionViolatedException;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.config.AllureModelUtils;
import ru.yandex.qatools.allure.events.*;
import ru.yandex.qatools.allure.utils.AnnotationManager;

import java.io.File;
import java.lang.reflect.Method;

public class AllureTestListener implements TestListener {

    private FileCollection classPathFiles;

    private ClassLoader classLoader;

    private Suites suites;

    public AllureTestListener(FileCollection classPathFiles) {
        this.classPathFiles = classPathFiles;
    }

    public ClassLoader getClassLoader() {
        init();
        return classLoader;
    }

    public Suites getSuites() {
        init();
        return suites;
    }

    private void init() {
        if (classLoader != null) {
            return;
        }
        GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
        for (File file : classPathFiles) {
            groovyClassLoader.addClasspath(file.toString());
        }
        this.classLoader = groovyClassLoader;
        this.suites = new Suites(groovyClassLoader);
    }

    @Override
    public void beforeSuite(TestDescriptor descriptor) {
        String className = descriptor.getClassName();
        if (className == null) {
            return;
        }
        String uid = getSuites().generateSuiteUid(descriptor.getClassName());
        Class<?> aClass = classForName(className);

        TestSuiteStartedEvent event = new TestSuiteStartedEvent(uid, className);
        AnnotationManager am = new AnnotationManager(aClass.getAnnotations());

        am.update(event);

        event.withLabels(AllureModelUtils.createTestFrameworkLabel("JUnit"));

        getLifecycle().fire(event);

    }

    private Class classForName(String className) {
        try {
            return Class.forName(className, false, getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format("Illegal className %s", className), e);
        }
    }

    private Method methodByName(Class c, String methodName) {
        try {
            return c.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("Illegal methodName %s", methodName), e);
        }
    }

    @Override
    public void afterSuite(TestDescriptor descriptor, TestResult result) {
        if (descriptor.getClassName() == null) {
            return;
        }
        testSuiteFinished(getSuites().getSuiteUid(descriptor));
    }

    @Override
    public void beforeTest(TestDescriptor testDescriptor) {
        String suiteUid = getSuites().getSuiteUid(testDescriptor);
        TestCaseStartedEvent event = new TestCaseStartedEvent(suiteUid, testDescriptor.getName());
        Class c = classForName(testDescriptor.getClassName());
        Method declaredMethod = methodByName(c, testDescriptor.getName());
        AnnotationManager am = new AnnotationManager(declaredMethod.getAnnotations());

        am.update(event);

        fireClearStepStorage();
        getLifecycle().fire(event);
    }

    @Override
    public void afterTest(TestDescriptor testDescriptor, TestResult result) {
        TestResult.ResultType resultType = result.getResultType();
        switch (resultType) {
            case FAILURE:
                fireTestCaseFailure(result.getException());
                break;
            case SKIPPED:
                startFakeTestCase(testDescriptor);
                getLifecycle().fire(new TestCasePendingEvent().withMessage("Test ignored (without reason)!"));
                finishFakeTestCase();
                break;
            case SUCCESS:
                getLifecycle().fire(new TestCaseFinishedEvent());
        }
    }

    public void testSuiteFinished(String uid) {
        getLifecycle().fire(new TestSuiteFinishedEvent(uid));
    }

    public void startFakeTestCase(TestDescriptor description) {

    }

    public void finishFakeTestCase() {
        getLifecycle().fire(new TestCaseFinishedEvent());
    }

    public void fireTestCaseFailure(Throwable throwable) {
        if (throwable instanceof AssumptionViolatedException) {
            getLifecycle().fire(new TestCaseCanceledEvent().withThrowable(throwable));
        } else {
            getLifecycle().fire(new TestCaseFailureEvent().withThrowable(throwable));
        }
    }

    public void fireClearStepStorage() {
        getLifecycle().fire(new ClearStepStorageEvent());
    }

    private Allure getLifecycle() {
        return getSuites().getLifecycle();
    }

}
