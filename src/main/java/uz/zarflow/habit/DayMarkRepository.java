package uz.zarflow.habit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface DayMarkRepository extends Repository<DayMark, UUID> {

    DayMark save(DayMark dayMark);

    Optional<DayMark> findByUserIdAndDate(UUID userId, LocalDate date);

    List<DayMark> findAllByUserIdAndDateBetween(UUID userId, LocalDate from, LocalDate toInclusive);

}
