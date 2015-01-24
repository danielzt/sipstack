/**
 * 
 */
package io.sipstack.config;

import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * @author jonas@jonasborjesson.com
 */
public class ConfigTestBase {

    /**
     * Helper method for loading a configuration. 
     * 
     * Note, the path of the resources being loaded is based on the
     * {@link ConfigTestBase}.
     * 
     * @param clazz
     * @param resource
     * @return
     * @throws Exception
     */
    public <T> T loadConfiguration(final Class<T> clazz, final String resource) throws Exception {
        final InputStream stream = ConfigTestBase.class.getResourceAsStream(resource);
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(NetworkInterfaceConfiguration.class, new NetworkInterfaceDeserializer());
        mapper.registerModule(module);
        return mapper.readValue(stream, clazz);
    }

}
