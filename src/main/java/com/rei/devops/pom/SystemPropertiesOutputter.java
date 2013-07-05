package com.rei.devops.pom;

import java.net.URISyntaxException;

public class SystemPropertiesOutputter {
    public static void main(String[] args) {
        for(Object key : System.getProperties().keySet()) {
            System.out.println(key);
        }
    }
}