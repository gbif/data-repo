package org.gbif.datarepo.api.validation.constraints;

import org.gbif.datarepo.api.model.Creator;
import org.gbif.datarepo.api.validation.identifierschemes.IdentifierSchemaValidatorFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Custom constraint validation for Creator.identifier.
 */
public class IdentifierSchemeValidator implements ConstraintValidator<ValidIdentifierScheme,Creator> {

  private static final String DEFAULT_ERROR_MSG = "The identifier  %S is invalid for the identifier scheme %s";

  @Override
  public void initialize(ValidIdentifierScheme constraintAnnotation) {
    // ;
  }

  @Override
  public boolean isValid(Creator value, ConstraintValidatorContext context) {
    boolean result = true;
    if (value.getIdentifier() != null && value.getIdentifierScheme() != null) {
      if(!IdentifierSchemaValidatorFactory.getValidator(value.getIdentifierScheme())
        .isValid(value.getIdentifier())) {
        context.buildConstraintViolationWithTemplate(String.format(DEFAULT_ERROR_MSG, value.getIdentifier(),
                                                                   value.getIdentifierScheme()))
        .addConstraintViolation();
        result = false;
      }
    }
    return result;
  }
}
