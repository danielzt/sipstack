/**
 * 
 */
package io.sipstack.core;

import io.sipstack.config.Configuration;

import com.codahale.metrics.MetricRegistry;

/**
 * @author jonas@jonasborjesson.com
 */
public final class Bootstrap<T extends Configuration> {

    private final Application<T> application;

    private final MetricRegistry metricRegistry;

    /**
     * 
     */
    public Bootstrap(final Application<T> application) {
        this.application = application;
        this.metricRegistry = new MetricRegistry();
    }

    public MetricRegistry getMetricRegistry() {
        return this.metricRegistry;
    }

}
