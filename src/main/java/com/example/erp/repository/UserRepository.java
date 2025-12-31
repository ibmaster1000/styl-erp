package com.example.erp.repository;

import com.example.erp.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);

    @Query("""
            select u
            from User u
            where u.active = true
              and (:empNo is null or u.empNo like concat('%', :empNo, '%'))
              and (:name is null or u.name like concat('%', :name, '%'))
              and (:dept is null or u.dept like concat('%', :dept, '%'))
            order by u.empNo asc
            """)
    List<User> searchActiveUsers(@Param("empNo") String empNo,
                                 @Param("name") String name,
                                 @Param("dept") String dept);

    @Query("""
            select distinct u.dept
            from User u
            where u.active = true
              and u.dept is not null
              and u.dept <> ''
            order by u.dept asc
            """)
    List<String> findDistinctActiveDepartments();
}