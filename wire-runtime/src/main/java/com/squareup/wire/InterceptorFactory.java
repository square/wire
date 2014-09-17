package com.squareup.wire;

import java.util.Collection;

public interface InterceptorFactory {

  Interceptor interceptorFor(Class<? extends Message> messageType,
      Collection<Extension<?, ?>> extensions);
}
