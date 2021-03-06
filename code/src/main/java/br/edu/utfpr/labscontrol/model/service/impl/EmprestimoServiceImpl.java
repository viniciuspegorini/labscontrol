package br.edu.utfpr.labscontrol.model.service.impl;

import br.edu.utfpr.labscontrol.model.data.EmprestimoData;
import br.edu.utfpr.labscontrol.model.entity.Emprestimo;
import br.edu.utfpr.labscontrol.model.framework.CrudService;
import br.edu.utfpr.labscontrol.model.service.EmprestimoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Created by EdsonGustavo on 03/05/2015.
 */
@Service
public class EmprestimoServiceImpl extends CrudService<Emprestimo, Integer> implements EmprestimoService {
    @Autowired
    private EmprestimoData emprestimoData;

    @Override
    protected JpaRepository<Emprestimo, Integer> getData() {
        return emprestimoData;
    }

    @Override
    public List<Emprestimo> findByPendenciasDoSolicitanteId(Integer solicitanteId) {
        return emprestimoData.findByPendenciasDoSolicitanteId(solicitanteId);
    }

    @Override
    public List<Emprestimo> findByDataBetweenAndSolicitante_IdAndEmprestimoItens_Baixado(Date dataInicial, Date dataFinal, Integer solicitanteId, Boolean baixado) {
        return emprestimoData.findByDataBetweenAndSolicitante_IdAndEmprestimoItens_Baixado(dataInicial, dataFinal, solicitanteId, baixado);
    }


    @Override
    public List findBySolicitanteId(Integer id) {
        return null;
    }
}
