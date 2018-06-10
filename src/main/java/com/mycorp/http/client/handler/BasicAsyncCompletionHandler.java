package com.mycorp.http.client.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycorp.http.client.exceptions.ZendeskException;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

public class BasicAsyncCompletionHandler<T> extends AsyncCompletionHandler<T> {
    private static final Logger LOG = LoggerFactory.getLogger(BasicAsyncCompletionHandler.class);

    private final Class<T> clazz;
    private final String name;
    private final Class[] typeParams;
    private final ObjectMapper mapper;

    public BasicAsyncCompletionHandler(Class clazz, String name, ObjectMapper mapper, Class... typeParams) {
	this.clazz = clazz;
	this.name = name;
	this.typeParams = typeParams;
	this.mapper = mapper;
    }

    @Override
    public T onCompleted(Response response) throws Exception {
	logResponse(response);
	if (isStatus2xx(response)) {
	    if (typeParams.length > 0) {
		JavaType type = mapper.getTypeFactory().constructParametricType(clazz, typeParams);
		return mapper.convertValue(mapper.readTree(response.getResponseBodyAsStream()).get(name), type);
	    }
	    return mapper.convertValue(mapper.readTree(response.getResponseBodyAsStream()).get(name), clazz);
	} else if (isRateLimitResponse(response)) {
	    throw new ZendeskException(response.toString());
	}
	if (response.getStatusCode() == 404) {
	    return null;
	}
	throw new ZendeskException(response.toString());
    }

    @Override
    public void onThrowable(Throwable t) {
	if (t instanceof IOException) {
	    throw new ZendeskException(t);
	} else {
	    super.onThrowable(t);
	}
    }

    private boolean isRateLimitResponse(Response response) {
	return response.getStatusCode() == 429;
    }

    private void logResponse(Response response) throws IOException {
	if (LOG.isDebugEnabled()) {
	    LOG.debug("Response HTTP/{} {}\n{}", response.getStatusCode(), response.getStatusText(),
		    response.getResponseBody());
	}
	if (LOG.isTraceEnabled()) {
	    LOG.trace("Response headers {}", response.getHeaders());
	}
    }

    private boolean isStatus2xx(Response response) {
	return (response.getStatusCode() / 100) == 2;
    }

}
