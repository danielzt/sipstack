package io.sipstack.core;

import static io.pkts.packet.sip.impl.PreConditions.ensureNotEmpty;
import static io.pkts.packet.sip.impl.PreConditions.ensureNotNull;

import com.codahale.metrics.MetricRegistry;

/**
 * The execution environment for an application.
 * 
 * @author jonas@jonasborjesson.com
 */
public class Environment {

    private final String name;
    private final MetricRegistry metricRegistry;


    private Environment(final String name, final MetricRegistry registry) {
        this.name = name;
        this.metricRegistry = registry;
    }

    public Environment addResource(final Object resource) {
        System.err.println("Cool, new resource added " + resource);
        return this;
    }

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
