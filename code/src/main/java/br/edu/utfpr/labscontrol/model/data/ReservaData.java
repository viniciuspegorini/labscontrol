package br.edu.utfpr.labscontrol.model.data;

import br.edu.utfpr.labscontrol.model.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by EdsonGustavo on 29/03/2015.
 */
public interface ReservaData extends JpaRepository<Reserva, Integer> {
}