package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.Role;
import com.lynus.cs203.entities.UserRole;
import com.lynus.cs203.entities.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    boolean existsByUserUserIdAndRole(String userId, Role role);

    void deleteByUserUserIdAndRole(String userId, Role role);

    List<UserRole> findByUserUserId(String userId);

    boolean existsByRole(Role role);
}
