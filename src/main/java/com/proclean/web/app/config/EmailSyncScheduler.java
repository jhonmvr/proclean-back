package com.proclean.web.app.config;

import com.proclean.web.app.model.Usuario;
import com.proclean.web.app.repository.UserRepository;
import com.proclean.web.app.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailSyncScheduler {

    private final Logger log = LoggerFactory.getLogger(EmailSyncScheduler.class);

    private final EmailService emailService;
    private final UserRepository userRepository;

    public EmailSyncScheduler(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    /**
     * Programa la sincronización de todos los correos cada 30 minutos.
     * Se ejecutará de forma asíncrona.
     */
    @Scheduled(fixedRate = 1800000) // Cada 30 minutos (1800000 ms)
    @Async("asyncTaskExecutor")
    public void syncAllUsersEmails() {
        log.info("Iniciando sincronización de correos para todos los usuarios...");

        List<Usuario> usuarios = userRepository.findAll();
        for (Usuario usuario : usuarios) {
            log.info("Sincronizando correos para el usuario: {}", usuario.getEmail());
            try {
                emailService.syncAllEmails(usuario.getId());
            } catch (Exception e) {
                log.error("Error al sincronizar correos para el usuario {}: {}", usuario.getEmail(), e.getMessage());
            }
        }

        log.info("Sincronización de correos completada.");
    }
}
