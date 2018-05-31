package org.gbif.datarepo.api.validation.constraints;

import org.gbif.datarepo.api.model.Creator;
import org.gbif.datarepo.api.validation.identifierschemes.IdentifierSchemaValidatorFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Custom constraint validation for Creator.identifier.
 */
public class IdentifierSchemeValidator implements ConstraintValidator<ValidIdentifierScheme,Creator> {

  private static final String INVALID_ERROR_MSG = "The identifier %s is invalid for the identifier scheme %s";

  private static final String ID_AND_SCHEME_REQUIRED_MSG = "The identifier and identifier scheme must be set of one of them is present";

  @Override
  public void initialize(ValidIdentifierScheme constraintAnnotation) {
    // ;
  }

  @Override
  public boolean isValid(Creator value, ConstraintValidatorContext context) {
    boolean result = true;
    //if identifier or identifierScheme are specified, both must be present
    if ((value.getIdentifier() != null && value.getIdentifierScheme() == null) ||
        (value.getIdentifier() == null && value.getIdentifierScheme() != null)) {
      context.buildConstraintViolationWithTemplate(ID_AND_SCHEME_REQUIRED_MSG).addConstraintViolation();
      result= false;
    }
    if (value.getIdentifier() != null && value.getIdentifierScheme() != null) {
      if(!IdentifierSchemaValidatorFactory.getValidator(value.getIdentifierScheme())
        .isValid(value.getIdentifier())) {
        context.buildConstraintViolationWithTemplate(String.format(INVALID_ERROR_MSG, value.getIdentifier(),
                                                                   value.getIdentifierScheme()))
        .addConstraintViolation();
        result = false;
      }
    }
    return result;
  }
}
