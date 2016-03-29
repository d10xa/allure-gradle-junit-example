import groovy.lang.GroovyClassLoader;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;
import org.junit.Ignore;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.config.AllureModelUtils;
import ru.yandex.qatools.allure.events.*;
import ru.yandex.qatools.allure.utils.AnnotationManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//import ru.yandex.qatools.allure.events.ClearStepStorageEvent;
//import ru.yandex.qatools.allure.events.TestCaseCanceledEvent;
//import ru.yandex.qatools.allure.events.TestCaseFailureEvent;
//import ru.yandex.qatools.allure.events.TestCaseFinishedEvent;
//import ru.yandex.qatools.allure.events.TestCasePendingEvent;
//import ru.yandex.qatools.allure.events.TestCaseStartedEvent;
//import ru.yandex.qatools.allure.events.TestSuiteFinishedEvent;
//import ru.yandex.qatools.allure.events.TestSuiteStartedEvent;

public class AllureTestListener implements TestListener {

    private final Map<String, String> suites = new HashMap<>();

    private GroovyClassLoader classLoader;

//    String uid = UUID.randomUUID().toString();

    private Allure lifecycle = Allure.LIFECYCLE;

    AnnotationManager am = null;

    public AllureTestListener(List<File> classpath) {
        classLoader = new GroovyClassLoader();
        for (File file : classpath) {
            classLoader.addClasspath(file.toString());
        }
    }

    @Override
    public void beforeSuite(TestDescriptor suite) {
        String className = suite.getClassName();
        if (className == null) {
            return;
        }
        String uid = generateSuiteUid(suite.getClassName());
        Class<?> aClass = classForName(className);

//
        TestSuiteStartedEvent event = new TestSuiteStartedEvent(uid, className);
        am = new AnnotationManager(aClass.getAnnotations());

        am.update(event);
//
        event.withLabels(AllureModelUtils.createTestFrameworkLabel("JUnit"));
//
        getLifecycle().fire(event);
//        System.out.println("before suite");
//        System.out.println(className);
//        System.out.println(suite.getName());
//        System.out.println("----------------");
    }

    private Class classForName(String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format("Illegal className %s", className), e);
        }
    }

    private Method methodByName(Class c, String methodName){
        try {
            return c.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("Illegal methodName %s", methodName), e);
        }
    }

    @Override
    public void afterSuite(TestDescriptor suite, TestResult result) {
        System.out.println("after suite");
        System.out.println(suite.getName());
        System.out.println(result.getFailedTestCount());
        System.out.println("----------------");
        testSuiteFinished(getSuiteUid(suite));
    }

    @Override
    public void beforeTest(TestDescriptor testDescriptor) {
        System.out.println("EXPECT TEST NAME" + testDescriptor.getName());
        TestCaseStartedEvent event = new TestCaseStartedEvent(getSuiteUid(testDescriptor), testDescriptor.getName());
        Class c = classForName(testDescriptor.getClassName());
        Method declaredMethod = methodByName(c,testDescriptor.getName());
        AnnotationManager am = new AnnotationManager(c.getAnnotations());

        am.update(event);

        fireClearStepStorage();
        getLifecycle().fire(event);
//        TestCaseStartedEvent event = new TestCaseStartedEvent(uid, testDescriptor.getName());// TODO method name
//        AnnotationManager am = null;
//        try {
//            am = new AnnotationManager(Class.forName(testDescriptor.getClassName()).getAnnotations());
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        am.update(event);
//
//        fireClearStepStorage();
//        getLifecycle().fire(event);
    }

    public void testSuiteFinished(String uid) {
        getLifecycle().fire(new TestSuiteFinishedEvent(uid));
    }

    @Override
    public void afterTest(TestDescriptor testDescriptor, TestResult result) {
        for (String uid : getSuites().values()) {
            testSuiteFinished(uid);
        }
//        try {
//            Class.forName(testDescriptor.getClassName()).getAnnotations();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }

    }

    public String generateSuiteUid(String suiteName) {
        String uid = UUID.randomUUID().toString();
        synchronized (getSuites()) {
            getSuites().put(suiteName, uid);
        }
        return uid;
    }

    public String getSuiteUid(TestDescriptor descriptor) {
        String suiteName = descriptor.getClassName();
        if (!getSuites().containsKey(suiteName)) {
            Description suiteDescription = Description.createSuiteDescription(classForName(descriptor.getClassName()));
            testSuiteStarted(suiteDescription);
        }
        return getSuites().get(suiteName);
    }

    public void testSuiteStarted(Description description) {
        String uid = generateSuiteUid(description.getClassName());

        TestSuiteStartedEvent event = new TestSuiteStartedEvent(uid, description.getClassName());
        AnnotationManager am = new AnnotationManager(description.getAnnotations());

        am.update(event);

        event.withLabels(AllureModelUtils.createTestFrameworkLabel("JUnit"));

        getLifecycle().fire(event);
    }

    public String getIgnoredMessage(Description description) {
        Ignore ignore = description.getAnnotation(Ignore.class);
        return ignore == null || ignore.value().isEmpty() ? "Test ignored (without reason)!" : ignore.value();
    }

    public void startFakeTestCase(Description description) {
//        String uid = getSuiteUid(description);

//        String name = description.isTest() ? description.getMethodName() : description.getClassName();
//        TestCaseStartedEvent event = new TestCaseStartedEvent(getSuiteUid(description), name);
//        AnnotationManager am = new AnnotationManager(description.getAnnotations());
//        am.update(event);
//
//        fireClearStepStorage();
//        getLifecycle().fire(event);
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

    public Allure getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(Allure lifecycle) {
        this.lifecycle = lifecycle;
    }

    public Map<String, String> getSuites() {
        return suites;
    }

}
