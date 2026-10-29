package com.hotelaria.repositories;

import com.hotelaria.models.Guest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<Guest> findWithUserById(Long id);

    @EntityGraph(attributePaths = "user")
    Optional<Guest> findByUser_Id(Long userId);

    @EntityGraph(attributePaths = {"user", "user.role", "user.role.permissions", "user.extraPermissions"})
    Optional<Guest> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    @Query("select g.id from Guest g where g.user.id = :userId")
    Optional<Long> findIdByUserId(@Param("userId") Long userId);

    @Query(value = """
            select g from Guest g join fetch g.user u
            where lower(u.fullName) like lower(concat('%', :term, '%'))
               or lower(u.email) like lower(concat('%', :term, '%'))
               or g.cpf = :cpf
            """,
            countQuery = """
            select count(g) from Guest g join g.user u
            where lower(u.fullName) like lower(concat('%', :term, '%'))
               or lower(u.email) like lower(concat('%', :term, '%'))
               or g.cpf = :cpf
            """)
    Page<Guest> search(@Param("term") String term, @Param("cpf") String cpf, Pageable pageable);

    @Query(value = "select g from Guest g join fetch g.user",
            countQuery = "select count(g) from Guest g")
    Page<Guest> findAllWithUser(Pageable pageable);
}
