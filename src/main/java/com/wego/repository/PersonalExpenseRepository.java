package com.wego.repository;

import com.wego.entity.PersonalExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonalExpenseRepository extends JpaRepository<PersonalExpense, UUID> {

    List<PersonalExpense> findByUserIdAndTripId(UUID userId, UUID tripId);

    Optional<PersonalExpense> findByIdAndUserIdAndTripId(UUID id, UUID userId, UUID tripId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
