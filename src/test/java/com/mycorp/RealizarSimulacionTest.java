package com.mycorp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.mycorp.configuration.ZendeskConfiguration;
import com.mycorp.services.ZendeskService;
import com.mycorp.support.CorreoElectronico;
import com.mycorp.support.DatosEnvioMail;
import com.mycorp.support.MensajeriaService;
import com.mycorp.support.PolizaBasicoFromPolizaBuilder;
import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.PojoClassFilter;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;

import junit.framework.TestCase;
import portalclientesweb.ejb.interfaces.PortalClientesWebEJBRemote;
import util.datos.DatosPersonales;
import util.datos.DetallePoliza;
import util.datos.PolizaBasico;
import util.datos.UsuarioAlta;

/**
 * Unit test for simple App.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RealizarSimulacionTest.TestConfig.class, ZendeskConfiguration.class })
public class RealizarSimulacionTest extends TestCase {

    @Configuration
    static class TestConfig {
	@Bean
	public Properties envPC() throws IOException {
	    Properties properties = new Properties();
	    properties.load(new ClassPathResource("application.properties").getInputStream());
	    return properties;
	}

	@Bean
	@Qualifier("restTemplateUTF8")
	public RestTemplate restTemplateUTF8() {
	    HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
	    return new RestTemplate(httpRequestFactory);
	}

	@Bean
	public PortalClientesWebEJBRemote portalClienteWebEJBRemote() {
	    return mock(PortalClientesWebEJBRemote.class);
	}

	@Bean
	@Qualifier("emailService")
	public MensajeriaService mensajeriaService() {
	    return mock(MensajeriaService.class);
	}

    }

    // To mock AsyncHttpClient tickets creation
    private ClientAndServer mockServer;

    @Autowired
    ZendeskService zendeskService;

    @Autowired
    @Qualifier("restTemplateUTF8")
    RestTemplate restTemplate;

    @Autowired
    PortalClientesWebEJBRemote ejb;

    @Autowired
    MensajeriaService mensajeriaService;

    @Before
    public void setup() {
	mockServer = startClientAndServer(18080);
    }

    @After
    public void after() {
	mockServer.stop();
    }

    @Test
    public void altaTicket_With_CardNumber() {
	prepareMockRestServiceServerForTwoRequest();

	mockServer.when(request().withMethod("POST").withPath("/zendesk/api/v2/tickets.json")).respond(response()
		.withStatusCode(200).withHeader(new Header("Content-Type", "application/json")).withBody("{}"));

	UsuarioAlta userAlta = new UsuarioAlta();
	userAlta.setNumTarjeta("00001");
	assertNotNull(zendeskService.altaTicketZendesk(userAlta, ""));

    }

    @Test
    public void altaTicket_With_NumPoliza() {
	MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
	server.expect(requestTo("http://localhost:8080/test-endpoint")).andRespond(
		withSuccess("{\"fechaNacimiento\":\"01/01/1980\", \"genTTipoCliente\":1}", MediaType.APPLICATION_JSON));

	mockServer.when(request().withMethod("POST").withPath("/zendesk/api/v2/tickets.json")).respond(response()
		.withStatusCode(200).withHeader(new Header("Content-Type", "application/json")).withBody("{}"));

	// ejb
	DetallePoliza detallePoliza = new DetallePoliza();
	DatosPersonales tomador = new DatosPersonales();
	tomador.setNombre("sergio");
	tomador.setApellido1("alvarez");
	tomador.setApellido2("alvarez");
	tomador.setIdentificador("12345");
	detallePoliza.setTomador(tomador);

	when(ejb.recuperarDatosPoliza(any(PolizaBasico.class))).thenReturn(detallePoliza);

	UsuarioAlta userAlta = new UsuarioAlta();
	userAlta.setNumPoliza("00001");
	userAlta.setNumDocAcreditativo("00001");
	assertNotNull(zendeskService.altaTicketZendesk(userAlta, ""));

    }

    @Test
    public void altaTicket_With_Zendesk_HTTP_RATE_LIMIT_ERROR() {
	prepareMockRestServiceServerForTwoRequest();

	mockServer.when(request().withMethod("POST").withPath("/zendesk/api/v2/tickets.json"))
		.respond(response().withStatusCode(429));

	UsuarioAlta userAlta = new UsuarioAlta();
	userAlta.setNumTarjeta("00001");
	assertNotNull(zendeskService.altaTicketZendesk(userAlta, ""));

	verify(mensajeriaService, atLeastOnce()).enviar(any(CorreoElectronico.class));

    }

    @Test
    public void altaTicket_With_Zendesk_HTTP_SERVER_ERROR() {
	prepareMockRestServiceServerForTwoRequest();

	mockServer.when(request().withMethod("POST").withPath("/zendesk/api/v2/tickets.json"))
		.respond(response().withStatusCode(500));

	UsuarioAlta userAlta = new UsuarioAlta();
	userAlta.setNumTarjeta("00001");
	assertNotNull(zendeskService.altaTicketZendesk(userAlta, ""));

	verify(mensajeriaService, atLeastOnce()).enviar(any(CorreoElectronico.class));

    }

    @Test
    public void test_support_pojos() {
	Validator validator = ValidatorBuilder.create().with(new GetterMustExistRule()).with(new SetterMustExistRule())
		.with(new SetterTester()).with(new GetterTester()).build();

	validator.validate("com.mycorp.support", new PojoClassFilter() {

	    @Override
	    public boolean include(PojoClass pojoClass) {
		// exluyo estos dos pojos que le faltan getters y setters, y uno es un builder
		return !pojoClass.getName().equals(PolizaBasicoFromPolizaBuilder.class.getName())
			&& !pojoClass.getName().equals(DatosEnvioMail.class.getName());
	    }
	});

    }

    private void prepareMockRestServiceServerForTwoRequest() {
	MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
	server.expect(requestTo("http://localhost:8080/user/tarjeta/00001"))
		.andRespond(withSuccess("Sergio", MediaType.APPLICATION_JSON));
	server.expect(requestTo("http://localhost:8080/test-endpoint")).andRespond(
		withSuccess("{\"fechaNacimiento\":\"01/01/1980\", \"genTTipoCliente\":1}", MediaType.APPLICATION_JSON));
    }

}
