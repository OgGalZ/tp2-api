package fr.efrei.tp2.bmi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BMIRecordRepository extends JpaRepository<BMIRecord, Long> {

    Page<BMIRecord> findByUserId(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    @Query("select avg(r.bmi) from BMIRecord r")
    Double globalAverageBmi();

    @Query("select min(r.bmi) from BMIRecord r")
    Double globalMinBmi();

    @Query("select max(r.bmi) from BMIRecord r")
    Double globalMaxBmi();

    @Query("select count(distinct r.userId) from BMIRecord r")
    long countDistinctUsers();

    @Query("select avg(r.bmi) from BMIRecord r where r.userId = :userId")
    Double userAverageBmi(@Param("userId") Long userId);

    /** Chaque ligne : [BmiCategory, Long nombre] */
    @Query("select r.category, count(r) from BMIRecord r group by r.category")
    List<Object[]> countByCategory();
}
