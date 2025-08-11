package com.ezlevup.dentalchat.service;

import com.ezlevup.dentalchat.entity.User;
import com.ezlevup.dentalchat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User createCustomer(String nickname) {
        String username = "customer_" + UUID.randomUUID().toString().substring(0, 8);
        
        User customer = new User();
        customer.setUsername(username);
        customer.setNickname(nickname);
        customer.setUserType(User.UserType.CUSTOMER);
        customer.setStatus(User.UserStatus.ONLINE);
        
        return userRepository.save(customer);
    }

    public User createAdmin(String username, String nickname) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다.");
        }
        
        User admin = new User();
        admin.setUsername(username);
        admin.setNickname(nickname);
        admin.setUserType(User.UserType.ADMIN);
        admin.setStatus(User.UserStatus.ONLINE);
        
        return userRepository.save(admin);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<User> findAvailableAdmins() {
        return userRepository.findAvailableAdmins();
    }

    public User updateUserStatus(String username, User.UserStatus status) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        user.setStatus(status);
        user.setLastSeen(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findOnlineAdmins() {
        return userRepository.findByStatus(User.UserStatus.ONLINE)
                .stream()
                .filter(user -> user.getUserType() == User.UserType.ADMIN)
                .toList();
    }

    public void updateLastSeen(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);
    }
}