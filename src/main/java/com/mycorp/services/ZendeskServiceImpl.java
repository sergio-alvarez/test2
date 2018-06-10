package com.mycorp.services;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycorp.constants.Constants;
import com.mycorp.http.client.Zendesk;
import com.mycorp.support.CorreoElectronico;
import com.mycorp.support.DatosCliente;
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

    private SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

    /** The rest template. */
    @Autowired
    @Qualifier("restTemplateUTF8")
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("emailService")
    MensajeriaService emailService;

    @Autowired
    ClientService clientService;

    @Autowired
    @Qualifier("onlyIndentOutput")
    private ObjectMapper mapper;

    @Override
    public String altaTicketZendesk(UsuarioAlta usuarioAlta, String userAgent) {

	StringBuilder datosUsuario = new StringBuilder();
	StringBuilder datosBravo = new StringBuilder();

	StringBuilder clientName = new StringBuilder();

	// AÃ±ade los datos del formulario
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

	datosBravo.append(Constants.ESCAPED_LINE_SEPARATOR + "Datos recuperados de BRAVO:"
		+ Constants.ESCAPED_LINE_SEPARATOR + Constants.ESCAPED_LINE_SEPARATOR);
	StringBuilder datosServicio = new StringBuilder();

	// Obtiene el idCliente de la tarjeta
	String idCliente = clientService.getIdClient(usuarioAlta, clientName, datosServicio);

	try {
	    // Obtenemos los datos del cliente
	    DatosCliente cliente = restTemplate.getForObject("http://localhost:8080/test-endpoint", DatosCliente.class,
		    idCliente);

	    datosBravo.append("TelÃ©fono: ").append(cliente.getGenTGrupoTmk()).append(Constants.ESCAPED_LINE_SEPARATOR);

	    datosBravo.append("Feha de nacimiento: ")
		    .append(formatter.format(formatter.parse(cliente.getFechaNacimiento())))
		    .append(Constants.ESCAPED_LINE_SEPARATOR);

	    List<ValueCode> tiposDocumentos = getTiposDocumentosRegistro();
	    for (int i = 0; i < tiposDocumentos.size(); i++) {
		if (tiposDocumentos.get(i).getCode().equals(cliente.getGenCTipoDocumento().toString())) {
		    datosBravo.append("Tipo de documento: ").append(tiposDocumentos.get(i).getValue())
			    .append(Constants.ESCAPED_LINE_SEPARATOR);
		}
	    }
	    datosBravo.append("NÃºmero documento: ").append(cliente.getNumeroDocAcred())
		    .append(Constants.ESCAPED_LINE_SEPARATOR);

	    datosBravo.append("Tipo cliente: ");
	    switch (cliente.getGenTTipoCliente()) {
	    case 1:
		datosBravo.append("POTENCIAL").append(Constants.ESCAPED_LINE_SEPARATOR);
		break;
	    case 2:
		datosBravo.append("REAL").append(Constants.ESCAPED_LINE_SEPARATOR);
		break;
	    case 3:
		datosBravo.append("PROSPECTO").append(Constants.ESCAPED_LINE_SEPARATOR);
		break;
	    }

	    datosBravo.append("ID estado del cliente: ").append(cliente.getGenTStatus())
		    .append(Constants.ESCAPED_LINE_SEPARATOR);

	    datosBravo.append("ID motivo de alta cliente: ").append(cliente.getIdMotivoAlta())
		    .append(Constants.ESCAPED_LINE_SEPARATOR);

	    datosBravo.append("Registrado: ").append((cliente.getfInactivoWeb() == null ? "SÃ­" : "No"))
		    .append(Constants.ESCAPED_LINE_SEPARATOR + Constants.ESCAPED_LINE_SEPARATOR);

	} catch (Exception e) {
	    LOG.error("Error al obtener los datos en BRAVO del cliente", e);
	}

	String ticket = String.format(PETICION_ZENDESK, clientName.toString(), usuarioAlta.getEmail(),
		datosUsuario.toString() + datosBravo.toString() + parseJsonBravo(datosServicio));
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
		    .addParam(datosBravo.toString().replaceAll(Constants.ESCAPE_ER + Constants.ESCAPED_LINE_SEPARATOR,
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