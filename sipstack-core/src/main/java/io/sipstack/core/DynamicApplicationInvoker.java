/**
 * 
 */
package io.sipstack.core;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;
import io.sipstack.annotations.BYE;
import io.sipstack.annotations.INVITE;
import io.sipstack.netty.codec.sip.event.SipMessageEvent;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author jonas@jonasborjesson.com
 */
public class DynamicApplicationInvoker {

    private final Object application;
    private final Method doInvite;
    private final Method doBye;

    /**
     * 
     */
    private DynamicApplicationInvoker(final Object application, final Method doInvite, final Method doBye) {
        this.application = application;
        this.doInvite = doInvite;
        this.doBye = doBye;
    }

    public void doInvite(final SipMessageEvent event) {
        invokeSipMethod(this.doInvite, event);
    }

    public void doBye(final SipMessageEvent event) {
        invokeSipMethod(this.doBye, event);
    }

    private void invokeSipMethod(final Method method, final SipMessageEvent event) {
        if (method == null) {
            return;
        }

        try {
            method.invoke(this.application, event);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static DynamicApplicationInvoker wrap(final Object application) {
        ensureNotNull(application, "The application object cannot be null");
        final Class<?> clazz = application.getClass();
        final Method[] methods = clazz.getMethods();
        final Method doInvite = ensureSipMethod(INVITE.class, methods);
        final Method doBye = ensureSipMethod(BYE.class, methods);

        // if there were no annotations whatsoever on the application then
        // what's the point? So, complain and bail out
        if (doInvite == null && doBye == null) {
            throw new IllegalArgumentException("Did not find a single annoted method on the application " 
                    + application.getClass() + ", which would mean that your SIP Application simply wouldn't do "
                    + "anything so therefore I refuse to continue");
        }
        return new DynamicApplicationInvoker(application, doInvite, doBye);
    }


    private static Method ensureSipMethod(final Class<? extends Annotation> annotation, final Method[] methods) {
        for (final Method method : methods) {
            if (method.isAnnotationPresent(annotation)) {
                final Class<?>[] params = method.getParameterTypes();
                if (params.length != 1 || params[0] != SipMessageEvent.class) {
                    throw new IllegalArgumentException("Methods marked with " 
                            + annotation.toString() 
                            + " must have a single parameter of type " + SipMessageEvent.class);
                }
                return method;
            }
        }

        return null;
    }



}
