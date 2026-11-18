package org.example.meetingroomreservation.user.infrastructure.adapter.out.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    boolean existsByEmail(String email);
}
