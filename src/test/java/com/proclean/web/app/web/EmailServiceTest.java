package com.proclean.web.app.web;

import com.proclean.web.app.model.Usuario;
import com.proclean.web.app.repository.EmailRepository;
import com.proclean.web.app.service.EmailService;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Ignorar stubbings innecesarios
class EmailServiceTest {


    @Mock
    private EmailRepository emailRepository;

    @InjectMocks
    private EmailService emailService;

    private Usuario usuario;
    private Message message;

    @BeforeEach
    void setUp() throws Exception {
        usuario = new Usuario();
        usuario.setId(3L);

        message = mock(Message.class);
        when(message.getSubject()).thenReturn(null); // Simula subject como null
        when(message.getFrom()).thenReturn(new InternetAddress[]{new InternetAddress("jhon romero <desa.jhon.romero@gmail.com>")});
        when(message.getSentDate()).thenReturn(new Date(1708957709000L)); // Mon Feb 26 14:28:29 ECT 2024
        when(message.getContentType()).thenReturn("text/plain");
        Folder mockFolder = mock(Folder.class);
        when(mockFolder.getFullName()).thenReturn("[Gmail]/Borradores");
        when(message.getFolder()).thenReturn(mockFolder);
    }


    @Test
    void emailExists_shouldReturnTrue_whenEmailAlreadyExists() {
        // Simular que el correo ya existe en la base de datos
        when(emailRepository.existsByUsuarioAndFolderAndSubjectAndFromAddressAndReceivedDateAndContent(
                any(Usuario.class), eq("[Gmail]/Borradores"), eq(null), eq("jhon romero <desa.jhon.romero@gmail.com>"), any(Date.class),eq(null)))
                .thenReturn(true);

        boolean exists = emailService.emailExists(usuario, message);

        // Validar que el método devuelve true si el correo ya existe
        assertTrue(exists, "Se esperaba que el correo ya existiera en la base de datos");
    }

    @Test
    void emailExists_shouldReturnFalse_whenEmailDoesNotExist() {
        // Simular que el correo NO existe en la base de datos
        when(emailRepository.existsByUsuarioAndFolderAndSubjectAndFromAddressAndReceivedDateAndContent(
                any(Usuario.class), eq("[Gmail]/Borradores"), eq(null), eq("jhon romero <desa.jhon.romero@gmail.com>"), any(Date.class),anyString()))
                .thenReturn(false);

        boolean exists = emailService.emailExists(usuario, message);

        // Validar que el método devuelve false si el correo no existe
        assertFalse(exists, "Se esperaba que el correo NO existiera en la base de datos");
    }



    @Test
    void emailExists_shouldHandleMessagingException() throws Exception {
        when(message.getFrom()).thenThrow(new MessagingException("Error en el remitente"));

        boolean exists = emailService.emailExists(usuario, message);

        assertFalse(exists, "Se esperaba false cuando hay una excepción en `message.getFrom()`");
    }

}
