package com.mycorp.http.client;

import java.io.Closeable;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycorp.http.client.exceptions.ZendeskException;
import com.mycorp.http.client.handler.BasicAsyncCompletionHandler;
import com.mycorp.support.Ticket;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.uri.Uri;

public class Zendesk implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(Zendesk.class);
    private static final Pattern RESTRICTED_PATTERN = Pattern.compile("%2B", Pattern.LITERAL);

    private static final String JSON = "application/json; charset=UTF-8";
    private final AsyncHttpClient client;
    private final Realm realm;
    private final String url;
    private final String oauthToken;
    private boolean closed = false;

    @Autowired
    @Qualifier("zendesk")
    private ObjectMapper mapper;

    private Zendesk(String url, String username, String password) {
	this.oauthToken = null;
	this.client = new AsyncHttpClient();
	this.url = url.endsWith("/") ? url + "api/v2" : url + "/api/v2";
	if (username != null) {
	    this.realm = new Realm.RealmBuilder().setScheme(Realm.AuthScheme.BASIC).setPrincipal(username)
		    .setPassword(password).setUsePreemptiveAuth(true).build();
	} else {
	    if (password != null) {
		throw new IllegalStateException("Cannot specify token or password without specifying username");
	    }
	    this.realm = null;
	}
    }

    public Ticket createTicket(Ticket ticket) {
	return complete(
		submit(req("POST", cnst("/tickets.json"), JSON, json(Collections.singletonMap("ticket", ticket))),
			new BasicAsyncCompletionHandler<Ticket>(Ticket.class, "ticket")));
    }

    private byte[] json(Object object) {
	try {
	    return mapper.writeValueAsBytes(object);
	} catch (JsonProcessingException e) {
	    throw new ZendeskException(e.getMessage(), e);
	}
    }

    private Request req(String method, Uri template, String contentType, byte[] body) {
	RequestBuilder builder = new RequestBuilder(method);
	if (realm != null) {
	    builder.setRealm(realm);
	} else {
	    builder.addHeader("Authorization", "Bearer " + oauthToken);
	}
	builder.setUrl(RESTRICTED_PATTERN.matcher(template.toString()).replaceAll("+")); // replace out %2B with + due
											 // to API restriction
	builder.addHeader("Content-type", contentType);
	builder.setBody(body);
	return builder.build();
    }

    private Uri cnst(String template) {
	return Uri.create(url + template);
    }

    private <T> ListenableFuture<T> submit(Request request, BasicAsyncCompletionHandler<T> handler) {
	if (LOG.isDebugEnabled()) {
	    if (request.getStringData() != null) {
		LOG.debug("Request {} {}\n{}", request.getMethod(), request.getUrl(), request.getStringData());
	    } else if (request.getByteData() != null) {
		LOG.debug("Request {} {} {} {} bytes", request.getMethod(), request.getUrl(),
			request.getHeaders().getFirstValue("Content-type"), request.getByteData().length);
	    } else {
		LOG.debug("Request {} {}", request.getMethod(), request.getUrl());
	    }
	}
	return client.executeRequest(request, handler);
    }

    //////////////////////////////////////////////////////////////////////
    // Closeable interface methods
    //////////////////////////////////////////////////////////////////////

    public boolean isClosed() {
	return closed || client.isClosed();
    }

    @Override
    public void close() {
	if (!client.isClosed()) {
	    client.close();
	}
	closed = true;
    }

    //////////////////////////////////////////////////////////////////////
    // Static helper methods
    //////////////////////////////////////////////////////////////////////

    private static <T> T complete(ListenableFuture<T> future) {
	try {
	    return future.get();
	} catch (InterruptedException e) {
	    throw new ZendeskException(e.getMessage(), e);
	} catch (ExecutionException e) {
	    if (e.getCause() instanceof ZendeskException) {
		throw (ZendeskException) e.getCause();
	    }
	    throw new ZendeskException(e.getMessage(), e);
	}
    }

    public static class Builder {
	private final String url;
	private String username = null;
	private String password = null;
	private String token = null;

	public Builder(String url) {
	    this.url = url;
	}

	public Builder setUsername(String username) {
	    this.username = username;
	    return this;
	}

	public Builder setPassword(String password) {
	    this.password = password;
	    if (password != null) {
		this.token = null;
	    }
	    return this;
	}

	public Builder setToken(String token) {
	    this.token = token;
	    if (token != null) {
		this.password = null;
	    }
	    return this;
	}

	public Zendesk build() {
	    if (token != null) {
		return new Zendesk(url, username + "/token", token);
	    }
	    return new Zendesk(url, username, password);
	}
    }
}