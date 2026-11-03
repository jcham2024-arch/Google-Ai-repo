package com.apex.core.port;

import java.util.List;
import java.util.Optional;

public interface StoragePort {
    void save(String key, String value);
    Optional<String> get(String key);
    void delete(String key);
    List<String> listKeys(String prefix);
}
