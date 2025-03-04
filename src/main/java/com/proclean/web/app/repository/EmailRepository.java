package com.proclean.web.app.repository;

import com.proclean.web.app.model.Email;
import com.proclean.web.app.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {


    @Query("SELECT DISTINCT e.folder FROM Email e WHERE e.usuario.email = :email")
    List<String> findDistinctFoldersByUserEmail(@Param("email") String email);

    @Query("SELECT e FROM Email e WHERE e.usuario.email = :email AND e.folder = :folder")
    Page<Email> findByUserEmailAndFolder(@Param("email") String email, @Param("folder") String folder, Pageable pageable);

    Page<Email> findByUsuario(Usuario usuario, Pageable pageable);
    // Obtener todos los correos de un usuario específico

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


    Optional<Email> findTopByUsuarioAndFolderOrderByReceivedDateDesc(Usuario usuario, String folderName);

    Optional<Email> findTopByUsuarioAndFolderOrderByUidDesc(Usuario usuario, String folderName);
}
