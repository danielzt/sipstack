/**
 * 
 */
package io.sipstack.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jonas@jonasborjesson.com
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SipMethod {

    public static final String INVITE="INVITE";

    public static final String ACK="ACK"; 

    public static final String CANCEL="CANCEL";

    public static final String BYE="BYE";

    public static final String OPTIONS="OPTIONS";

    public static final String SUBSCRIBE="SUBSCRIBE";

    public static final String NOTIFY="NOTIFY";

    public static final String REGISTER="REGISTER";

    public static final String PRACK="PRACK";

    public static final String MESSAGE="MESSAGE";

    public static final String INFO="INFO";

    public static final String UPDATE="UPDATE";

    public static final String REFER="REFER";

    public static final String PUBLISH="PUBLISH";

    String value();
}
