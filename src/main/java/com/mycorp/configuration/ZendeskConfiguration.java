package com.mycorp.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
@ComponentScan(basePackages = { "com.mycorp.*" })
public class ZendeskConfiguration {

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
