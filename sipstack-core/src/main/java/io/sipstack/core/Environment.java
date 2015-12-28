package io.sipstack.core;

import com.codahale.metrics.MetricRegistry;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotEmpty;
import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

/**
 * The execution environment for an io.sipstack.application.application.
 * 
 * @author jonas@jonasborjesson.com
 */
public class Environment {

    private final String name;
    private final MetricRegistry metricRegistry;

    // private final List<DynamicApplicationInvoker> sipHandlers = new ArrayList<>();


    private Environment(final String name, final MetricRegistry registry) {
        this.name = name;
        this.metricRegistry = registry;
    }

    public Environment addResource(final Object resource) {
        /*
        final DynamicApplicationInvoker sip = wrapSipHandler(resource);
        if (sip != null) {
            this.sipHandlers.add(sip);
        }
        */

        return this;
    }

    /*
    private DynamicApplicationInvoker wrapSipHandler(final Object resource) {
        try {
            return DynamicApplicationInvoker.wrap(resource);
        } catch (final IllegalArgumentException e) {
            // ignore. Just means that the resource wasn't a SIP
            // enabled object
            return null;
        }
    }
    */

    public static Builder withName(final String name) throws IllegalArgumentException {
        return new Builder(ensureNotEmpty(name, "Name cannot be null or the empty string"));
    }

    public static class Builder {

        private final String name;

        private MetricRegistry metricRegistry;

        private Builder(final String name) {
            this.name = name;
        }

        public Builder withMetricRegistry(final MetricRegistry registry) {
            this.metricRegistry = ensureNotNull(registry);
            return this;
        }

        public Environment build() throws IllegalArgumentException {
            ensureNotNull(this.metricRegistry, "You must specify the MetricsRegistry");
            return new Environment(this.name, this.metricRegistry);
        }

    }

}
