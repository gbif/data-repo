package org.gbif.datarepo.api.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = IdentifierSchemeValidator.class)
/**
 * Custom constraint annotation.
 */
public @interface ValidIdentifierScheme {

  String message() default "The identifier is invalid for the identifier scheme used";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

}
