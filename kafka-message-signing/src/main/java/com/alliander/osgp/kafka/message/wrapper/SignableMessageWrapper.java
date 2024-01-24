/*
 * Copyright 2022 Alliander N.V.
 */

package com.alliander.osgp.kafka.message.wrapper;

public abstract class SignableMessageWrapper<T> {
  protected final T message;

  protected SignableMessageWrapper(final T message) {
    this.message = message;
  }

  public T getMessage() {
    return this.message;
  }

  public abstract java.nio.ByteBuffer toByteBuffer() throws java.io.IOException;

  public abstract java.nio.ByteBuffer getSignature();

  public abstract void setSignature(java.nio.ByteBuffer signature);
}
