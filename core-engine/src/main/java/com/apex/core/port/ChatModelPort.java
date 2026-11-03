package com.apex.core.port;

import com.apex.core.port.model.ChatRequest;
import com.apex.core.port.model.ChatResponse;
import java.util.Iterator;

public interface ChatModelPort {
    ChatResponse generate(ChatRequest request);
    Iterator<String> stream(ChatRequest request);
}
