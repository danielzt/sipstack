/**
 * 
 */
package io.sipstack.actor;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public interface Task extends Runnable {

    /**
     * Returns the friendly name of the task. Primarily for logging purposes.
     * 
     * @return
     */
    String name();

    void onEvent(PipeLine pipe, final Event event);

    static class DefaultTask implements Task {

        @Override
        public void run() {
            // TODO Auto-generated method stub
        }

        @Override
        public String name() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void onEvent(final PipeLine pipe, final Event event) {
            // TODO Auto-generated method stub
        }

    }


}
