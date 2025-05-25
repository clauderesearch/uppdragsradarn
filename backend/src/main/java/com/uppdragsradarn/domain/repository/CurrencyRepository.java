package com.uppdragsradarn.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uppdragsradarn.domain.model.Currency;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {
  Optional<Currency> findByCode(String code);
}
