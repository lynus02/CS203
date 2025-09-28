package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
}
