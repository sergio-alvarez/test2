package com.mycorp.services;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycorp.constants.Constants;
import com.mycorp.support.Poliza;
import com.mycorp.support.PolizaBasicoFromPolizaBuilder;

import portalclientesweb.ejb.interfaces.PortalClientesWebEJBRemote;
import util.datos.PolizaBasico;
import util.datos.UsuarioAlta;

@Service
public class ClientServiceImpl implements ClientService {
    private static final Logger LOG = LoggerFactory.getLogger(ClientServiceImpl.class);

    @Autowired
    @Qualifier("onlyIndentOutput")
    private ObjectMapper mapper;

    @Value("#{envPC['tarjetas.getDatos']}")
    public String tarjetaDatos = "";

    /** The rest template. */
    @Autowired
    @Qualifier("restTemplateUTF8")
    private RestTemplate restTemplate;

    /** The portalclientes web ejb remote. */
    @Autowired
    // @Qualifier("portalclientesWebEJB")
    private PortalClientesWebEJBRemote portalclientesWebEJBRemote;

    @Override
    public String getIdClient(UsuarioAlta usuarioAlta, StringBuilder clientName, StringBuilder datosServicio) {
	String idClient = null;
	try {
	    if (StringUtils.isNotBlank(usuarioAlta.getNumTarjeta())) {

		String urlToRead = tarjetaDatos + usuarioAlta.getNumTarjeta();
		ResponseEntity<String> res = restTemplate.getForEntity(urlToRead, String.class);
		if (res.getStatusCode() == HttpStatus.OK) {
		    String dusuario = res.getBody();
		    clientName.append(dusuario);
		    idClient = dusuario;
		    datosServicio.append("Datos recuperados del servicio de tarjeta:")
			    .append(Constants.ESCAPED_LINE_SEPARATOR).append(mapper.writeValueAsString(dusuario));
		}
	    } else if (StringUtils.isNotBlank(usuarioAlta.getNumPoliza())) {
		Poliza poliza = new Poliza();
		poliza.setNumPoliza(Integer.valueOf(usuarioAlta.getNumPoliza()));
		poliza.setNumColectivo(Integer.valueOf(usuarioAlta.getNumDocAcreditativo()));
		poliza.setCompania(1);

		PolizaBasico polizaBasicoConsulta = new PolizaBasicoFromPolizaBuilder().withPoliza(poliza).build();

		final util.datos.DetallePoliza detallePolizaResponse = portalclientesWebEJBRemote
			.recuperarDatosPoliza(polizaBasicoConsulta);

		System.out.println(detallePolizaResponse);

		clientName.append(detallePolizaResponse.getTomador().getNombre()).append(" ")
			.append(detallePolizaResponse.getTomador().getApellido1()).append(" ")
			.append(detallePolizaResponse.getTomador().getApellido2());

		idClient = detallePolizaResponse.getTomador().getIdentificador();
		datosServicio.append("Datos recuperados del servicio de tarjeta:")
			.append(Constants.ESCAPED_LINE_SEPARATOR)
			.append(mapper.writeValueAsString(detallePolizaResponse));
	    }
	} catch (Exception e) {
	    LOG.error("Error al obtener los datos de la tarjeta", e);
	}
	return idClient;

    }

}
