package com.interswitch.verveguarddemo.repositories;

import com.interswitch.verveguarddemo.entities.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByCardHash(String cardHash);
}
