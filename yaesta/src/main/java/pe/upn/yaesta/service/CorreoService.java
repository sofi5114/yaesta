package pe.upn.yaesta.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import pe.upn.yaesta.model.EstadoRecordatorio;
import pe.upn.yaesta.model.Usuario;
import pe.upn.yaesta.repository.RecordatorioRepository;

import java.time.format.DateTimeFormatter;

import org.springframework.transaction.event.TransactionalEventListener;

import pe.upn.yaesta.dto.TurnoLlamado;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorreoService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private final JavaMailSender correo;
    private final RecordatorioRepository recordatorios;

    @Value("${yaesta.correo.remitente}")
    private String remitente;

    @Value("${yaesta.web-url}")
    private String webUrl;
    @Value("${yaesta.correo.habilitado:false}")
    private boolean habilitado;

    @Transactional
    public void enviarRecordatorio(Long recordatorioId) {
        var recordatorio = recordatorios.bloquearParaEnvio(
                recordatorioId,
                EstadoRecordatorio.PENDIENTE,
                LocalDate.now(ZONA)).orElse(null);

        if (recordatorio == null) {
            return;
        }

        var turno = recordatorio.getTurno();
        Usuario cliente = turno.getCliente();

        if (cliente == null
                || !cliente.isActivo()
                || !cliente.isAceptaRecordatorios()) {

            recordatorio.cancelar();
            return;
        }

        var negocio = turno.getNegocio();
        var servicio = turno.getServicio();

        SimpleMailMessage mensaje = new SimpleMailMessage();

        mensaje.setFrom(remitente);
        mensaje.setTo(cliente.getCorreo());
        mensaje.setSubject("YAESTÁ · Tu próxima visita");

        mensaje.setText("""
                Hola %s.

                Ya llegó la fecha sugerida para volver a %s
                por el servicio de %s.

                Si deseas volver, consulta el negocio y su disponibilidad:
                %s/negocios/%d

                Este mensaje es un recordatorio de visita.
                Los turnos se atienden por orden de llegada.

                Equipo YAESTÁ
                """.formatted(
                cliente.getNombre(),
                negocio.getNombre(),
                servicio.getNombre(),
                webUrl,
                negocio.getId()));

        recordatorio.iniciarEnvio();

        try {
            correo.send(mensaje);

            recordatorio.marcarEnviado(
                    LocalDateTime.now(ZONA));
        } catch (MailException error) {
            recordatorio.marcarError();

            log.warn(
                    "Falló el envío del recordatorio {}",
                    recordatorioId);
        }
    }

    @TransactionalEventListener
    public void enviarAvisoTurno(TurnoLlamado aviso) {
        if (!habilitado) {
            return;
        }

        var formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        SimpleMailMessage mensaje = new SimpleMailMessage();

        mensaje.setFrom(remitente);
        mensaje.setTo(aviso.correo());
        mensaje.setSubject("YAESTÁ · Te están llamando");

        mensaje.setText("""
                Hola %s.

                %s está llamando tu turno número %d.

                Si todavía no llegaste, preséntate antes de:
                %s, hora de Perú.

                Si ya estás en el local, acércate al personal.

                Consulta tu turno:
                %s/turnos/%d

                Equipo YAESTÁ
                """.formatted(
                aviso.nombreCliente(),
                aviso.negocio(),
                aviso.turnoId(),
                aviso.limiteLlegada().format(formato),
                webUrl,
                aviso.turnoId()));

        try {
            correo.send(mensaje);
        } catch (MailException error) {
            log.warn(
                    "No se pudo enviar el aviso del turno {}",
                    aviso.turnoId());
        }
    }
}
