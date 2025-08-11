package com.ezlevup.dentalchat.repository;

import com.ezlevup.dentalchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    List<User> findByUserType(User.UserType userType);
    
    List<User> findByStatus(User.UserStatus status);
    
    @Query("SELECT u FROM User u WHERE u.userType = 'ADMIN' AND u.status = 'ONLINE'")
    List<User> findAvailableAdmins();
    
    boolean existsByUsername(String username);
}