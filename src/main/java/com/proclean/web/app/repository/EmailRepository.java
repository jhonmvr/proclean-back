package com.proclean.web.app.repository;

import com.proclean.web.app.model.Email;
import com.proclean.web.app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    // Obtener todos los correos de un usuario específico
    List<Email> findByUsuario(Usuario usuario);

    // Obtener correos por usuario y carpeta
    List<Email> findByUsuarioAndFolder(Usuario usuario, String folder);

    // Obtener correos más recientes de un usuario
    List<Email> findTop50ByUsuarioOrderByReceivedDateDesc(Usuario usuario);

    @Query("SELECT COUNT(e) > 0 FROM Email e " +
            "WHERE e.usuario = :usuario " +
            "AND (:folder IS NULL OR e.folder = :folder) " +
            "AND (:subject IS NULL OR e.subject = :subject) " +
            "AND (:fromAddress IS NULL OR e.fromAddress = :fromAddress) " +
            "AND (CAST(:receivedDate AS TIMESTAMP) IS NULL OR e.receivedDate = :receivedDate)")
    boolean existsByUsuarioAndFolderAndSubjectAndFromAddressAndReceivedDate(
            @Param("usuario") Usuario usuario,
            @Param("folder") String folder,
            @Param("subject") String subject,
            @Param("fromAddress") String fromAddress,
            @Param("receivedDate") Date receivedDate
    );





}
