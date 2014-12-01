/**
 * 
 */
package io.sipstack.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The base class for all SIP related configuration options.
 *
 * @author jonas@jonasborjesson.com
 */
public class SipConfiguration {

    @JsonProperty
    private List<ListeningPointConfiguration> listen;

    @JsonIgnore
    public List<ListeningPointConfiguration> getListeningPoints() {
        return this.listen;
    }
}
