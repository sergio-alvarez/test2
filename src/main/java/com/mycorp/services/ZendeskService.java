package com.mycorp.services;

import util.datos.UsuarioAlta;

public interface ZendeskService {

    /**
     * Crea un ticket en Zendesk. Si se ha informado el nÂº de tarjeta, obtiene los
     * datos asociados a dicha tarjeta de un servicio externo.
     * 
     * @param usuarioAlta
     * @param userAgent
     */
    public String altaTicketZendesk(UsuarioAlta usuarioAlta, String userAgent);
}
