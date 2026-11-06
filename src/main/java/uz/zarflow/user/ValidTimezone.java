package uz.zarflow.user;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimezoneValidator.class)
@interface ValidTimezone {

    String message() default "must be a valid IANA time zone id (e.g. Asia/Tashkent)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
