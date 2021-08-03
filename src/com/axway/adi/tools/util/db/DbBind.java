package com.axway.adi.tools.util.db;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DbBind {
    /**
     * @return SQL binding name
     */
    String value() default "";

    /**
     * @return true if field is primary key, false otherwise
     */
    boolean primary() default false;

    /**
     * @return true if field is foreign key, false otherwise
     */
    boolean foreign() default false;
}
