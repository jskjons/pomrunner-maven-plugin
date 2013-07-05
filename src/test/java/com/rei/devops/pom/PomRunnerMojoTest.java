package com.rei.devops.pom;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;

public class PomRunnerMojoTest {

    @Test
    public void testExecute() throws Exception {
        System.setProperty("custom.property", "blah");
        System.setProperty("g", "blahblah");
        System.setProperty("goals", "blahblah");
        PomRunnerMojo mojo = new PomRunnerMojo();
        mojo.javaHome = new File(System.getProperty("java.home"));  
        Map<String, String> userProperties = mojo.getUserSuppliedSystemProperties();
        assertTrue(userProperties.containsKey("custom.property"));
    }

}
