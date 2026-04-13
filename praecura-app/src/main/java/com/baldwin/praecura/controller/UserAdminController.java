package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.AppUser;
import com.baldwin.praecura.entity.Role;
import com.baldwin.praecura.repository.RoleRepository;
import com.baldwin.praecura.repository.UserRepository;
import com.baldwin.praecura.service.AuditService;
import com.baldwin.praecura.security.PasswordPolicy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserAdminController(UserRepository userRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder,
                               AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("q", q);
        if (q != null && !q.trim().isBlank()) {
            model.addAttribute("users", userRepository.findByUsernameContainingIgnoreCaseOrderByIdAsc(q.trim()));
        } else {
            model.addAttribute("users", userRepository.findAll());
        }
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        AppUser user = new AppUser();
        user.setEnabled(true);
        return showForm(model, user, false, null);
    }

    /**
     * Link principal que usa el listado.
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return showForm(model, user, true, null);
    }

    /**
     * Compatibilidad: si alguien navega a /admin/users/{id}, redirige al formulario de edición.
     */
    @GetMapping("/{id}")
    public String editFormLegacy(@PathVariable Long id) {
        return "redirect:/admin/users/" + id + "/edit";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("user") AppUser formUser,
                       @RequestParam(name = "password", required = false) String password,
                       @RequestParam(name = "roleName", required = false) String roleName,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        boolean isNew = (formUser.getId() == null);

        // NOTA: en modo edición NO permitimos cambiar el username (solo password/rol/estado).
        // Esto evita falsos positivos de "ya existe" y protege la integridad.
        String username = normalizeUsername(formUser.getUsername());
        if (isNew) {
            if (username.isBlank()) {
                return showForm(model, formUser, false, "El nombre de usuario es obligatorio.");
            }
        }

        String normalizedRole = normalizeRoleName(roleName);
        if (normalizedRole.isBlank()) {
            return showForm(model, attachRoleToFormUser(formUser, null), !isNew, "Debes seleccionar un rol.");
        }

        Role role = roleRepository.findByName(normalizedRole)
                .orElse(null);
        if (role == null) {
            return showForm(model, attachRoleToFormUser(formUser, null), !isNew, "Rol inválido.");
        }

        if (isNew) {
            // Validación rápida antes de golpear el UNIQUE constraint.
            if (userRepository.findByUsername(username).isPresent()) {
                return showForm(model, attachRoleToFormUser(formUser, role), false,
                        "Ya existe un usuario con ese nombre. Usa otro.");
            }

            if (password == null || password.isBlank()) {
                return showForm(model, attachRoleToFormUser(formUser, role), false,
                        "Debes indicar una contraseña para el nuevo usuario.");
            }
            String policyError = PasswordPolicy.validate(password);
            if (policyError != null) {
                return showForm(model, attachRoleToFormUser(formUser, role), false, policyError);
            }
        }

        AppUser target;
        if (isNew) {
            target = new AppUser();
        } else {
            target = userRepository.findById(formUser.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }

        if (isNew) {
            target.setUsername(username);
        }

        // Protección del superadmin: no se puede desactivar, eliminar ni cambiar rol.
        if (!isNew && target.isSuperAdmin()) {
            if (!formUser.isEnabled()) {
                return showForm(model, attachRoleToFormUser(formUser, target.getRole()), true,
                        "El usuario admin del sistema no puede desactivarse.");
            }
            // Si intentan cambiar el rol, rechazamos.
            if (role != null && target.getRole() != null && role.getId() != null
                    && target.getRole().getId() != null
                    && !role.getId().equals(target.getRole().getId())) {
                return showForm(model, attachRoleToFormUser(formUser, target.getRole()), true,
                        "El rol del usuario admin del sistema no puede modificarse.");
            }

            // Fuerza valores seguros
            target.setEnabled(true);
            target.setFailedLoginAttempts(0);
            target.setLockedUntil(null);
        } else {
            target.setEnabled(formUser.isEnabled());
            target.setRole(role);
        }

        // Solo se actualiza el hash si vino una contraseña.
        if (password != null && !password.isBlank()) {
            String policyError = PasswordPolicy.validate(password);
            if (policyError != null) {
                return showForm(model, attachRoleToFormUser(formUser, role), !isNew, policyError);
            }
            target.setPasswordHash(passwordEncoder.encode(password));
        } else if (isNew) {
            // En teoría ya se validó arriba, pero lo dejamos por seguridad.
            return showForm(model, attachRoleToFormUser(formUser, role), false,
                    "Debes indicar una contraseña para el nuevo usuario.");
        }

        try {
            userRepository.save(target);
        } catch (DataIntegrityViolationException ex) {
            return showForm(model, attachRoleToFormUser(formUser, role), !isNew,
                    "No se pudo guardar el usuario. Verifica que el nombre de usuario sea único.");
        }

        // Auditoría
        if (isNew) {
            auditService.log("CREATE", "User", target.getId(),
                    "Creó el usuario '" + target.getUsername() + "' (rol=" + role.getName() + ", activo=" + (target.isEnabled() ? "Sí" : "No") + ").");
        } else {
            auditService.log("UPDATE", "User", target.getId(),
                    "Actualizó el usuario '" + target.getUsername() + "' (rol=" + role.getName() + ", activo=" + (target.isEnabled() ? "Sí" : "No") + ").");
        }

        redirectAttributes.addFlashAttribute("success", isNew ? "Usuario creado." : "Usuario actualizado.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggleEnabled(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        Optional<AppUser> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/admin/users";
        }

        AppUser u = opt.get();
        boolean willDisable = u.isEnabled();

        if (u.isSuperAdmin()) {
            redirectAttributes.addFlashAttribute("error",
                    "El usuario admin del sistema no puede ser activado/desactivado.");
            return "redirect:/admin/users";
        }

        if (principal != null && principal.getName() != null
                && principal.getName().equalsIgnoreCase(u.getUsername())
                && willDisable) {
            redirectAttributes.addFlashAttribute("error",
                    "No puedes desactivar tu propio usuario mientras estás autenticado.");
            return "redirect:/admin/users";
        }

        u.setEnabled(!u.isEnabled());
        userRepository.save(u);

        auditService.log(u.isEnabled() ? "ACTIVATE" : "DEACTIVATE", "User", u.getId(),
                (u.isEnabled() ? "Activó" : "Desactivó") + " el usuario '" + u.getUsername() + "'.");

        redirectAttributes.addFlashAttribute("success",
                u.isEnabled() ? "Usuario activado." : "Usuario desactivado.");
        return "redirect:/admin/users";
    }



    
    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        AppUser u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (u.isSuperAdmin()) {
            redirectAttributes.addFlashAttribute("error", "El usuario admin del sistema no puede resetearse desde aquí.");
            return "redirect:/admin/users";
        }

        String temp = generateTempPassword();
        u.setPasswordHash(passwordEncoder.encode(temp));
        u.setForcePasswordChange(true);
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
        u.setEnabled(true);
        userRepository.save(u);

        auditService.log("RESET_PASSWORD", "User", u.getId(),
                "Generó contraseña temporal para el usuario '" + u.getUsername() + "' y forzó cambio al próximo acceso.");

        redirectAttributes.addFlashAttribute("success", "Contraseña temporal generada para '" + u.getUsername() + "'.");
        redirectAttributes.addFlashAttribute("tempPassword", temp);
        redirectAttributes.addFlashAttribute("tempPasswordUser", u.getUsername());
        return "redirect:/admin/users";
    }

    private static String generateTempPassword() {
        // Cumple política: min 10, mayúscula, minúscula, número, símbolo
        String base = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        int n = (int) (Math.random() * 90) + 10;
        String[] symbols = new String[] {"!", "@", "#", "$", "%", "&", "?"};
        String sym = symbols[new SecureRandom().nextInt(symbols.length)];
        return "Tmp-" + base + "-" + n + sym + "A";
    }

@PostMapping("/{id}/unlock")
    public String unlockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<AppUser> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/admin/users";
        }

        AppUser u = opt.get();
        if (u.isSuperAdmin()) {
            // Seguridad: el superadmin nunca debe quedar bloqueado.
            u.setEnabled(true);
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
            userRepository.save(u);
            redirectAttributes.addFlashAttribute("success", "El usuario admin siempre está activo y sin bloqueo.");
            return "redirect:/admin/users";
        }
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
        userRepository.save(u);

        auditService.log("UNLOCK", "User", u.getId(), "Desbloqueó el usuario '" + u.getUsername() + "' (reinició intentos fallidos).");
        redirectAttributes.addFlashAttribute("success", "Usuario desbloqueado.");
        return "redirect:/admin/users";
    }
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        Optional<AppUser> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/admin/users";
        }

        AppUser u = opt.get();
        if (u.isSuperAdmin()) {
            redirectAttributes.addFlashAttribute("error", "El usuario admin del sistema no puede eliminarse.");
            return "redirect:/admin/users";
        }
        if (principal != null && principal.getName() != null
                && principal.getName().equalsIgnoreCase(u.getUsername())) {
            redirectAttributes.addFlashAttribute("error",
                    "No puedes eliminar tu propio usuario mientras estás autenticado.");
            return "redirect:/admin/users";
        }

        auditService.log("DELETE", "User", u.getId(), "Eliminó el usuario '" + u.getUsername() + "'.");
        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Usuario eliminado.");
        return "redirect:/admin/users";
    }

    private String showForm(Model model, AppUser user, boolean isEdit, String errorMsg) {
        List<Role> roles = roleRepository.findAll();
        model.addAttribute("roles", roles);
        model.addAttribute("mode", isEdit ? "edit" : "create");
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("user", user);
        model.addAttribute("policyHint", com.baldwin.praecura.security.PasswordPolicy.policyHint());
        if (errorMsg != null && !errorMsg.isBlank()) {
            model.addAttribute("error", errorMsg);
        }
        return "admin/users/form";
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private static String normalizeRoleName(String roleName) {
        return roleName == null ? "" : roleName.trim().toUpperCase();
    }

    private static AppUser attachRoleToFormUser(AppUser formUser, Role role) {
        // Para que el select mantenga la selección cuando hay errores.
        if (formUser != null) {
            formUser.setRole(role);
        }
        return formUser;
    }
}
