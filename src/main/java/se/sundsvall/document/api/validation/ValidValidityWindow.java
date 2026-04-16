package se.sundsvall.document.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = ValidValidityWindowValidator.class)
public @interface ValidValidityWindow {

	String message() default "validFrom must not be after validTo";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
