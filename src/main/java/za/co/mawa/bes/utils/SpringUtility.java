package za.co.mawa.bes.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringUtility implements ApplicationContextAware {

    private static volatile ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(final ApplicationContext context) {
        SpringUtility.applicationContext = context;
    }

    /** Get a bean from the application context. */
    public static <T> T getBean(final Class<T> type) {
        return applicationContext.getBean(type);
    }

    /** Return the application context if necessary for anything else. */
    public static ApplicationContext getContext() {
        return applicationContext;
    }
}
