package com.apex.core.port;

import java.util.Optional;

public interface SecretsPort {
    Optional<String> getSecret(String alias);
    void putSecret(String alias, String secretValue);
    void removeSecret(String alias);
}
