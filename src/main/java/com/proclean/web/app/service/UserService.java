package com.proclean.web.app.service;


import com.proclean.web.app.model.Usuario;
import com.proclean.web.app.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AESUtil aesUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, AESUtil aesUtil) {
        this.userRepository = userRepository;
        this.aesUtil = aesUtil;
    }

    public void registerUser(String username, String password, String email, String emailPassword) throws Exception {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setEmail(email);
        usuario.setEmailPassword(aesUtil.encrypt(emailPassword));
        userRepository.save(usuario);
    }

    public Optional<Usuario> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<Usuario> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public String getDecryptedEmailPassword(Usuario usuario) throws Exception {
        return aesUtil.decrypt(usuario.getEmailPassword());
    }

    public Optional<Usuario> findById(Long userId) {
        return userRepository.findById(userId);
    }
}
