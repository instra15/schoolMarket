package com.schoolMarket.aspect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface LogOperation {
    String value() default "";
}
