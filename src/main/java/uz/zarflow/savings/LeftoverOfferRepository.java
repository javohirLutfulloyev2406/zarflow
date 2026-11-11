package uz.zarflow.savings;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface LeftoverOfferRepository extends Repository<LeftoverOffer, UUID> {

    LeftoverOffer save(LeftoverOffer offer);

    Optional<LeftoverOffer> findByUserIdAndMonth(UUID userId, LocalDate month);

    Optional<LeftoverOffer> findByIdAndUserId(UUID id, UUID userId);

}
