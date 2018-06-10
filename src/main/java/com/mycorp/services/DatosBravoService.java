package com.mycorp.services;

import java.util.List;

import com.mycorp.support.ValueCode;

public interface DatosBravoService {

    public String getDatosBravo(String clientId, List<ValueCode> documentRegisterTypes);

}
