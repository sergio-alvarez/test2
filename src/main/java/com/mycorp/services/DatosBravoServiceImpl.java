package com.mycorp.services;

import java.text.SimpleDateFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.mycorp.constants.Constants;
import com.mycorp.support.DatosCliente;
import com.mycorp.support.ValueCode;

@Service
public class DatosBravoServiceImpl implements DatosBravoService {
    private static final Logger LOG = LoggerFactory.getLogger(DatosBravoServiceImpl.class);

    /** The rest template. */
    @Autowired
    @Qualifier("restTemplateUTF8")
    private RestTemplate restTemplate;

    private SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public String getDatosBravo(String clientId, List<ValueCode> documentRegisterTypes) {
	StringBuilder datosBravo = new StringBuilder();

	datosBravo.append(Constants.ESCAPED_LINE_SEPARATOR + "Datos recuperados de BRAVO:"
		+ Constants.ESCAPED_LINE_SEPARATOR + Constants.ESCAPED_LINE_SEPARATOR);
	try {
	    // Obtenemos los datos del cliente
	    DatosCliente cliente = restTemplate.getForObject("http://localhost:8080/test-endpoint", DatosCliente.class,
		    clientId);

	    datosBravo.append("TelÃ©fono: ").append(cliente.getGenTGrupoTmk()).append(Constants.ESCAPED_LINE_SEPARATOR);

	    datosBravo.append("Feha de nacimiento: ")
		    .append(formatter.format(formatter.parse(cliente.getFechaNacimiento())))
		    .append(Constants.ESCAPED_LINE_SEPARATOR);

	    for (int i = 0; i < documentRegisterTypes.size(); i++) {
		if ((cliente.getGenCTipoDocumento() != null)
			&& documentRegisterTypes.get(i).getCode().equals(cliente.getGenCTipoDocumento().toString())) {
		    datosBravo.append("Tipo de documento: ").append(documentRegisterTypes.get(i).getValue())
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

	return datosBravo.toString();

    }

}
