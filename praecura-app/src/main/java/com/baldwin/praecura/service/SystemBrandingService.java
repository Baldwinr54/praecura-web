package com.baldwin.praecura.service;

import com.baldwin.praecura.dto.OwnerBrandingForm;
import com.baldwin.praecura.entity.SystemSetting;
import com.baldwin.praecura.repository.SystemSettingRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemBrandingService {

  private static final String K_APP_DISPLAY_NAME = "APP_DISPLAY_NAME";
  private static final String K_APP_TAGLINE = "APP_TAGLINE";
  private static final String K_COMPANY_NAME = "COMPANY_NAME";
  private static final String K_COMPANY_TRADE_NAME = "COMPANY_TRADE_NAME";
  private static final String K_COMPANY_RNC = "COMPANY_RNC";
  private static final String K_COMPANY_ADDRESS = "COMPANY_ADDRESS";
  private static final String K_COMPANY_PHONE = "COMPANY_PHONE";
  private static final String K_COMPANY_EMAIL = "COMPANY_EMAIL";
  private static final String K_INVOICE_FOOTER = "INVOICE_FOOTER";

  private static final List<String> KEYS = List.of(
      K_APP_DISPLAY_NAME,
      K_APP_TAGLINE,
      K_COMPANY_NAME,
      K_COMPANY_TRADE_NAME,
      K_COMPANY_RNC,
      K_COMPANY_ADDRESS,
      K_COMPANY_PHONE,
      K_COMPANY_EMAIL,
      K_INVOICE_FOOTER
  );

  private static final long CACHE_TTL_MS = 300_000L;

  private final SystemSettingRepository systemSettingRepository;

  @Value("${spring.application.name:PraeCura}")
  private String defaultAppDisplayName;

  @Value("${praecura.app.tagline:Gestión Clínica Integral}")
  private String defaultAppTagline;

  @Value("${praecura.company.name:Centro Clínico Integral}")
  private String defaultCompanyName;

  @Value("${praecura.company.trade-name:}")
  private String defaultCompanyTradeName;

  @Value("${praecura.company.rnc:}")
  private String defaultCompanyRnc;

  @Value("${praecura.company.address:}")
  private String defaultCompanyAddress;

  @Value("${praecura.company.phone:}")
  private String defaultCompanyPhone;

  @Value("${praecura.company.email:}")
  private String defaultCompanyEmail;

  @Value("${praecura.billing.invoice-footer:Gracias por su preferencia}")
  private String defaultInvoiceFooter;

  private volatile BrandingProfile cachedProfile;
  private volatile long cachedAtMillis = 0L;

  public SystemBrandingService(SystemSettingRepository systemSettingRepository) {
    this.systemSettingRepository = systemSettingRepository;
  }

  public BrandingProfile load() {
    long now = System.currentTimeMillis();
    BrandingProfile snapshot = cachedProfile;
    if (snapshot != null && (now - cachedAtMillis) < CACHE_TTL_MS) {
      return snapshot;
    }

    synchronized (this) {
      long again = System.currentTimeMillis();
      if (cachedProfile != null && (again - cachedAtMillis) < CACHE_TTL_MS) {
        return cachedProfile;
      }
      BrandingProfile fresh = loadFromStorage();
      cachedProfile = fresh;
      cachedAtMillis = again;
      return fresh;
    }
  }

  public OwnerBrandingForm toForm() {
    BrandingProfile profile = load();
    OwnerBrandingForm form = new OwnerBrandingForm();
    form.setAppDisplayName(profile.appDisplayName());
    form.setAppTagline(profile.appTagline());
    form.setCompanyName(profile.companyName());
    form.setCompanyTradeName(profile.companyTradeName());
    form.setCompanyRnc(profile.companyRnc());
    form.setCompanyAddress(profile.companyAddress());
    form.setCompanyPhone(profile.companyPhone());
    form.setCompanyEmail(profile.companyEmail());
    form.setInvoiceFooter(profile.invoiceFooter());
    return form;
  }

  @Transactional
  public void save(OwnerBrandingForm form, String actorUsername) {
    upsert(K_APP_DISPLAY_NAME, form.getAppDisplayName(), actorUsername);
    upsert(K_APP_TAGLINE, form.getAppTagline(), actorUsername);
    upsert(K_COMPANY_NAME, form.getCompanyName(), actorUsername);
    upsert(K_COMPANY_TRADE_NAME, form.getCompanyTradeName(), actorUsername);
    upsert(K_COMPANY_RNC, form.getCompanyRnc(), actorUsername);
    upsert(K_COMPANY_ADDRESS, form.getCompanyAddress(), actorUsername);
    upsert(K_COMPANY_PHONE, form.getCompanyPhone(), actorUsername);
    upsert(K_COMPANY_EMAIL, form.getCompanyEmail(), actorUsername);
    upsert(K_INVOICE_FOOTER, form.getInvoiceFooter(), actorUsername);

    cachedProfile = null;
    cachedAtMillis = 0L;
  }

  private void upsert(String key, String rawValue, String actorUsername) {
    String value = normalize(rawValue);
    SystemSetting setting = systemSettingRepository.findById(key).orElseGet(SystemSetting::new);
    setting.setSettingKey(key);
    setting.setSettingValue(value);
    setting.setUpdatedBy(normalize(actorUsername));
    setting.setUpdatedAt(LocalDateTime.now());
    systemSettingRepository.save(setting);
  }

  private BrandingProfile loadFromStorage() {
    Map<String, SystemSetting> map = systemSettingRepository.findBySettingKeyIn(KEYS).stream()
        .collect(Collectors.toMap(SystemSetting::getSettingKey, Function.identity()));

    return new BrandingProfile(
        valueOrDefault(map, K_APP_DISPLAY_NAME, defaultAppDisplayName),
        valueOrDefault(map, K_APP_TAGLINE, defaultAppTagline),
        valueOrDefault(map, K_COMPANY_NAME, defaultCompanyName),
        valueOrDefault(map, K_COMPANY_TRADE_NAME, defaultCompanyTradeName),
        valueOrDefault(map, K_COMPANY_RNC, defaultCompanyRnc),
        valueOrDefault(map, K_COMPANY_ADDRESS, defaultCompanyAddress),
        valueOrDefault(map, K_COMPANY_PHONE, defaultCompanyPhone),
        valueOrDefault(map, K_COMPANY_EMAIL, defaultCompanyEmail),
        valueOrDefault(map, K_INVOICE_FOOTER, defaultInvoiceFooter)
    );
  }

  private String valueOrDefault(Map<String, SystemSetting> map, String key, String fallback) {
    SystemSetting setting = map.get(key);
    String value = setting != null ? normalize(setting.getSettingValue()) : null;
    if (value == null) {
      return normalize(fallback);
    }
    return value;
  }

  private String normalize(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public record BrandingProfile(
      String appDisplayName,
      String appTagline,
      String companyName,
      String companyTradeName,
      String companyRnc,
      String companyAddress,
      String companyPhone,
      String companyEmail,
      String invoiceFooter
  ) {
  }
}
