
package io.moquette.logging;

import java.util.ArrayList;
import java.util.Collection;
import io.moquette.interception.InterceptHandler;

public class LoggingUtils {

    public static <T extends InterceptHandler> Collection<String> getInterceptorIds(Collection<T> handlers) {
        Collection<String> result = new ArrayList<>(handlers.size());
        for (T handler : handlers) {
            result.add(handler.getID());
        }
        return result;
    }
}
