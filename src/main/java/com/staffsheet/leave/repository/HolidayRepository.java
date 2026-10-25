package com.staffsheet.leave.repository;

import com.staffsheet.leave.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    boolean existsByHolidayDate(LocalDate date);

    List<Holiday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate start, LocalDate end);
}
