/*
 * Copyright 2022 Alliander N.V.
 */

package com.alliander.osgp.kafka.message.signing;

import java.security.GeneralSecurityException;
import java.util.Objects;

/** Wraps a {@link GeneralSecurityException} with an unchecked exception. */
public class UncheckedSecurityException extends RuntimeException {

  private static final long serialVersionUID = 5152038114753546167L;

  /**
   * @throws NullPointerException if the cause is {@code null}
   */
  public UncheckedSecurityException(final String message, final GeneralSecurityException cause) {
    super(message, Objects.requireNonNull(cause));
  }

  /**
   * @throws NullPointerException if the cause is {@code null}
   */
  public UncheckedSecurityException(final GeneralSecurityException cause) {
    super(Objects.requireNonNull(cause));
  }

  /**
   * @return the {@code GeneralSecurityException} wrapped by this exception.
   */
  @Override
  public synchronized GeneralSecurityException getCause() {
    return (GeneralSecurityException) super.getCause();
  }
}
