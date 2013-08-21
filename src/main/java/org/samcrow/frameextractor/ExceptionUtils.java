package org.samcrow.frameextractor;

import java.io.PrintWriter;
import java.io.StringWriter;


public class ExceptionUtils {
    
    /**
     * Returns the stack trace of an exception
     * @param exception
     * @return 
     */
    public static String getStackTrace(Throwable exception) {
        
        StringWriter writer = new StringWriter();
        
        exception.printStackTrace(new PrintWriter(writer));
        
        return writer.toString();
    }
    
    private ExceptionUtils() {}
}
