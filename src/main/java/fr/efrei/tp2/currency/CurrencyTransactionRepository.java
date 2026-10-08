package fr.efrei.tp2.currency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyTransactionRepository extends JpaRepository<CurrencyTransaction, Long> {
}
