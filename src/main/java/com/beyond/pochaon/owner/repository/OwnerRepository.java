package com.beyond.pochaon.owner.repository;

import com.beyond.pochaon.owner.domain.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {
    Optional<Owner> findByOwnerEmail(String email);

    boolean existsByBusinessRegistrationNumber(String businessRegistrationNumber);

    Optional<Owner> findByOwnerNameAndPhoneNumber(String name, String phone);
}
