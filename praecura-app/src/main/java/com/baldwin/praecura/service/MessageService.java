package com.baldwin.praecura.service;

import com.baldwin.praecura.config.RequestContext;
import com.baldwin.praecura.config.RequestContextHolder;
import com.baldwin.praecura.entity.Appointment;
import com.baldwin.praecura.entity.MessageChannel;
import com.baldwin.praecura.entity.MessageLog;
import com.baldwin.praecura.entity.MessageStatus;
import com.baldwin.praecura.entity.Patient;
import com.baldwin.praecura.repository.AppointmentRepository;
import com.baldwin.praecura.repository.MessageLogRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

  private final MessageLogRepository messageLogRepository;
  private final AppointmentRepository appointmentRepository;
  private final AuditService auditService;

  @Value("${praecura.messaging.enabled:false}")
  private boolean messagingEnabled;

  @Value("${praecura.messaging.mode:disabled}")
  private String messagingMode;

  public MessageService(MessageLogRepository messageLogRepository,
                        AppointmentRepository appointmentRepository,
                        AuditService auditService) {
    this.messageLogRepository = messageLogRepository;
    this.appointmentRepository = appointmentRepository;
    this.auditService = auditService;
  }

  public MessageResult sendAppointmentReminder(Long appointmentId) {
    Appointment a = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new IllegalArgumentException("Cita no existe"));
    if (!a.isActive()) {
      throw new IllegalArgumentException("La cita está archivada.");
    }
    if (a.getStatus() == null) {
      throw new IllegalArgumentException("La cita no tiene estado definido.");
    }

    Patient p = a.getPatient();
    if (p == null) {
      throw new IllegalArgumentException("La cita no tiene paciente asociado.");
    }

    MessageChannel channel = resolveChannel(p);
    if (channel == null) {
      throw new IllegalArgumentException("El paciente no tiene consentimiento para recibir recordatorios.");
    }

    String to = resolveDestination(p, channel);
    if (to == null || to.isBlank()) {
      throw new IllegalArgumentException("El paciente no tiene contacto válido para el canal seleccionado.");
    }

    String subject = channel == MessageChannel.EMAIL ? "Recordatorio de cita" : null;
    String body = buildReminderBody(a, p);

    MessageStatus status;
    String error = null;

    if (!messagingEnabled || "disabled".equalsIgnoreCase(messagingMode)) {
      status = MessageStatus.SKIPPED;
      error = "Mensajería deshabilitada";
    } else if ("log".equalsIgnoreCase(messagingMode)) {
      status = MessageStatus.SENT;
      error = "Envío simulado (modo log)";
    } else {
      status = MessageStatus.FAILED;
      error = "Proveedor de mensajería no configurado";
    }

    MessageLog log = new MessageLog();
    log.setChannel(channel);
    log.setStatus(status);
    log.setToAddress(to);
    log.setSubject(subject);
    log.setBody(body);
    log.setError(error);
    log.setAppointmentId(a.getId());
    log.setPatientId(p.getId());

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      log.setUsername(auth.getName());
    }

    RequestContext ctx = RequestContextHolder.get();
    if (ctx != null) {
      log.setRequestId(ctx.getRequestId());
      log.setIpAddress(ctx.getIpAddress());
      log.setUserAgent(ctx.getUserAgent());
    }

    messageLogRepository.save(log);

    auditService.log("SEND_MESSAGE",
        "MessageLog",
        log.getId(),
        "channel=" + channel +
            ", status=" + status +
            ", appointmentId=" + a.getId() +
            ", patientId=" + p.getId() +
            ", to=" + to);

    if (status == MessageStatus.SENT || status == MessageStatus.SKIPPED) {
      a.setRemindedAt(LocalDateTime.now());
      appointmentRepository.save(a);
    }

    return new MessageResult(status, channel, to, error);
  }

  private MessageChannel resolveChannel(Patient p) {
    if (p.getPreferredChannel() != null) {
      if (hasConsent(p, p.getPreferredChannel())) return p.getPreferredChannel();
    }
    if (hasConsent(p, MessageChannel.SMS)) return MessageChannel.SMS;
    if (hasConsent(p, MessageChannel.WHATSAPP)) return MessageChannel.WHATSAPP;
    if (hasConsent(p, MessageChannel.EMAIL)) return MessageChannel.EMAIL;
    return null;
  }

  private boolean hasConsent(Patient p, MessageChannel channel) {
    if (channel == null || p == null) return false;
    return switch (channel) {
      case SMS -> p.isConsentSms();
      case EMAIL -> p.isConsentEmail();
      case WHATSAPP -> p.isConsentWhatsapp();
    };
  }

  private String resolveDestination(Patient p, MessageChannel channel) {
    if (channel == null || p == null) return null;
    return switch (channel) {
      case SMS, WHATSAPP -> p.getPhone();
      case EMAIL -> p.getEmail();
    };
  }

  private String buildReminderBody(Appointment a, Patient p) {
    String doctor = (a.getDoctor() != null && a.getDoctor().getFullName() != null) ? a.getDoctor().getFullName() : "el médico";
    String service = (a.getService() != null && a.getService().getName() != null) ? a.getService().getName() : "servicio";
    String date = a.getScheduledAt() != null
        ? a.getScheduledAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US))
        : "fecha pendiente";
    return "Hola " + p.getFullName() + ", recordatorio de su cita para " + service +
        " con " + doctor + " el " + date + ".";
  }

  public record MessageResult(MessageStatus status, MessageChannel channel, String to, String error) {}
}
