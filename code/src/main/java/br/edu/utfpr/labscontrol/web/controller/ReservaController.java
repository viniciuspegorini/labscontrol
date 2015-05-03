package br.edu.utfpr.labscontrol.web.controller;

import br.edu.utfpr.labscontrol.model.entity.*;
import br.edu.utfpr.labscontrol.model.framework.ICrudService;
import br.edu.utfpr.labscontrol.model.service.*;
import br.edu.utfpr.labscontrol.web.exceptions.IllegalHorarioException;
import br.edu.utfpr.labscontrol.web.exceptions.ReservaException;
import br.edu.utfpr.labscontrol.web.framework.CrudController;
import javafx.scene.paint.Material;
import org.exolab.castor.types.DateTime;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.ScheduleEntryMoveEvent;
import org.primefaces.event.ScheduleEntryResizeEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultScheduleEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleEvent;
import org.primefaces.model.ScheduleModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import javax.faces.application.FacesMessage;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * Created by EdsonGustavo on 29/03/2015.
 */
@Controller
@Scope("view")
public class ReservaController extends CrudController<Reserva, Integer> {
    @Autowired
    private ReservaService reservaService;
    @Autowired
    private ReservaItemService reservaItemService;
    @Autowired
    private AmbienteService ambienteService;
    @Autowired
    private EquipamentoService equipamentoService;
    @Autowired
    private MaterialDeConsumoService materialDeConsumoService;
    @Autowired
    private PermissaoService permissaoService;

    private ScheduleModel scheduleModel;
    private ScheduleEvent scheduleEvent = new DefaultScheduleEvent();

    private final Calendar calendar = Calendar.getInstance();

    private String tipo;
    private MaterialDeConsumo materialDeConsumo;
    private Equipamento equipamento;
    private BigDecimal quantidade;

    @Override
    protected void inicializar() {
        this.quantidade = BigDecimal.ZERO;
        populaSchedule();
    }

    private void populaSchedule() {
        scheduleModel = new DefaultScheduleModel();
        for (Reserva reserva : reservaService.findAll()) {
            Calendar inicio = Calendar.getInstance();
            inicio.setTime(reserva.getData());
            inicio.set(inicio.get(Calendar.YEAR), inicio.get(Calendar.MONTH), inicio.get(Calendar.DATE), getHour(reserva.getHoraInicio()), getMinute(reserva.getHoraInicio()));

            Calendar fim = Calendar.getInstance();
            fim.setTime(reserva.getData());
            fim.set(fim.get(Calendar.YEAR), fim.get(Calendar.MONTH), fim.get(Calendar.DATE), getHour(reserva.getHoraFim()), getMinute(reserva.getHoraFim()));

            scheduleModel.addEvent(new DefaultScheduleEvent(getTitle(reserva), inicio.getTime(), fim.getTime(), reserva));
        }
    }

    private String getTitle(Reserva reserva) {
        return " até " + getHorario(reserva.getHoraFim()) + " - " + reserva.getAmbiente().getIdentificacao();
    }

    @Override
    protected ICrudService<Reserva, Integer> getService() {
        return reservaService;
    }

    @Override
    protected String getUrlFormPage() {
        return "/pages/reserva/reservaForm.xhtml?faces-redirect=true";
    }

    public List<Equipamento> completeEquipamento(String nome) {
        return equipamentoService.findByNomeContaining(nome);
    }

    public List<MaterialDeConsumo> completeMaterialDeConsumo(String nome) {
        return materialDeConsumoService.findByNomeContaining(nome);
    }

    public List<Ambiente> completeAmbiente(String identificacao) {
        return ambienteService.findByIdentificacaoContaining(identificacao);
    }

    private Usuario getUsuarioLogado() {
        Usuario usuarioLogado = new Usuario();
        SecurityContext context = SecurityContextHolder.getContext();
        if (context instanceof SecurityContext) {
            Authentication authentication = context.getAuthentication();
            if (authentication instanceof Authentication) {
                try {
                    usuarioLogado = (Usuario) authentication.getPrincipal();
                } catch (Exception e) {
                    usuarioLogado.setUsername("Desconhecido");
                }
            }
        }
        return usuarioLogado;
    }

    public void onDateSelect(SelectEvent selectEvent) {
        reset();
        this.entity.setData((Date) selectEvent.getObject());
        this.entity.setUsuario(getUsuarioLogado());
        if (getUsuarioLogado().getPermissoes().contains(this.permissaoService.findByPermissao("ROLE_USER"))) {
            this.entity.setOutroUsuario(getUsuarioLogado().getUsername());
        }
        scheduleEvent = new DefaultScheduleEvent("", (Date) selectEvent.getObject(), (Date) selectEvent.getObject(), this.entity);
    }

    public void onEventSelect(SelectEvent selectEvent) {
        scheduleEvent = (ScheduleEvent) selectEvent.getObject();
        try {
            this.entity = (Reserva) scheduleEvent.getData();
        } catch (Exception e) {
            reset();
        }
    }

    public void addEvent() {
        try {
            validaHorario();
            validaDisponibilidadeDaSalaNoHorario();
            this.entity.setConfirmada(Boolean.FALSE);
            if (scheduleEvent.getId() == null) {
                scheduleModel.addEvent(scheduleEvent);
            } else {
                scheduleModel.updateEvent(scheduleEvent);
            }
            save();
            scheduleEvent = new DefaultScheduleEvent();
            populaSchedule();
        } catch (Exception e) {
            addMessage(e.getMessage(), FacesMessage.SEVERITY_ERROR);
        }
    }

    private void validaDisponibilidadeDaSalaNoHorario() throws ReservaException {
        //TODO verificar que o Confirmada não está funcionando
        List<Reserva> reservas = this.reservaService.findByDataAndHoraInicioBetweenAndHoraFimBetweenAndAmbienteIdAndConfirmada(this.entity.getData(),
                                                                                                                               this.entity.getHoraInicio(),
                                                                                                                               this.entity.getHoraFim(),
                                                                                                                               this.entity.getAmbiente().getId());
        if (!reservas.isEmpty()) {
            throw new ReservaException("Já existe Reserva nessa data, horário e ambiente!");
        }
    }

    private void validaHorario() throws IllegalHorarioException {
        if (this.entity.getHoraInicio().getTime() > this.entity.getHoraFim().getTime()) {
            throw new IllegalHorarioException("Hora de Início deve ser menor ou igual a Hora de Fim!");
        }
    }

    public void addItem() {
        Boolean isAlreadyExistsItem = Boolean.FALSE;
        //Evitar que os registros fiquem duplicados
        for (ReservaItem ri: this.entity.getReservasItens()) {
            if (this.tipo.equals("E")) {
                if (ri.getEquipamento() != null) {
                    if (ri.getEquipamento().getId().equals(this.equipamento.getId())) {
                        isAlreadyExistsItem = Boolean.TRUE;
                        ri.setQuantidade(ri.getQuantidade().add(this.quantidade, MathContext.DECIMAL64));
                        break;
                    }
                }
            } else {
                if (ri.getMaterialDeConsumo() != null) {
                    if (ri.getMaterialDeConsumo().getId().equals(this.materialDeConsumo.getId())) {
                        isAlreadyExistsItem = Boolean.TRUE;
                        ri.setQuantidade(ri.getQuantidade().add(this.quantidade, MathContext.DECIMAL64));
                        break;
                    }
                }
            }
        }
        if (!isAlreadyExistsItem) {
            ReservaItem reservaItem = new ReservaItem();
            reservaItem.setReserva(this.entity);
            reservaItem.setQuantidade(this.quantidade);
            if (this.tipo.equals("E")) {
                reservaItem.setEquipamento(this.equipamento);
            } else {
                reservaItem.setMaterialDeConsumo(this.materialDeConsumo);
            }
            if (this.entity.getReservasItens() == null) {
                this.entity.setReservasItens(new ArrayList<>());
            }
            this.entity.getReservasItens().add(reservaItem);
        }
        this.quantidade = null;
        this.equipamento = null;
        this.materialDeConsumo = null;
    }

    public void excluirItem(ReservaItem reservaItem) {
        this.entity.getReservasItens().remove(reservaItem);
    }

    public void onEdit(RowEditEvent event) {
        ReservaItem reservaItem = (ReservaItem) event.getObject();
        try {
            reservaItemService.save(reservaItem);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String isConfirmada() {
        if (this.entity != null && this.entity.getConfirmada() != null && this.entity.getConfirmada()) {
            return "Confirmada";
        } else {
            return "Não Confirmada";
        }
    }

    public void confirmaReserva() {
        //TODO validar a quantidade dos materiais de consumo conforme existente em estoque, criar um relatório ou exibir em tela quais podem ou não ser gravados, e os que podem já devem ser descontados do estoque
        save();
    }

    public void onEventMove(ScheduleEntryMoveEvent event) {
//        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Event moved", "Day delta:" + event.getDayDelta() + ", Minute delta:" + event.getMinuteDelta());
//        addMessage(message);
    }

    public void onEventResize(ScheduleEntryResizeEvent event) {
        //TODO ao alterar o horario, recalcular e atualizar a reserva
//        event.getScheduleEvent().getData();
//        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Event resized", "Day delta:" + event.getDayDelta() + ", Minute delta:" + event.getMinuteDelta());
//        addMessage(message);
    }

    public ScheduleEvent getScheduleEvent() {
        return scheduleEvent;
    }

    public void setScheduleEvent(ScheduleEvent scheduleEvent) {
        this.scheduleEvent = scheduleEvent;
    }

    public ScheduleModel getScheduleModel() {
        return scheduleModel;
    }

    public void setScheduleModel(ScheduleModel scheduleModel) {
        this.scheduleModel = scheduleModel;
    }

    private Integer getHour(Date date) {
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    private Integer getMinute(Date date){
        calendar.setTime(date);
        return calendar.get(Calendar.MINUTE);
    }

    private String getHorario(Date date) {
        return new SimpleDateFormat("HH:mm").format(date);
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public MaterialDeConsumo getMaterialDeConsumo() {
        return materialDeConsumo;
    }

    public void setMaterialDeConsumo(MaterialDeConsumo materialDeConsumo) {
        this.materialDeConsumo = materialDeConsumo;
    }

    public Equipamento getEquipamento() {
        return equipamento;
    }

    public void setEquipamento(Equipamento equipamento) {
        this.equipamento = equipamento;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }
}
