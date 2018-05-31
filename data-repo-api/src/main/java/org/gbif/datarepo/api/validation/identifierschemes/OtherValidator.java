package org.gbif.datarepo.api.validation.identifierschemes;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class OtherValidator implements IdentifierSchemeValidator {

  @Override
  public boolean isValid(String value) {
    return !Strings.isNullOrEmpty(value);
  }

  @Override
  public String normalize(String value) {
    Preconditions.checkNotNull(value, "Identifier value can't be null");
    return value.trim();
  }
}
