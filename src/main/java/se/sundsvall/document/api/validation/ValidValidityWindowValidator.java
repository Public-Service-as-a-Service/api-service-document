package se.sundsvall.document.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import org.springframework.beans.BeanWrapperImpl;

public class ValidValidityWindowValidator implements ConstraintValidator<ValidValidityWindow, Object> {

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		final var wrapper = new BeanWrapperImpl(value);
		final var validFrom = (LocalDate) wrapper.getPropertyValue("validFrom");
		final var validTo = (LocalDate) wrapper.getPropertyValue("validTo");
		return validFrom == null || validTo == null || !validFrom.isAfter(validTo);
	}
}
