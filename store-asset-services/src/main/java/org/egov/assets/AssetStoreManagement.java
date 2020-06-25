package org.egov.assets;

import java.util.TimeZone;

import org.egov.tracer.config.TracerConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;


@SpringBootApplication
@ComponentScan(basePackages = {"org.egov.assets","org.egov.assets.common"})
@Import({TracerConfiguration.class})
public class AssetStoreManagement {

    @Value("${app.timezone}")
    private String timeZone;

    @Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).setTimeZone(TimeZone.getTimeZone(timeZone));
	}

    public static void main(String[] args) throws Exception {
        new SpringApplication(AssetStoreManagement.class).run(args);
    }
}
