package com.baldwin.praecura.config;

import com.baldwin.praecura.entity.Appointment;
import com.baldwin.praecura.entity.AppointmentStatus;
import com.baldwin.praecura.entity.AllergySeverity;
import com.baldwin.praecura.entity.AllergyStatus;
import com.baldwin.praecura.entity.AuditLog;
import com.baldwin.praecura.entity.ClinicalAllergy;
import com.baldwin.praecura.entity.ClinicalCondition;
import com.baldwin.praecura.entity.ClinicalMedication;
import com.baldwin.praecura.entity.ClinicalNote;
import com.baldwin.praecura.entity.ClinicalVital;
import com.baldwin.praecura.entity.ClinicResource;
import com.baldwin.praecura.entity.ClinicSite;
import com.baldwin.praecura.entity.ConditionStatus;
import com.baldwin.praecura.entity.CashSession;
import com.baldwin.praecura.entity.CashSessionStatus;
import com.baldwin.praecura.entity.Doctor;
import com.baldwin.praecura.entity.FiscalSequence;
import com.baldwin.praecura.entity.Invoice;
import com.baldwin.praecura.entity.InvoiceItem;
import com.baldwin.praecura.entity.InvoiceStatus;
import com.baldwin.praecura.entity.MedicalService;
import com.baldwin.praecura.entity.MedicationStatus;
import com.baldwin.praecura.entity.MessageChannel;
import com.baldwin.praecura.entity.MessageLog;
import com.baldwin.praecura.entity.MessageStatus;
import com.baldwin.praecura.entity.MetricsDaily;
import com.baldwin.praecura.entity.Patient;
import com.baldwin.praecura.entity.PatientConsentLog;
import com.baldwin.praecura.entity.ConsentType;
import com.baldwin.praecura.entity.Payment;
import com.baldwin.praecura.entity.PaymentChannel;
import com.baldwin.praecura.entity.PaymentLink;
import com.baldwin.praecura.entity.PaymentLinkProvider;
import com.baldwin.praecura.entity.PaymentLinkStatus;
import com.baldwin.praecura.entity.PaymentMethod;
import com.baldwin.praecura.entity.PaymentStatus;
import com.baldwin.praecura.entity.ResourceType;
import com.baldwin.praecura.entity.Specialty;
import com.baldwin.praecura.entity.TriageLevel;
import com.baldwin.praecura.repository.AppointmentRepository;
import com.baldwin.praecura.repository.AuditLogRepository;
import com.baldwin.praecura.repository.CashSessionRepository;
import com.baldwin.praecura.repository.ClinicResourceRepository;
import com.baldwin.praecura.repository.ClinicSiteRepository;
import com.baldwin.praecura.repository.ClinicalAllergyRepository;
import com.baldwin.praecura.repository.ClinicalConditionRepository;
import com.baldwin.praecura.repository.ClinicalMedicationRepository;
import com.baldwin.praecura.repository.ClinicalNoteRepository;
import com.baldwin.praecura.repository.ClinicalVitalRepository;
import com.baldwin.praecura.repository.DoctorRepository;
import com.baldwin.praecura.repository.FiscalSequenceRepository;
import com.baldwin.praecura.repository.InvoiceRepository;
import com.baldwin.praecura.repository.MedicalServiceRepository;
import com.baldwin.praecura.repository.MessageLogRepository;
import com.baldwin.praecura.repository.MetricsDailyRepository;
import com.baldwin.praecura.repository.PatientConsentLogRepository;
import com.baldwin.praecura.repository.PatientRepository;
import com.baldwin.praecura.repository.PaymentLinkRepository;
import com.baldwin.praecura.repository.PaymentRepository;
import com.baldwin.praecura.repository.SpecialtyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeds a realistic dataset for demos/testing.
 *
 * Opt-in only. Enable with:
 *   PRAECURA_DEMO_SEED=true
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DemoDataSeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

  private static final String[] FIRST_NAMES = {
      "Ana", "Luis", "Carlos", "María", "José", "Laura", "Pedro", "Sofía", "Miguel", "Valentina",
      "Juan", "Isabella", "Andrés", "Camila", "Diego", "Lucía", "Jorge", "Gabriela", "Rafael", "Paula",
      "Héctor", "Daniela", "Ricardo", "Elena", "Fernando", "Patricia", "Eduardo", "Marta", "Roberto", "Natalia"
  };

  private static final java.util.Set<String> FEMALE_NAMES = java.util.Set.of(
      "Ana", "María", "Laura", "Sofía", "Valentina", "Isabella", "Camila", "Lucía", "Gabriela",
      "Paula", "Daniela", "Elena", "Patricia", "Marta", "Natalia"
  );

  private static final String[] LAST_NAMES = {
      "García", "Rodríguez", "Martínez", "Hernández", "Pérez", "Sánchez", "Gómez", "Díaz", "Vargas", "Castillo",
      "Reyes", "Morales", "Jiménez", "Navarro", "Torres", "Ramírez", "Cruz", "Rojas", "Flores", "Gutiérrez",
      "Almonte", "Mejía", "Guerrero", "Pimentel", "Matos", "Báez", "Polanco", "Peralta", "Cordero", "Santana"
  };

  private static final String[] SERVICE_BASE = {
      "Consulta general",
      "Consulta pediátrica",
      "Consulta cardiológica",
      "Consulta de medicina interna",
      "Consulta endocrinológica",
      "Consulta nefrológica",
      "Consulta neurológica",
      "Consulta reumatológica",
      "Consulta neumológica",
      "Consulta dermatológica",
      "Consulta gastroenterológica",
      "Consulta urológica",
      "Consulta oftalmológica",
      "Consulta otorrinolaringológica",
      "Consulta traumatológica",
      "Consulta psiquiátrica",
      "Consulta psicológica",
      "Consulta ginecológica",
      "Consulta obstétrica",
      "Consulta de medicina familiar",
      "Consulta geriátrica",
      "Consulta de medicina preventiva",
      "Consulta de medicina laboral",
      "Consulta de nutrición",
      "Consulta de nutrición deportiva",
      "Consulta de rehabilitación",
      "Consulta de dolor",
      "Consulta de cardiología preventiva",
      "Consulta de medicina del sueño",
      "Consulta de alergología",
      "Consulta de infectología",
      "Consulta de hematología",
      "Consulta de oncología",
      "Consulta de cirugía general",
      "Consulta de cirugía vascular",
      "Control prenatal",
      "Control posoperatorio",
      "Evaluación preoperatoria",
      "Evaluación cardiovascular",
      "Evaluación nutricional",
      "Evaluación psicológica",
      "Evaluación funcional",
      "Chequeo anual",
      "Chequeo ejecutivo",
      "Toma de presión arterial",
      "Electrocardiograma",
      "Ecocardiograma",
      "Holter 24 horas",
      "MAPA 24 horas",
      "Espirometría",
      "Ultrasonido abdominal",
      "Ultrasonido pélvico",
      "Rayos X",
      "Curación de heridas",
      "Sutura simple",
      "Retiro de puntos",
      "Nebulización",
      "Lavado ótico",
      "Papanicolau",
      "Colposcopía",
      "Vacunación",
      "Toma de muestras de laboratorio",
      "Infiltración articular",
      "Terapia física",
      "Terapia respiratoria",
      "Terapia ocupacional",
      "Consulta de oftalmología pediátrica",
      "Consulta de neumología pediátrica",
      "Consulta de cardiología pediátrica",
      "Consulta de gastroenterología pediátrica",
      "Consulta de neurología pediátrica",
      "Consulta de endocrinología pediátrica",
      "Consulta de nefrología pediátrica",
      "Consulta de inmunología",
      "Evaluación del desarrollo infantil",
      "Consulta de clínica del dolor",
      "Consulta de salud mental",
      "Evaluación de riesgo cardiovascular",
      "Evaluación de pie diabético",
      "Control de anticoagulación",
      "Consulta de medicina estética",
      "Consulta de medicina deportiva",
      "Consulta de medicina física",
      "Seguimiento de crónicos",
      "Programa de cesación tabáquica",
      "Consejería nutricional",
      "Consejería en lactancia",
      "Consulta de planificación familiar",
      "Consulta de menopausia",
      "Consulta de andrología",
      "Evaluación auditiva básica",
      "Evaluación visual básica",
      "Consulta de salud ocupacional",
      "Consulta de medicina preventiva familiar",
      "Consulta de cuidados paliativos",
      "Consulta de geriatría integral",
      "Consulta de neurología del adulto mayor",
      "Consulta de hepatología",
      "Consulta de endocrinología reproductiva",
      "Consulta de reumatología pediátrica"
  };

  private static final String[] CONSULT_SUFFIX = {
      "inicial", "de seguimiento", "de control", "integral", "prioritaria", "extendida"
  };

  private static final String[] PROCEDURE_SUFFIX = {
      "con informe", "de control", "de rutina", "programada"
  };

  private static final String[] FLAGS = {
      "Alergia a penicilina", "Diabetes controlada", "Hipertensión", "Asma leve",
      "Embarazo de alto riesgo", "Sin alergias conocidas", "Postoperatorio reciente"
  };

  private static final String[] NOTES = {
      "Paciente refiere molestias leves.",
      "Control rutinario sin hallazgos relevantes.",
      "Se recomienda seguimiento en 30 días.",
      "Indicaciones entregadas por escrito.",
      "Se ajustó tratamiento según evolución.",
      "Paciente estable, sin complicaciones.",
      "Se ordenan laboratorios de control."
  };

  private static final String[] REASONS = {
      "Dolor de cabeza", "Chequeo anual", "Control de presión", "Dolor lumbar",
      "Seguimiento de tratamiento", "Revisión de resultados", "Consulta preventiva",
      "Molestias digestivas", "Control pediátrico", "Evaluación inicial"
  };

  private static final String[] ALLERGENS = {
      "Penicilina", "Mariscos", "Polen", "Látex", "Ibuprofeno",
      "Ácaros", "Huevo", "Maní", "Aspirina", "Leche"
  };

  private static final String[] ALLERGY_REACTIONS = {
      "Urticaria", "Dificultad respiratoria", "Rinitis", "Edema leve",
      "Náuseas", "Tos", "Sarpullido", "Congestión", "Mareos", "Sin síntomas graves"
  };

  private static final String[] CONDITIONS = {
      "Hipertensión arterial", "Diabetes tipo 2", "Asma", "Dislipidemia",
      "Gastritis crónica", "Hipotiroidismo", "Artritis", "Migraña",
      "Insomnio", "Ansiedad"
  };

  private static final String[] CONDITION_CODES = {
      "I10", "E11.9", "J45.909", "E78.5",
      "K29.50", "E03.9", "M19.90", "G43.909",
      "G47.00", "F41.9"
  };

  private static final String[] MEDICATIONS = {
      "Losartán", "Metformina", "Amlodipino", "Atorvastatina", "Omeprazol",
      "Levotiroxina", "Salbutamol", "Enalapril", "Paracetamol", "Ibuprofeno"
  };

  private static final String[] MED_FREQ = {
      "1 vez al día", "2 veces al día", "Cada 8 horas", "Cada 12 horas", "Según necesidad"
  };

  private static final String[] NOTE_TITLES = {
      "Evaluación inicial", "Nota de evolución", "Seguimiento clínico",
      "Resultados y plan", "Indicaciones al paciente"
  };

  private static final String[] SITE_NAMES = {
      "Sede Central", "Sede Norte", "Sede Este", "Sede Oeste"
  };

  private static final String[] SITE_CODES = {
      "SC", "SN", "SE", "SO"
  };

  private static final String[] RESOURCE_CONSULTORIOS = {
      "Consulta Medicina General", "Consulta Pediatria", "Consulta Ginecologia", "Consulta Cardiologia",
      "Consulta Medicina Interna", "Consulta Neurologia", "Consulta Endocrinologia", "Consulta Dermatologia"
  };

  private static final String[] RESOURCE_PROCEDIMIENTOS = {
      "Sala de Curas", "Sala de Procedimientos Menores", "Sala de Infiltracion"
  };

  private static final String[] RESOURCE_DIAGNOSTICO = {
      "Area de Diagnostico por Imagen", "Area de Electrocardiografia", "Area de Espirometria"
  };

  private static final String[] RESOURCE_EQUIPOS = {
      "Equipo RX Digital", "Electrocardiografo", "Ultrasonido Doppler", "Monitor Multiparametro"
  };

  private final JdbcTemplate jdbcTemplate;
  private final SpecialtyRepository specialtyRepository;
  private final DoctorRepository doctorRepository;
  private final MedicalServiceRepository medicalServiceRepository;
  private final PatientRepository patientRepository;
  private final AppointmentRepository appointmentRepository;
  private final ClinicSiteRepository clinicSiteRepository;
  private final ClinicResourceRepository clinicResourceRepository;
  private final InvoiceRepository invoiceRepository;
  private final FiscalSequenceRepository fiscalSequenceRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentLinkRepository paymentLinkRepository;
  private final MessageLogRepository messageLogRepository;
  private final MetricsDailyRepository metricsDailyRepository;
  private final AuditLogRepository auditLogRepository;
  private final PatientConsentLogRepository patientConsentLogRepository;
  private final CashSessionRepository cashSessionRepository;
  private final ClinicalAllergyRepository clinicalAllergyRepository;
  private final ClinicalConditionRepository clinicalConditionRepository;
  private final ClinicalMedicationRepository clinicalMedicationRepository;
  private final ClinicalVitalRepository clinicalVitalRepository;
  private final ClinicalNoteRepository clinicalNoteRepository;

  @Value("${praecura.demo.seed:false}")
  private boolean seedEnabled;

  @Value("${praecura.demo.count:50}")
  private int demoCount;

  @Value("${praecura.demo.services:30}")
  private int servicesCountConfig;

  @Value("${praecura.demo.doctors:20}")
  private int doctorsCountConfig;

  @Value("${praecura.demo.patients:100}")
  private int patientsCountConfig;

  @Value("${praecura.demo.appointments:350}")
  private int appointmentsCountConfig;

  @Value("${praecura.demo.sites:4}")
  private int sitesCountConfig;

  @Value("${praecura.billing.currency:DOP}")
  private String defaultCurrency;

  @Value("${praecura.demo.exit:false}")
  private boolean exitAfterSeed;

  private final ApplicationContext applicationContext;

  public DemoDataSeeder(JdbcTemplate jdbcTemplate,
                        ApplicationContext applicationContext,
                        SpecialtyRepository specialtyRepository,
                        DoctorRepository doctorRepository,
                        MedicalServiceRepository medicalServiceRepository,
                        PatientRepository patientRepository,
                        AppointmentRepository appointmentRepository,
                        ClinicSiteRepository clinicSiteRepository,
                        ClinicResourceRepository clinicResourceRepository,
                        InvoiceRepository invoiceRepository,
                        FiscalSequenceRepository fiscalSequenceRepository,
                        PaymentRepository paymentRepository,
                        PaymentLinkRepository paymentLinkRepository,
                        MessageLogRepository messageLogRepository,
                        MetricsDailyRepository metricsDailyRepository,
                        AuditLogRepository auditLogRepository,
                        PatientConsentLogRepository patientConsentLogRepository,
                        CashSessionRepository cashSessionRepository,
                        ClinicalAllergyRepository clinicalAllergyRepository,
                        ClinicalConditionRepository clinicalConditionRepository,
                        ClinicalMedicationRepository clinicalMedicationRepository,
                        ClinicalVitalRepository clinicalVitalRepository,
                        ClinicalNoteRepository clinicalNoteRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.applicationContext = applicationContext;
    this.specialtyRepository = specialtyRepository;
    this.doctorRepository = doctorRepository;
    this.medicalServiceRepository = medicalServiceRepository;
    this.patientRepository = patientRepository;
    this.appointmentRepository = appointmentRepository;
    this.clinicSiteRepository = clinicSiteRepository;
    this.clinicResourceRepository = clinicResourceRepository;
    this.invoiceRepository = invoiceRepository;
    this.fiscalSequenceRepository = fiscalSequenceRepository;
    this.paymentRepository = paymentRepository;
    this.paymentLinkRepository = paymentLinkRepository;
    this.messageLogRepository = messageLogRepository;
    this.metricsDailyRepository = metricsDailyRepository;
    this.auditLogRepository = auditLogRepository;
    this.patientConsentLogRepository = patientConsentLogRepository;
    this.cashSessionRepository = cashSessionRepository;
    this.clinicalAllergyRepository = clinicalAllergyRepository;
    this.clinicalConditionRepository = clinicalConditionRepository;
    this.clinicalMedicationRepository = clinicalMedicationRepository;
    this.clinicalVitalRepository = clinicalVitalRepository;
    this.clinicalNoteRepository = clinicalNoteRepository;
  }

  @Override
  public void run(String... args) {
    if (!seedEnabled) {
      return;
    }
    int baseCount = Math.max(1, demoCount);
    int servicesCount = Math.max(1, servicesCountConfig);
    int doctorsCount = Math.max(1, doctorsCountConfig);
    int patientsCount = Math.max(1, patientsCountConfig);
    int appointmentCount = Math.max(appointmentsCountConfig, Math.max(baseCount * 3, baseCount));
    if (appointmentsCountConfig > 0) {
      appointmentCount = appointmentsCountConfig;
    }
    int invoiceCount = appointmentCount;
    int paymentCount = appointmentCount;
    int linkCount = appointmentCount;
    int messageCount = appointmentCount;
    int metricsCount = Math.max(120, appointmentCount / 2);
    int auditCount = appointmentCount;
    int allergyCount = Math.max(patientsCount + (patientsCount / 2), 120);
    int conditionCount = Math.max(patientsCount + (patientsCount / 3), 110);
    int medicationCount = Math.max(patientsCount + (patientsCount / 2), 130);
    int vitalCount = Math.max(patientsCount * 2, 200);
    int noteCount = Math.max(patientsCount + (patientsCount / 2), 140);
    int siteCount = Math.max(2, Math.min(SITE_NAMES.length, sitesCountConfig));
    int resourceCount = siteCount * 6;

    log.warn("Seeding habilitado. Se eliminarán datos y se crearán:");
    log.warn("  Especialidades: se conservan (NO se tocan)");
    log.warn("  Servicios: {}", servicesCount);
    log.warn("  Médicos: {}", doctorsCount);
    log.warn("  Pacientes: {}", patientsCount);
    log.warn("  Citas: {}", appointmentCount);
    log.warn("  Facturas: {}", invoiceCount);
    log.warn("  Pagos: {}", paymentCount);
    log.warn("  Links de pago: {}", linkCount);
    log.warn("  Mensajes: {}", messageCount);
    log.warn("  Métricas diarias: {}", metricsCount);
    log.warn("  Auditoría: {}", auditCount);
    log.warn("  Alergias clínicas: {}", allergyCount);
    log.warn("  Condiciones clínicas: {}", conditionCount);
    log.warn("  Medicaciones clínicas: {}", medicationCount);
    log.warn("  Signos vitales: {}", vitalCount);
    log.warn("  Notas clínicas: {}", noteCount);
    log.warn("  Sedes: {}", siteCount);
    log.warn("  Recursos: {}", resourceCount);

    truncateDomainTables();

    List<Specialty> specialties = specialtyRepository.findAll();
    seedFiscalSequences();
    List<MedicalService> services = seedServices(servicesCount);
    List<Doctor> doctors = seedDoctors(doctorsCount, specialties, services);
    List<Patient> patients = seedPatients(patientsCount);
    seedConsentLogs(patients);
    CashSession cashSession = seedCashSession();
    seedClinicalAllergies(allergyCount, patients);
    seedClinicalConditions(conditionCount, patients);
    seedClinicalMedications(medicationCount, patients);
    seedClinicalVitals(vitalCount, patients);
    seedClinicalNotes(noteCount, patients);
    List<ClinicSite> sites = seedSites(siteCount);
    List<ClinicResource> resources = seedResources(sites);
    List<Appointment> appointments = seedAppointments(appointmentCount, doctors, patients, services, sites, resources);
    List<Invoice> invoices = seedInvoices(invoiceCount, appointments);
    seedPayments(paymentCount, invoices, cashSession);
    seedPaymentLinks(linkCount, invoices);
    seedMessageLogs(messageCount, appointments, patients);
    seedMetrics(metricsCount);
    seedAuditLogs(auditCount);

    log.warn("Seeding finalizado correctamente.");

    if (exitAfterSeed) {
      log.warn("Saliendo después del seeding (praecura.demo.exit=true)...");
      int code = SpringApplication.exit(applicationContext, () -> 0);
      System.exit(code);
    }
  }

  private void truncateDomainTables() {
    jdbcTemplate.execute(
        "TRUNCATE TABLE " +
            "clinical_allergies, clinical_conditions, clinical_medications, clinical_vitals, clinical_notes, " +
            "cash_sessions, payment_links, payments, invoice_items, invoices, message_logs, audit_logs, patient_consent_logs, " +
            "appointments, clinic_resources, clinic_sites, doctor_services, doctors, patients, medical_services, metrics_daily " +
            "RESTART IDENTITY CASCADE"
    );
  }

  private void seedFiscalSequences() {
    if (fiscalSequenceRepository.count() > 0) return;
    List<FiscalSequence> sequences = new ArrayList<>();
    sequences.add(buildSequence("B01", "Consumidor final", 1, 99999999L, 8));
    sequences.add(buildSequence("B02", "Crédito fiscal", 1, 99999999L, 8));
    sequences.add(buildSequence("B04", "Nota de crédito", 1, 99999999L, 8));
    sequences.add(buildSequence("E31", "Factura electrónica", 1, 99999999L, 8));
    fiscalSequenceRepository.saveAll(sequences);
  }

  private FiscalSequence buildSequence(String typeCode, String description, long start, Long end, int length) {
    FiscalSequence seq = new FiscalSequence();
    seq.setTypeCode(typeCode);
    seq.setDescription(description);
    seq.setStartNumber(start);
    seq.setNextNumber(start);
    seq.setEndNumber(end);
    seq.setNumberLength(length);
    seq.setActive(true);
    seq.setCreatedBy("seed");
    seq.setUpdatedBy("seed");
    return seq;
  }

  private List<MedicalService> seedServices(int count) {
    List<MedicalService> services = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String base = SERVICE_BASE[i % SERVICE_BASE.length];
      int cycle = i / SERVICE_BASE.length;
      String name = base;
      if (cycle > 0) {
        String suffix = isConsultBase(base)
            ? CONSULT_SUFFIX[(cycle - 1) % CONSULT_SUFFIX.length]
            : PROCEDURE_SUFFIX[(cycle - 1) % PROCEDURE_SUFFIX.length];
        name = base + " " + suffix;
      }

      MedicalService s = new MedicalService();
      s.setName(name);
      s.setDurationMinutes(durationFor(i));
      s.setPrice(priceFor(i));
      s.setActive(true);
      services.add(s);
    }
    return medicalServiceRepository.saveAll(services);
  }

  private boolean isConsultBase(String base) {
    String t = base == null ? "" : base.trim().toLowerCase();
    return t.startsWith("consulta")
        || t.startsWith("control")
        || t.startsWith("evaluación")
        || t.startsWith("chequeo")
        || t.startsWith("seguimiento")
        || t.startsWith("consejería")
        || t.startsWith("programa");
  }

  private List<Doctor> seedDoctors(int count, List<Specialty> specialties, List<MedicalService> services) {
    List<Doctor> doctors = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Doctor d = new Doctor();
      String personName = fullName(i + 5000);
      String title = FEMALE_NAMES.contains(firstName(personName)) ? "Dra." : "Dr.";
      d.setFullName(title + " " + personName);
      d.setLicenseNo(String.format("CMD-%05d", 1000 + i));
      d.setPhone(phoneFor(i));
      d.setBufferMinutes(5 + (i % 3) * 5);
      d.setWorkStart(LocalTime.of(8, 0));
      d.setWorkEnd(LocalTime.of(17, 0));
      d.setWorkDays("MON,TUE,WED,THU,FRI");
      d.setActive(true);

      if (!specialties.isEmpty()) {
        Specialty sp = specialties.get(i % specialties.size());
        d.setSpecialty(sp);
        d.setSpecialtyText(sp.getName());
      } else {
        d.setSpecialtyText("Medicina General");
      }

      Set<MedicalService> offered = new HashSet<>();
      for (int k = 0; k < 3; k++) {
        offered.add(services.get((i + k) % services.size()));
      }
      d.setServices(offered);

      doctors.add(d);
    }
    return doctorRepository.saveAll(doctors);
  }

  private List<Patient> seedPatients(int count) {
    List<Patient> patients = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Patient p = new Patient();
      p.setFullName(fullName(i + 1000));
      p.setPhone(phoneFor(i + 200));
      p.setCedula(String.format("402%08d", 10000000 + i));
      p.setEmail(emailFor(i));

      boolean consentEmail = (i % 2 == 0);
      boolean consentSms = (i % 3 == 0);
      boolean consentWhatsapp = (i % 4 == 0);
      if (!consentEmail && !consentSms && !consentWhatsapp) consentEmail = true;

      p.setConsentEmail(consentEmail);
      p.setConsentSms(consentSms);
      p.setConsentWhatsapp(consentWhatsapp);
      p.setPreferredChannel(preferredChannel(consentEmail, consentSms, consentWhatsapp));
      p.setFlags(FLAGS[i % FLAGS.length]);
      p.setNotes(NOTES[i % NOTES.length]);
      if (i % 4 == 0) {
        p.setBillingName("Centro Médico " + LAST_NAMES[i % LAST_NAMES.length] + " SRL");
        p.setBillingTaxId(String.format("101%06d", 100000 + i));
        p.setBillingAddress("Av. Principal #" + (50 + (i % 200)));
      }
      p.setActive(true);
      patients.add(p);
    }
    return patientRepository.saveAll(patients);
  }

  private CashSession seedCashSession() {
    CashSession session = new CashSession();
    session.setStatus(CashSessionStatus.OPEN);
    session.setOpeningAmount(new BigDecimal("5000.00"));
    session.setOpenedAt(LocalDateTime.now().minusHours(2));
    session.setOpenedBy("admin");
    session.setNotes("Caja inicial del turno");
    return cashSessionRepository.save(session);
  }

  private void seedConsentLogs(List<Patient> patients) {
    if (patients == null || patients.isEmpty()) return;
    List<PatientConsentLog> logs = new ArrayList<>();
    for (Patient p : patients) {
      if (p.isConsentEmail()) {
        logs.add(buildConsentLog(p, ConsentType.EMAIL, true));
      }
      if (p.isConsentSms()) {
        logs.add(buildConsentLog(p, ConsentType.SMS, true));
      }
      if (p.isConsentWhatsapp()) {
        logs.add(buildConsentLog(p, ConsentType.WHATSAPP, true));
      }
    }
    if (!logs.isEmpty()) {
      patientConsentLogRepository.saveAll(logs);
    }
  }

  private PatientConsentLog buildConsentLog(Patient p, ConsentType type, boolean granted) {
    PatientConsentLog log = new PatientConsentLog();
    log.setPatient(p);
    log.setConsentType(type);
    log.setGranted(granted);
    log.setSource("SEED");
    log.setCapturedBy("system");
    log.setNotes("Consentimiento inicial");
    return log;
  }

  private List<ClinicSite> seedSites(int count) {
    List<ClinicSite> sites = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ClinicSite s = new ClinicSite();
      s.setName(SITE_NAMES[i]);
      s.setCode(SITE_CODES[i % SITE_CODES.length]);
      s.setAddress("Av. " + LAST_NAMES[(i * 3) % LAST_NAMES.length] + " #" + (100 + i));
      s.setPhone("809-555-" + String.format("%04d", 2000 + i));
      s.setNotes("Sede habilitada para consultas y procedimientos.");
      s.setActive(true);
      sites.add(s);
    }
    return clinicSiteRepository.saveAll(sites);
  }

  private List<ClinicResource> seedResources(List<ClinicSite> sites) {
    List<ClinicResource> resources = new ArrayList<>();
    if (sites == null || sites.isEmpty()) return resources;

    for (int i = 0; i < sites.size(); i++) {
      ClinicSite site = sites.get(i);

      int consultCount = 2 + (i % 3);
      for (int c = 0; c < consultCount; c++) {
        resources.add(buildResource(site, RESOURCE_CONSULTORIOS[(i + c) % RESOURCE_CONSULTORIOS.length], ResourceType.CONSULTORIO));
      }

      resources.add(buildResource(site, RESOURCE_PROCEDIMIENTOS[i % RESOURCE_PROCEDIMIENTOS.length], ResourceType.SALA_PROCEDIMIENTOS));
      resources.add(buildResource(site, RESOURCE_DIAGNOSTICO[i % RESOURCE_DIAGNOSTICO.length], ResourceType.DIAGNOSTICO));
      resources.add(buildResource(site, RESOURCE_EQUIPOS[i % RESOURCE_EQUIPOS.length], ResourceType.EQUIPO));
    }

    return clinicResourceRepository.saveAll(resources);
  }

  private ClinicResource buildResource(ClinicSite site, String name, ResourceType type) {
    ClinicResource r = new ClinicResource();
    r.setSite(site);
    String code = site != null && site.getCode() != null ? site.getCode() : "SITE";
    r.setName(name + " - " + code);
    r.setType(type);
    r.setNotes("Recurso operativo");
    r.setActive(true);
    return r;
  }

  private List<Appointment> seedAppointments(int count, List<Doctor> doctors, List<Patient> patients, List<MedicalService> services,
                                             List<ClinicSite> sites, List<ClinicResource> resources) {
    List<Appointment> appointments = new ArrayList<>();
    if (doctors.isEmpty() || patients.isEmpty() || services.isEmpty()) {
      return appointments;
    }

    Random rnd = new Random();
    LocalDate base = LocalDate.now().minusDays(90);

    int doctorCount = doctors.size();
    int patientCount = patients.size();
    int serviceCount = services.size();

    int idx = 0;

    // Garantiza al menos una cita por médico
    for (int i = 0; i < doctorCount; i++) {
      Doctor doctor = doctors.get(i);
      Patient patient = patients.get((i * 3) % patientCount);
      MedicalService service = services.get((i * 5) % serviceCount);
      appointments.add(buildAppointment(idx++, doctor, patient, service, base, sites, resources));
    }

    // Garantiza al menos una cita por paciente
    for (int i = 0; i < patientCount; i++) {
      Doctor doctor = doctors.get((i * 7) % doctorCount);
      Patient patient = patients.get(i);
      MedicalService service = services.get((i * 11) % serviceCount);
      appointments.add(buildAppointment(idx++, doctor, patient, service, base, sites, resources));
    }

    // Completa hasta el total requerido con distribución variada
    while (appointments.size() < count) {
      Doctor doctor = doctors.get((idx + rnd.nextInt(doctorCount)) % doctorCount);
      Patient patient = patients.get((idx * 2 + rnd.nextInt(patientCount)) % patientCount);
      MedicalService service = services.get((idx * 3 + rnd.nextInt(serviceCount)) % serviceCount);
      appointments.add(buildAppointment(idx++, doctor, patient, service, base, sites, resources));
    }

    return appointmentRepository.saveAll(appointments);
  }

  private Appointment buildAppointment(int idx, Doctor doctor, Patient patient, MedicalService service, LocalDate base,
                                       List<ClinicSite> sites, List<ClinicResource> resources) {
    LocalDate day = base.plusDays(idx % 120);
    LocalTime time = LocalTime.of(8 + (idx % 9), (idx % 2 == 0) ? 0 : 30);

    Appointment a = new Appointment();
    a.setDoctor(doctor);
    a.setPatient(patient);
    a.setService(service);
    a.setScheduledAt(LocalDateTime.of(day, time));
    a.setDurationMinutes(service.getDurationMinutes());
    a.setStatus(statusFor(idx));
    a.setReason(REASONS[idx % REASONS.length]);
    a.setNotes(NOTES[(idx + 2) % NOTES.length]);
    if (sites != null && !sites.isEmpty()) {
      ClinicSite site = sites.get(idx % sites.size());
      a.setSite(site);
      if (resources != null && !resources.isEmpty()) {
        // Prefer resources from same site
        ClinicResource res = resources.stream().filter(r -> r.getSite() != null && r.getSite().getId().equals(site.getId()))
            .findAny().orElse(resources.get(idx % resources.size()));
        a.setResource(res);
      }
    }
    if (idx % 4 == 0) {
      a.setTriageLevel(TriageLevel.values()[idx % TriageLevel.values().length]);
      a.setTriageNotes("Triage asignado en recepción.");
    }
    if (a.getStatus() == AppointmentStatus.COMPLETADA) {
      LocalDateTime checkIn = a.getScheduledAt().minusMinutes(10);
      a.setCheckedInAt(checkIn);
      a.setStartedAt(a.getScheduledAt());
      a.setCompletedAt(a.getScheduledAt().plusMinutes(a.getDurationMinutes()));
    } else if (a.getStatus() == AppointmentStatus.CONFIRMADA) {
      a.setCheckedInAt(a.getScheduledAt().minusMinutes(5));
    }
    a.setActive(true);
    return a;
  }

  private List<Invoice> seedInvoices(int count, List<Appointment> appointments) {
    List<Invoice> invoices = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Appointment a = appointments.get(i % appointments.size());
      BigDecimal price = priceFor(i);
      String currency = (defaultCurrency == null || defaultCurrency.isBlank()) ? "DOP" : defaultCurrency.trim().toUpperCase();

      Invoice invoice = new Invoice();
      invoice.setPatient(a.getPatient());
      invoice.setAppointment(a);
      invoice.setStatus(InvoiceStatus.ISSUED);
      invoice.setCurrency(currency);
      invoice.setSubtotal(price);
      BigDecimal tax = price.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
      invoice.setTax(tax);
      invoice.setDiscount(BigDecimal.ZERO);
      BigDecimal total = price.add(tax).setScale(2, RoundingMode.HALF_UP);
      invoice.setTotal(total);
      invoice.setBalance(total);
      invoice.setCreatedBy("admin");
      invoice.setUpdatedBy("admin");
      invoice.setIssuedAt(LocalDateTime.now().minusDays(i % 7));

      InvoiceItem item = new InvoiceItem();
      item.setInvoice(invoice);
      item.setService(a.getService());
      item.setAppointment(a);
      item.setDescription(a.getService() != null ? a.getService().getName() : "Servicio");
      item.setQuantity(1);
      item.setUnitPrice(price);
      item.setTotal(price);
      invoice.getItems().add(item);

      invoices.add(invoice);
    }
    return invoiceRepository.saveAll(invoices);
  }

  private void seedPayments(int count, List<Invoice> invoices, CashSession cashSession) {
    List<Payment> payments = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Invoice invoice = invoices.get(i % invoices.size());
      BigDecimal total = invoice.getTotal();

      BigDecimal amount = (i % 5 == 0)
          ? total.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP)
          : total;

      Payment p = new Payment();
      p.setInvoice(invoice);
      p.setPatient(invoice.getPatient());
      p.setAppointment(invoice.getAppointment());
      p.setAmount(amount);
      p.setCurrency(invoice.getCurrency());
      p.setMethod(methodFor(i));
      p.setChannel(channelFor(p.getMethod(), i));
      p.setStatus(PaymentStatus.CAPTURED);
      p.setProvider(p.getMethod() == PaymentMethod.CARD ? "AZUL" : null);
      p.setExternalId("PAY-" + (10000 + i));
      p.setLast4(p.getMethod() == PaymentMethod.CARD ? String.valueOf(1000 + (i % 9000)) : null);
      p.setCardBrand(p.getMethod() == PaymentMethod.CARD ? cardBrandFor(i) : null);
      p.setTerminalId(p.getMethod() == PaymentMethod.CARD ? "POS-" + (100 + (i % 20)) : null);
      p.setBatchId(p.getMethod() == PaymentMethod.CARD ? "BATCH-" + (10 + (i % 5)) : null);
      p.setRrn(p.getMethod() == PaymentMethod.CARD ? "RRN-" + (100000 + i) : null);
      p.setNotes("Pago registrado en recepción");
      p.setPaidAt(LocalDateTime.now().minusDays(i % 5));
      p.setUsername("admin");
      if (p.getMethod() == PaymentMethod.CASH && cashSession != null) {
        p.setCashSession(cashSession);
      }
      payments.add(p);

      BigDecimal newBalance = total.subtract(amount).max(BigDecimal.ZERO);
      invoice.setBalance(newBalance);
      invoice.setUpdatedAt(LocalDateTime.now());
      if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
      } else {
        invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
      }
    }

    invoiceRepository.saveAll(invoices);
    paymentRepository.saveAll(payments);
  }

  private PaymentChannel channelFor(PaymentMethod method, int idx) {
    if (method == null) return PaymentChannel.IN_PERSON;
    return switch (method) {
      case TRANSFER -> PaymentChannel.BANK_TRANSFER;
      case INSURANCE -> PaymentChannel.INSURANCE;
      default -> (idx % 10 == 0 ? PaymentChannel.ONLINE_LINK : PaymentChannel.IN_PERSON);
    };
  }

  private String cardBrandFor(int idx) {
    String[] brands = {"VISA", "MASTERCARD", "AMEX"};
    return brands[idx % brands.length];
  }

  private void seedPaymentLinks(int count, List<Invoice> invoices) {
    List<PaymentLink> links = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Invoice invoice = invoices.get(i % invoices.size());
      PaymentLink link = new PaymentLink();
      link.setInvoice(invoice);
      link.setProvider(linkProviderFor(i));
      link.setStatus(linkStatusFor(i));
      link.setAmount(invoice.getBalance());
      link.setCurrency(invoice.getCurrency());
      link.setUrl("https://pagos.praecura.test/link/" + (10000 + i));
      link.setExternalId("LNK-" + (10000 + i));
      link.setNotes("Link de pago generado por administración");
      link.setCreatedBy("admin");
      link.setUpdatedAt(LocalDateTime.now());
      link.setExpiresAt(LocalDateTime.now().plusDays(7));
      links.add(link);
    }
    paymentLinkRepository.saveAll(links);
  }

  private void seedMessageLogs(int count, List<Appointment> appointments, List<Patient> patients) {
    List<MessageLog> logs = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Appointment a = appointments.get(i % appointments.size());
      Patient p = patients.get(i % patients.size());

      MessageChannel channel = p.getPreferredChannel() != null ? p.getPreferredChannel() : MessageChannel.EMAIL;
      MessageLog log = new MessageLog();
      log.setCreatedAt(LocalDateTime.now().minusHours(i % 24));
      log.setChannel(channel);
      log.setStatus((i % 10 == 0) ? MessageStatus.FAILED : MessageStatus.SENT);
      log.setToAddress(channel == MessageChannel.EMAIL ? p.getEmail() : p.getPhone());
      log.setSubject("Recordatorio de cita");
      log.setBody("Recordatorio: cita programada para " + a.getScheduledAt().toLocalDate());
      log.setAppointmentId(a.getId());
      log.setPatientId(p.getId());
      log.setUsername("admin");
      logs.add(log);
    }
    messageLogRepository.saveAll(logs);
  }

  private void seedClinicalAllergies(int count, List<Patient> patients) {
    if (patients.isEmpty()) return;
    List<ClinicalAllergy> rows = new ArrayList<>();
    Random rnd = new Random();
    int patientCount = patients.size();

    int idx = 0;
    for (Patient p : patients) {
      rows.add(buildAllergy(idx++, p, rnd));
    }
    while (rows.size() < count) {
      Patient p = patients.get((idx + rnd.nextInt(patientCount)) % patientCount);
      rows.add(buildAllergy(idx++, p, rnd));
    }
    clinicalAllergyRepository.saveAll(rows);
  }

  private ClinicalAllergy buildAllergy(int idx, Patient patient, Random rnd) {
    ClinicalAllergy a = new ClinicalAllergy();
    a.setPatient(patient);
    a.setAllergen(ALLERGENS[idx % ALLERGENS.length]);
    a.setReaction(ALLERGY_REACTIONS[(idx + rnd.nextInt(ALLERGY_REACTIONS.length)) % ALLERGY_REACTIONS.length]);
    AllergySeverity[] severities = {AllergySeverity.MILD, AllergySeverity.MODERATE, AllergySeverity.SEVERE};
    a.setSeverity(severities[idx % severities.length]);
    a.setStatus(idx % 10 == 0 ? AllergyStatus.RESOLVED : AllergyStatus.ACTIVE);
    a.setNotes("Historial registrado en admisión.");
    a.setCreatedBy("admin");
    a.setUpdatedBy("admin");
    a.setCreatedAt(LocalDateTime.now().minusDays(idx % 120));
    a.setUpdatedAt(LocalDateTime.now().minusDays(idx % 60));
    return a;
  }

  private void seedClinicalConditions(int count, List<Patient> patients) {
    if (patients.isEmpty()) return;
    List<ClinicalCondition> rows = new ArrayList<>();
    Random rnd = new Random();
    int patientCount = patients.size();
    int idx = 0;

    for (Patient p : patients) {
      rows.add(buildCondition(idx++, p, rnd));
    }
    while (rows.size() < count) {
      Patient p = patients.get((idx + rnd.nextInt(patientCount)) % patientCount);
      rows.add(buildCondition(idx++, p, rnd));
    }
    clinicalConditionRepository.saveAll(rows);
  }

  private ClinicalCondition buildCondition(int idx, Patient patient, Random rnd) {
    ClinicalCondition c = new ClinicalCondition();
    c.setPatient(patient);
    c.setName(CONDITIONS[idx % CONDITIONS.length]);
    c.setIcd10Code(CONDITION_CODES[idx % CONDITION_CODES.length]);
    c.setStatus(idx % 8 == 0 ? ConditionStatus.RESOLVED : ConditionStatus.ACTIVE);
    c.setOnsetDate(LocalDate.now().minusDays(60 + rnd.nextInt(900)));
    c.setNotes("Condición monitoreada periódicamente.");
    c.setCreatedBy("admin");
    c.setUpdatedBy("admin");
    c.setCreatedAt(LocalDateTime.now().minusDays(idx % 200));
    c.setUpdatedAt(LocalDateTime.now().minusDays(idx % 90));
    return c;
  }

  private void seedClinicalMedications(int count, List<Patient> patients) {
    if (patients.isEmpty()) return;
    List<ClinicalMedication> rows = new ArrayList<>();
    Random rnd = new Random();
    int patientCount = patients.size();
    int idx = 0;

    for (Patient p : patients) {
      rows.add(buildMedication(idx++, p, rnd));
    }
    while (rows.size() < count) {
      Patient p = patients.get((idx + rnd.nextInt(patientCount)) % patientCount);
      rows.add(buildMedication(idx++, p, rnd));
    }
    clinicalMedicationRepository.saveAll(rows);
  }

  private ClinicalMedication buildMedication(int idx, Patient patient, Random rnd) {
    ClinicalMedication m = new ClinicalMedication();
    m.setPatient(patient);
    m.setName(MEDICATIONS[idx % MEDICATIONS.length]);
    m.setDosage((5 + (idx % 4) * 5) + " mg");
    m.setFrequency(MED_FREQ[idx % MED_FREQ.length]);
    LocalDate start = LocalDate.now().minusDays(30 + rnd.nextInt(300));
    m.setStartDate(start);
    if (idx % 6 == 0) {
      m.setEndDate(start.plusDays(30 + rnd.nextInt(120)));
      m.setStatus(MedicationStatus.STOPPED);
    } else {
      m.setStatus(MedicationStatus.ACTIVE);
    }
    m.setNotes("Indicaciones entregadas al paciente.");
    m.setCreatedBy("admin");
    m.setUpdatedBy("admin");
    m.setCreatedAt(LocalDateTime.now().minusDays(idx % 150));
    m.setUpdatedAt(LocalDateTime.now().minusDays(idx % 70));
    return m;
  }

  private void seedClinicalVitals(int count, List<Patient> patients) {
    if (patients.isEmpty()) return;
    List<ClinicalVital> rows = new ArrayList<>();
    Random rnd = new Random();
    int patientCount = patients.size();
    int idx = 0;

    for (Patient p : patients) {
      rows.add(buildVital(idx++, p, rnd));
    }
    while (rows.size() < count) {
      Patient p = patients.get((idx + rnd.nextInt(patientCount)) % patientCount);
      rows.add(buildVital(idx++, p, rnd));
    }
    clinicalVitalRepository.saveAll(rows);
  }

  private ClinicalVital buildVital(int idx, Patient patient, Random rnd) {
    ClinicalVital v = new ClinicalVital();
    v.setPatient(patient);
    v.setRecordedAt(LocalDateTime.now().minusDays(idx % 60).withHour(8 + (idx % 9)));
    v.setWeightKg(BigDecimal.valueOf(52 + rnd.nextInt(35) + rnd.nextDouble()).setScale(2, RoundingMode.HALF_UP));
    v.setHeightCm(BigDecimal.valueOf(150 + rnd.nextInt(35) + rnd.nextDouble()).setScale(2, RoundingMode.HALF_UP));
    v.setTemperatureC(BigDecimal.valueOf(36 + rnd.nextDouble() * 1.8).setScale(1, RoundingMode.HALF_UP));
    v.setHeartRate(60 + rnd.nextInt(40));
    v.setRespiratoryRate(12 + rnd.nextInt(8));
    v.setBloodPressure((110 + rnd.nextInt(25)) + "/" + (70 + rnd.nextInt(15)));
    v.setOxygenSaturation(95 + rnd.nextInt(4));
    v.setNotes("Registro de signos vitales.");
    v.setCreatedBy("admin");
    v.setCreatedAt(LocalDateTime.now().minusDays(idx % 60));
    return v;
  }

  private void seedClinicalNotes(int count, List<Patient> patients) {
    if (patients.isEmpty()) return;
    List<ClinicalNote> rows = new ArrayList<>();
    Random rnd = new Random();
    int patientCount = patients.size();
    int idx = 0;

    for (Patient p : patients) {
      rows.add(buildClinicalNote(idx++, p, rnd));
    }
    while (rows.size() < count) {
      Patient p = patients.get((idx + rnd.nextInt(patientCount)) % patientCount);
      rows.add(buildClinicalNote(idx++, p, rnd));
    }
    clinicalNoteRepository.saveAll(rows);
  }

  private ClinicalNote buildClinicalNote(int idx, Patient patient, Random rnd) {
    ClinicalNote n = new ClinicalNote();
    n.setPatient(patient);
    n.setTitle(NOTE_TITLES[idx % NOTE_TITLES.length]);
    n.setNote(NOTES[(idx + rnd.nextInt(NOTES.length)) % NOTES.length]);
    n.setRecordedAt(LocalDateTime.now().minusDays(idx % 30));
    n.setCreatedBy("admin");
    n.setUpdatedBy("admin");
    n.setCreatedAt(LocalDateTime.now().minusDays(idx % 30));
    n.setUpdatedAt(LocalDateTime.now().minusDays(idx % 15));
    return n;
  }

  private void seedMetrics(int count) {
    List<MetricsDaily> rows = new ArrayList<>();
    Random rnd = new Random();
    LocalDate today = LocalDate.now();

    for (int i = 0; i < count; i++) {
      MetricsDaily m = new MetricsDaily();
      m.setDay(today.minusDays(i));
      long total = 20 + rnd.nextInt(40);
      long cancel = rnd.nextInt(6);
      long noShow = rnd.nextInt(5);
      long completed = Math.max(0, total - cancel - noShow - rnd.nextInt(4));
      long confirmadas = Math.max(0, total / 3);
      long pendientes = Math.max(0, total - completed - cancel - noShow);
      long programadas = Math.max(0, pendientes - confirmadas);

      m.setTotal(total);
      m.setCanceladas(cancel);
      m.setNoAsistio(noShow);
      m.setCompletadas(completed);
      m.setConfirmadas(confirmadas);
      m.setPendientes(pendientes);
      m.setProgramadas(programadas);
      double denom = Math.max(1, total - cancel);
      m.setNoShowRate((noShow / denom) * 100.0);
      m.setUpdatedAt(LocalDateTime.now());
      rows.add(m);
    }
    metricsDailyRepository.saveAll(rows);
  }

  private void seedAuditLogs(int count) {
    List<AuditLog> logs = new ArrayList<>();
    String[] actions = {"CREATE", "UPDATE", "DEACTIVATE", "PAYMENT_CAPTURED", "LOGIN_SUCCESS"};
    String[] entities = {"Patient", "Doctor", "Appointment", "Invoice", "Payment"};

    for (int i = 0; i < count; i++) {
      AuditLog a = new AuditLog();
      a.setUsername("admin");
      a.setAction(actions[i % actions.length]);
      a.setEntity(entities[i % entities.length]);
      a.setEntityId((long) (1000 + i));
      a.setDetail("Registro generado automáticamente");
      a.setCreatedAt(LocalDateTime.now().minusDays(i % 14));
      logs.add(a);
    }
    auditLogRepository.saveAll(logs);
  }

  private String fullName(int i) {
    int idx = Math.max(0, i);
    int firstSize = FIRST_NAMES.length;
    int lastSize = LAST_NAMES.length;

    String first = FIRST_NAMES[idx % firstSize];
    int q = idx / firstSize;
    String last1 = LAST_NAMES[q % lastSize];
    String last2 = LAST_NAMES[(q / lastSize + idx) % lastSize];
    if (last1.equals(last2)) {
      last2 = LAST_NAMES[(q / lastSize + idx + 7) % lastSize];
    }
    return first + " " + last1 + " " + last2;
  }

  private String firstName(String fullName) {
    if (fullName == null) {
      return "";
    }
    String trimmed = fullName.trim();
    int space = trimmed.indexOf(' ');
    return space == -1 ? trimmed : trimmed.substring(0, space);
  }

  private String emailFor(int i) {
    String name = fullName(i).toLowerCase()
        .replace(" ", ".")
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
        .replace("ñ", "n");
    return name + "." + (100 + i) + "@praecura.test";
  }

  private String phoneFor(int i) {
    String[] areas = {"809", "829", "849"};
    String area = areas[i % areas.length];
    int mid = 500 + (i % 400);
    int end = 1000 + i;
    return String.format("%s-%03d-%04d", area, mid, end);
  }

  private int durationFor(int i) {
    int[] options = {15, 20, 30, 45, 60};
    return options[i % options.length];
  }

  private BigDecimal priceFor(int i) {
    BigDecimal base = new BigDecimal("650");
    BigDecimal step = new BigDecimal("75");
    return base.add(step.multiply(BigDecimal.valueOf(i % 10))).setScale(2, RoundingMode.HALF_UP);
  }

  private AppointmentStatus statusFor(int i) {
    AppointmentStatus[] statuses = {
        AppointmentStatus.PROGRAMADA,
        AppointmentStatus.CONFIRMADA,
        AppointmentStatus.COMPLETADA,
        AppointmentStatus.CANCELADA,
        AppointmentStatus.NO_ASISTIO
    };
    return statuses[i % statuses.length];
  }

  private MessageChannel preferredChannel(boolean email, boolean sms, boolean whatsapp) {
    if (email) return MessageChannel.EMAIL;
    if (sms) return MessageChannel.SMS;
    if (whatsapp) return MessageChannel.WHATSAPP;
    return MessageChannel.EMAIL;
  }

  private PaymentMethod methodFor(int i) {
    PaymentMethod[] methods = {
        PaymentMethod.CASH,
        PaymentMethod.CARD,
        PaymentMethod.TRANSFER,
        PaymentMethod.INSURANCE,
        PaymentMethod.OTHER
    };
    return methods[i % methods.length];
  }

  private PaymentLinkProvider linkProviderFor(int i) {
    PaymentLinkProvider[] providers = {
        PaymentLinkProvider.AZUL_LINK,
        PaymentLinkProvider.CARDNET_BOTON,
        PaymentLinkProvider.MANUAL
    };
    return providers[i % providers.length];
  }

  private PaymentLinkStatus linkStatusFor(int i) {
    PaymentLinkStatus[] statuses = {
        PaymentLinkStatus.CREATED,
        PaymentLinkStatus.SENT,
        PaymentLinkStatus.PAID,
        PaymentLinkStatus.CANCELLED,
        PaymentLinkStatus.EXPIRED
    };
    return statuses[i % statuses.length];
  }
}
