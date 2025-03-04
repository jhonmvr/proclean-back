package com.proclean.web.app.controller;


import com.proclean.web.app.model.Email;
import com.proclean.web.app.model.Usuario;
import com.proclean.web.app.service.EmailSenderService;
import com.proclean.web.app.service.EmailService;
import com.proclean.web.app.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/email")
@SecurityRequirement(name = "bearerToken")
public class EmailController {

    private final UserService userService;
    private final EmailService emailService;
    private final EmailSenderService emailSenderService;


    public EmailController(UserService userService, EmailService emailService,EmailSenderService emailSenderService) {
        this.userService = userService;
        this.emailService = emailService;
        this.emailSenderService = emailSenderService;
    }

    /**
     * Endpoint para obtener los correos del usuario autenticado.
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<Email>> getUserEmailsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getAuthenticatedUserId();
        Page<Email> emails = emailService.getEmailsByUserPaginated(userId, page, size);

        return ResponseEntity.ok(emails);
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<Usuario> usuarioOpt = userService.findByUsername(username);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }

        return usuarioOpt.get().getId();
    }
    /**
     * Endpoint para sincronizar correos del usuario autenticado.
     */
    @PostMapping("/sync")
    public ResponseEntity<String> syncUserEmails() {
        Long userId = getAuthenticatedUserId();
        try {
            emailService.syncAllEmails(userId);
            return ResponseEntity.ok("Correos sincronizados exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al sincronizar correos: " + e.getMessage());
        }
    }

    /**
     * Obtiene los correos de una carpeta específica del usuario autenticado.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserEmails(
            @RequestParam(defaultValue = "0") int page,  // Página actual (por defecto 0)
            @RequestParam(defaultValue = "20") int size) { // Tamaño de página (20 por defecto)

        Long userId = getAuthenticatedUserId();
        Page<Email> emailsPage = emailService.getEmailsByUserPaginated(userId, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("emails", emailsPage.getContent());
        response.put("currentPage", emailsPage.getNumber());
        response.put("totalItems", emailsPage.getTotalElements());
        response.put("totalPages", emailsPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkNewEmails/{userId}")
    public ResponseEntity<String> checkNewEmails(@PathVariable Long userId) {
        emailService.checkForNewEmails(userId);
        return ResponseEntity.ok("Verificación de nuevos correos en proceso.");
    }


    /**
     * Obtiene el ID del usuario autenticado desde el token JWT.
     */



    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String body) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            emailSenderService.sendEmail(username, to, subject, body);
            return ResponseEntity.ok(Map.of("mensaje", "Correo enviado exitosamente a " + to));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al enviar el correo: " + e.getMessage()));
        }
    }

    @GetMapping("/by-folder")
    public Page<Email> getEmailsByUserEmailAndFolder(
            @RequestParam String email,
            @RequestParam String folder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return emailService.getEmailsByUserEmailAndFolder(email, folder, page, size);
    }

    @GetMapping("/folders")
    public List<String> getUserFoldersByEmail(@RequestParam String email) {
        return emailService.getUserFoldersByEmail(email);
    }
    @GetMapping("/{id}")
    public Email getEmailById(@PathVariable Long id) {
        return emailService.getEmailById(id);
    }
}
