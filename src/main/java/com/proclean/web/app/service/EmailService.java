package com.proclean.web.app.service;

import com.proclean.web.app.model.Email;
import com.proclean.web.app.model.Usuario;
import com.proclean.web.app.repository.EmailRepository;
import com.proclean.web.app.repository.UserRepository;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);


    private final Map<Long, Message> pendingMessages = new ConcurrentHashMap<>();

    @Value("${mail.imap.host}")
    private String imapHost;

    @Value("${mail.imap.port}")
    private String imapPort;

    @Value("${mail.store.protocol}")
    private String protocol;

    @Autowired
    private EmailRepository emailRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private AESUtil aesUtil; // Para desencriptar contraseñas almacenadas

    /**
     * Sincroniza los correos de todas las carpetas de un usuario específico.
     */
    /**
     * Sincroniza los correos de todas las carpetas de un usuario en un solo proceso.
     */
    public void syncAllEmails(Long userId) throws Exception {
        Optional<Usuario> usuarioOpt = userRepository.findById(userId);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }

        String email = usuarioOpt.get().getEmail();
        String decryptedPassword = userService.getDecryptedEmailPassword(usuarioOpt.get());

        Properties properties = new Properties();
        properties.put("mail.store.protocol", protocol);
        properties.put("mail.imap.host", imapHost);
        properties.put("mail.imap.port", imapPort);

        try {
            Session session = Session.getDefaultInstance(properties);
            Store store = session.getStore(protocol);
            store.connect(imapHost, email, decryptedPassword);

            Folder[] folders = store.getDefaultFolder().list("*");

            for (Folder folder : folders) {
                log.info("Folder: {}", folder.getName());
                if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
                    folder.open(Folder.READ_ONLY);
                    syncEmailsFromFolder(usuarioOpt.get(), folder);
                    folder.close(false);
                }
            }

            store.close();
        } catch (Exception e) {
            log.error("Error al sincronizar correos: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }


    /**
     * Sincroniza los correos de una carpeta específica sin procesar adjuntos.
     */
    private void syncEmailsFromFolder(Usuario usuario, Folder folder) throws Exception {
        Message[] messages = folder.getMessages();
        log.info("Total messages: {}", messages.length);
        int recuentos = 0;
        for (Message message : messages) {

            if (!emailExists(usuario, message)) {

                recuentos=recuentos+1;
                Email emailEntity = new Email();
                emailEntity.setUsuario(usuario);
                emailEntity.setFromAddress(message.getFrom()[0].toString());
                emailEntity.setSubject(message.getSubject());
                emailEntity.setReceivedDate(message.getSentDate());
                emailEntity.setContent(getTextFromMessage(message));
                emailEntity.setFolder(folder.getFullName());
                emailEntity.setAttachmentPath("");

                log.info("Nuevo messages: {}", emailEntity.toString());
                emailEntity = emailRepository.save(emailEntity);

                // Guardamos el mensaje en el mapa temporal
                pendingMessages.put(emailEntity.getId(), message);
            }
        }
        log.info("Total recuentos: {}", recuentos);

        // Ejecutamos la tarea en segundo plano para los adjuntos
        processPendingAttachmentsAsync();
    }

    // Proceso en segundo plano para descargar los adjuntos
    @Async("asyncTaskExecutor")
    public void processPendingAttachmentsAsync() {
        log.info("Procesando adjuntos en segundo plano para correos pendientes...");

        for (Map.Entry<Long, Message> entry : pendingMessages.entrySet()) {
            Long emailId = entry.getKey();
            Message message = entry.getValue();

            try {
                processAttachmentsQueue(emailId, message);
                pendingMessages.remove(emailId);
            } catch (Exception e) {
                log.error("Error al procesar adjuntos para email ID {}: {}", emailId, e.getMessage());
            }
        }

        log.info("Procesamiento de adjuntos completado.");
    }

    @Async("asyncTaskExecutor")
    public void processAttachmentsQueue(Long emailId, Message message) {
        try {
            log.info("Procesando adjuntos en segundo plano para email ID: {} en hilo: {}", emailId, Thread.currentThread().getName());


            // Buscar el correo en la base de datos
            Optional<Email> emailOpt = emailRepository.findById(emailId);
            if (emailOpt.isEmpty()) {
                log.warn("Email ID {} no encontrado en la base de datos, omitiendo adjuntos.", emailId);
                return;
            }

            Email emailEntity = emailOpt.get();

            // Procesar los adjuntos
            String attachmentPaths = saveAttachments(message);
            emailEntity.setAttachmentPath(attachmentPaths);

            // Actualizar el registro en la base de datos con las rutas de los adjuntos
            emailRepository.save(emailEntity);

            log.info("Adjuntos actualizados en la base de datos para email ID: {}", emailId);
        } catch (Exception e) {
            log.error("Error al procesar adjuntos para el email ID {}: {}", emailId, e.getMessage());
        }
    }


    /**
     * Método asíncrono para procesar y guardar adjuntos en paralelo.
     */
    @Async("asyncTaskExecutor")
    public void processAttachmentsAsync(Email emailEntity, Message message) {
        try {
            log.info("Procesando adjuntos para el email ID: {}", emailEntity.getId());

            String attachmentPaths = saveAttachments(message);
            emailEntity.setAttachmentPath(attachmentPaths);
            emailRepository.save(emailEntity);

            log.info("Adjuntos procesados para el email ID: {}", emailEntity.getId());
        } catch (Exception e) {
            log.error("Error al procesar adjuntos para el email ID {}: {}", emailEntity.getId(), e.getMessage());
        }
    }

    /**
     * Descarga y guarda los adjuntos.
     */
    private String saveAttachments(Message message) throws Exception {
        if (!message.isMimeType("multipart/*")) {
            return "";
        }

        MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
        StringBuilder attachmentPaths = new StringBuilder();
        String uploadDir = "uploads/attachments/";

        Files.createDirectories(Paths.get(uploadDir));

        for (int i = 0; i < mimeMultipart.getCount(); i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);

            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                    bodyPart.getFileName() != null) {

                String fileName = bodyPart.getFileName();
                String filePath = uploadDir + System.currentTimeMillis() + "_" + fileName;
                saveFile(bodyPart.getInputStream(), filePath);

                if (!attachmentPaths.isEmpty()) {
                    attachmentPaths.append(";");
                }
                attachmentPaths.append(filePath);
            }
        }

        return attachmentPaths.toString();
    }


    private void saveFile(InputStream inputStream, String filePath) throws Exception {
        try (FileOutputStream outputStream = new FileOutputStream(new File(filePath))) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * Verifica si un correo ya está almacenado en la base de datos.
     */
    private boolean emailExists(Usuario usuario, Message message) {
        try {
            String messageSubject = message.getSubject();
            String messageFrom = (message.getFrom() != null && message.getFrom().length > 0) ? message.getFrom()[0].toString() : "";
            Date messageDate = message.getSentDate();
            String messageFolder = message.getFolder() != null ? message.getFolder().getFullName() : "";

            return emailRepository.existsByUsuarioAndFolderAndSubjectAndFromAddressAndReceivedDate(
                    usuario, messageFolder, messageSubject, messageFrom, messageDate
            );
        } catch (MessagingException e) {
            log.error("Error al verificar si el correo existe: {}", e.getMessage());
            return false;
        }

    }



    /**
     * Extrae el contenido de un correo electrónico.
     */
    private String getTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            return getTextFromMimeMultipart(mimeMultipart);
        }
        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < mimeMultipart.getCount(); i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                return bodyPart.getContent().toString();
            }
        }
        return result.toString();
    }

    /**
     * Devuelve los correos sincronizados de un usuario.
     */
    public List<Email> getEmailsByUser(Long userId) {
        Optional<Usuario> usuario = userRepository.findById(userId);
        if (usuario.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        Pageable pageable = null;
        return (List<Email>) emailRepository.findByUsuario(usuario.get(), pageable);
    }

    public List<Email> getEmailsByFolder(Usuario usuario, String folderName) {


        return emailRepository.findByUsuarioAndFolder(usuario, folderName);
    }

    public Page<Email> getEmailsByUserPaginated(Long userId, int page, int size) {
        Optional<Usuario> usuario = userRepository.findById(userId);
        if (usuario.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }

        Pageable pageable = PageRequest.of(page, size);
        return (Page<Email>) (Page<Email>) emailRepository.findByUsuario(usuario.get(), pageable);
    }

    
}
