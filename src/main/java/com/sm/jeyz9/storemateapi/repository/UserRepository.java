package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.RoleName;
import com.sm.jeyz9.storemateapi.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserByEmail(String email);
    boolean existsUserByEmail(String email);
    boolean existsUserByPhone(String phone);
    @Query("""
    SELECT u FROM User u
    LEFT JOIN u.roles r
    ORDER BY
        CASE r.roleName
            WHEN 'ADMIN' THEN 0
            WHEN 'MODERATOR' THEN 1
            WHEN 'USER' THEN 2
            ELSE 99
        END ASC
    """)
    Page<User> findAllSortedByRole(Pageable pageable);
    @Query("SELECT u FROM User u LEFT JOIN u.roles r WHERE " +
            "(:search IS NULL OR u.name LIKE %:search% OR u.email LIKE %:search%) AND " +
            "(:roleName IS NULL OR r.roleName = :roleName) AND " +
            "(:suspended IS NULL OR u.isSuspended = :suspended)")
    Page<User> findAllWithFilters(
            @Param("search") String search,
            @Param("roleName") RoleName roleName,
            @Param("suspended") Boolean suspended,
            Pageable pageable
    );
}
