package uz.zarflow.habit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface UserAchievementRepository extends Repository<UserAchievement, UUID> {

    UserAchievement save(UserAchievement achievement);

    List<UserAchievement> findAllByUserId(UUID userId);

    boolean existsByUserIdAndCode(UUID userId, AchievementCode code);

}
