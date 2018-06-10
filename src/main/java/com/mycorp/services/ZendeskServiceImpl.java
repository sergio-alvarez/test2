package com.mycorp.services;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycorp.constants.Constants;
import com.mycorp.http.client.Zendesk;
import com.mycorp.support.CorreoElectronico;
import com.mycorp.support.MensajeriaService;
import com.mycorp.support.Ticket;
import com.mycorp.support.ValueCode;

import util.datos.UsuarioAlta;

@Service
public class ZendeskServiceImpl implements ZendeskService {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(ZendeskServiceImpl.class);

    @Value("#{envPC['zendesk.ticket']}")
    public String PETICION_ZENDESK = "";

    @Value("#{envPC['zendesk.token']}")
    public String TOKEN_ZENDESK = "";

    @Value("#{envPC['zendesk.url']}")
    public String URL_ZENDESK = "";

    @Value("#{envPC['zendesk.user']}")
    public String ZENDESK_USER = "";

    @Value("#{envPC['cliente.getDatos']}")
    public String CLIENTE_GETDATOS = "";

    @Value("#{envPC['zendesk.error.mail.funcionalidad']}")
    public String ZENDESK_ERROR_MAIL_FUNCIONALIDAD = "";

    @Value("#{envPC['zendesk.error.destinatario']}")
    public String ZENDESK_ERROR_DESTINATARIO = "";

    @Autowired
    @Qualifier("emailService")
    MensajeriaService emailService;

    @Autowired
    ClientService clientService;

    @Autowired
    DatosBravoService datosBravoService;

    @Autowired
    @Qualifier("onlyIndentOutput")
    ObjectMapper mapper;

    @Override
    public String altaTicketZendesk(UsuarioAlta usuarioAlta, String userAgent) {

	StringBuilder clientName = new StringBuilder();
	StringBuilder datosServicio = new StringBuilder();

	// AÃ±ade los datos del formulario
	StringBuilder datosUsuario = fillDatosUsuario(usuarioAlta, userAgent);

	// Obtiene el idCliente de la tarjeta y luego los datosBravo
	String datosBravo = datosBravoService.getDatosBravo(
		clientService.getIdClient(usuarioAlta, clientName, datosServicio), getTiposDocumentosRegistro());

	// creamos el ticket
	String ticket = String.format(PETICION_ZENDESK, clientName.toString(), usuarioAlta.getEmail(),
		datosUsuario.toString() + datosBravo + parseJsonBravo(datosServicio));
	ticket = ticket.replaceAll("[" + Constants.ESCAPED_LINE_SEPARATOR + "]", " ");

	try (Zendesk zendesk = new Zendesk.Builder(URL_ZENDESK).setUsername(ZENDESK_USER).setToken(TOKEN_ZENDESK)
		.build()) {
	    // Ticket
	    Ticket petiZendesk = mapper.readValue(ticket, Ticket.class);
	    zendesk.createTicket(petiZendesk);

	} catch (Exception e) {
	    LOG.error("Error al crear ticket ZENDESK", e);
	    // Send email

	    CorreoElectronico correo = new CorreoElectronico(Long.parseLong(ZENDESK_ERROR_MAIL_FUNCIONALIDAD), "es")
		    .addParam(datosUsuario.toString().replaceAll(Constants.ESCAPE_ER + Constants.ESCAPED_LINE_SEPARATOR,
			    Constants.HTML_BR))
		    .addParam(datosBravo.replaceAll(Constants.ESCAPE_ER + Constants.ESCAPED_LINE_SEPARATOR,
			    Constants.HTML_BR));
	    correo.setEmailA(ZENDESK_ERROR_DESTINATARIO);
	    try {
		emailService.enviar(correo);
	    } catch (Exception ex) {
		LOG.error("Error al enviar mail", ex);
	    }

	}

	return datosUsuario.append(datosBravo).toString();
    }

    public List<ValueCode> getTiposDocumentosRegistro() {
	return Arrays.asList(new ValueCode(), new ValueCode()); // simulacion servicio externo
    }

    /**
     * Returns user data
     * 
     * @param usuarioAlta
     * @param userAgent
     * @return
     */
    private StringBuilder fillDatosUsuario(UsuarioAlta usuarioAlta, String userAgent) {
	StringBuilder datosUsuario = new StringBuilder();

	if (StringUtils.isNotBlank(usuarioAlta.getNumPoliza())) {
	    datosUsuario.append("NÂº de poliza/colectivo: ").append(usuarioAlta.getNumPoliza()).append("/")
		    .append(usuarioAlta.getNumDocAcreditativo()).append(Constants.ESCAPED_LINE_SEPARATOR);
	} else {
	    datosUsuario.append("NÂº tarjeta Sanitas o Identificador: ").append(usuarioAlta.getNumTarjeta())
		    .append(Constants.ESCAPED_LINE_SEPARATOR);
	}
	datosUsuario.append("Tipo documento: ").append(usuarioAlta.getTipoDocAcreditativo())
		.append(Constants.ESCAPED_LINE_SEPARATOR);
	datosUsuario.append("NÂº documento: ").append(usuarioAlta.getNumDocAcreditativo())
		.append(Constants.ESCAPED_LINE_SEPARATOR);
	datosUsuario.append("Email personal: ").append(usuarioAlta.getEmail()).append(Constants.ESCAPED_LINE_SEPARATOR);
	datosUsuario.append("NÂº mÃ³vil: ").append(usuarioAlta.getNumeroTelefono())
		.append(Constants.ESCAPED_LINE_SEPARATOR);
	datosUsuario.append("User Agent: ").append(userAgent).append(Constants.ESCAPED_LINE_SEPARATOR);

	return datosUsuario;
    }

    /**
     * MÃ©todo para parsear el JSON de respuesta de los servicios de tarjeta/pÃ³liza
     *
     * @param resBravo
     * @return
     */
    private String parseJsonBravo(StringBuilder resBravo) {
	return resBravo.toString().replaceAll("[\\[\\]\\{\\}\\\"\\r]", "").replaceAll(Constants.ESCAPED_LINE_SEPARATOR,
		Constants.ESCAPE_ER + Constants.ESCAPED_LINE_SEPARATOR);
    }
}