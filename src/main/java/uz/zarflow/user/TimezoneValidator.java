package uz.zarflow.user;

import java.time.ZoneId;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

class TimezoneValidator implements ConstraintValidator<ValidTimezone, String> {

    private static final Set<String> AVAILABLE_ZONE_IDS = ZoneId.getAvailableZoneIds();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return AVAILABLE_ZONE_IDS.contains(value);
    }

}
