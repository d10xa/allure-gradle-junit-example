import org.gradle.api.tasks.testing.TestDescriptor;
import org.junit.runner.Description;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.config.AllureModelUtils;
import ru.yandex.qatools.allure.events.TestSuiteStartedEvent;
import ru.yandex.qatools.allure.utils.AnnotationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Suites {

    private Allure lifecycle = Allure.LIFECYCLE;

    private final Map<String, String> suites = new HashMap<>();

    private final ClassLoader classLoader;

    public Suites(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Map<String, String> getSuites() {
        return suites;
    }

    public String getSuiteUid(TestDescriptor descriptor) {
        String suiteName = descriptor.getClassName();
        if (!getSuites().containsKey(suiteName)) {
            Description suiteDescription = Description.createSuiteDescription(classForName(descriptor.getClassName()));
            testSuiteStarted(suiteDescription);
        }
        return getSuites().get(suiteName);
    }

    private Class classForName(String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format("Illegal className %s", className), e);
        }
    }

    public void testSuiteStarted(Description description) {
        String uid = generateSuiteUid(description.getClassName());

        TestSuiteStartedEvent event = new TestSuiteStartedEvent(uid, description.getClassName());
        AnnotationManager am = new AnnotationManager(description.getAnnotations());

        am.update(event);

        event.withLabels(AllureModelUtils.createTestFrameworkLabel("JUnit"));

        getLifecycle().fire(event);
    }

    public String generateSuiteUid(String suiteName) {
        String uid = UUID.randomUUID().toString();
        synchronized (getSuites()) {
            getSuites().put(suiteName, uid);
        }
        return uid;
    }

    public Allure getLifecycle() {
        return lifecycle;
    }
}
