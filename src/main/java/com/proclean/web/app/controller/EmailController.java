package com.proclean.web.app.controller;


import com.proclean.web.app.model.Email;
import com.proclean.web.app.model.Usuario;
import com.proclean.web.app.service.EmailSenderService;
import com.proclean.web.app.service.EmailService;
import com.proclean.web.app.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping
    public ResponseEntity<List<Email>> getUserEmails() {
        Long userId = getAuthenticatedUserId();
        List<Email> emails = emailService.getEmailsByUser(userId);
        return ResponseEntity.ok(emails);
    }

    /**
     * Endpoint para sincronizar correos del usuario autenticado.
     */
    @PostMapping("/sync")
    public ResponseEntity<String> syncUserEmails() throws Exception {
        Long userId = getAuthenticatedUserId();
        emailService.syncAllEmails(userId);
        return ResponseEntity.ok("Correos sincronizados exitosamente.");
    }

    /**
     * Obtiene los correos de una carpeta específica del usuario autenticado.
     */
    @GetMapping("/folder/{folderName}")
    public ResponseEntity<List<Email>> getUserEmailsByFolder(@PathVariable String folderName) {
        Long userId = getAuthenticatedUserId();
        Optional<Usuario> usuarioOpt = userService.findById(userId);

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Email> emails = emailService.getEmailsByFolder(usuarioOpt.get(), folderName);
        return ResponseEntity.ok(emails);
    }

    /**
     * Obtiene el ID del usuario autenticado desde el token JWT.
     */
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<Usuario> usuarioOpt = userService.findByUsername(username);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }

        return usuarioOpt.get().getId();
    }


    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(@RequestParam String to, @RequestParam String subject, @RequestParam String body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            emailSenderService.sendEmail(username, to, subject, body);
            return ResponseEntity.ok().body(Map.of("mensaje", "Correo enviado exitosamente a " + to));

        } catch (Exception e) {
            return ResponseEntity.ok().body(Map.of("mensaje", "Error al enviar el correo: " + e.getMessage()));

        }
    }
}
