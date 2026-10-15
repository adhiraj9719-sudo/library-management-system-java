package com.library.repository;

import com.library.model.Role;
import com.library.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByMembershipIdIgnoreCase(String membershipId);

    boolean existsByMembershipIdIgnoreCaseAndIdNot(String membershipId, Long id);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    Optional<User> findByIdAndRole(Long id, Role role);

    List<User> findByRoleOrderByNameAsc(Role role);

    @Query("select u from User u where u.role = :role and ("
            + "lower(u.name) like lower(concat('%', :q, '%')) "
            + "or lower(u.email) like lower(concat('%', :q, '%')) "
            + "or lower(coalesce(u.membershipId, '')) like lower(concat('%', :q, '%'))) "
            + "order by lower(u.name)")
    List<User> search(@Param("role") Role role, @Param("q") String q);
}
