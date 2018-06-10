package com.mycorp.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mycorp.http.client.Zendesk;

@Configuration
@ComponentScan(basePackages = { "com.mycorp.*" })
public class ZendeskConfiguration {

    @Value("#{envPC['zendesk.token']}")
    public String zendeskToken = "";

    @Value("#{envPC['zendesk.url']}")
    public String zendeskURL = "";

    @Value("#{envPC['zendesk.user']}")
    public String zendeskUser = "";

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Zendesk zendesk() {
	return new Zendesk(zendeskURL, zendeskUser, zendeskToken);
    }

    @Bean
    @Qualifier("zendesk")
    public ObjectMapper objectMapperZendesk() {
	ObjectMapper mapper = new ObjectMapper();
	mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
	mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
	mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	// To allow empty beans --> i.e. Ticket
	mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	return mapper;
    }

    @Bean
    @Qualifier("onlyIndentOutput")
    public ObjectMapper objectMapperOnlyIndentOutput() {
	ObjectMapper mapper = new ObjectMapper();
	mapper.enable(SerializationFeature.INDENT_OUTPUT);
	mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	// To allow empty beans --> i.e. Ticket
	mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	return mapper;
    }

}
