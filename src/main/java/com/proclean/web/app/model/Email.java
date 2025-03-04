package com.proclean.web.app.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "emails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fromAddress; // Dirección del remitente
    private String subject; // Asunto del correo

    @Column(columnDefinition = "TEXT")
    private String content; // Contenido del correo

    private Date receivedDate; // Fecha de recepción del correo

    private String folder; // Carpeta (INBOX, SENT, etc.)

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario; // Relación con el usuario dueño del correo

    @Column(columnDefinition = "TEXT")
    private String attachmentPath;

    @Column(unique = false)  // No debe ser único globalmente, pero sí por usuario y carpeta
    private long uid; // IMAP Unique ID
    @Override
    public String toString() {
        return "Email{" +
                "id=" + id +
                ", fromAddress='" + fromAddress + '\'' +
                ", subject='" + subject + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 50)) + "..." : "null") + '\'' +
                ", receivedDate=" + receivedDate +
                ", folder='" + folder + '\'' +
                ", usuarioId=" + (usuario != null ? usuario.getId() : "null") +
                ", attachmentPath='" + (attachmentPath != null ? attachmentPath : "No Attachments") + '\'' +
                '}';
    }

}
