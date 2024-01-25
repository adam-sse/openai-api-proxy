package net.ssehub.openai_api_proxy.dto.openai_api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record Message(String content, Role role, String name) {
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ChatGptMessage[content=");
        String content = this.content != null ? this.content : "null";
        if (content.length() > 20) {
            content = content.substring(0, 20) + "...";
        }
        builder.append(content.replaceAll("\r?\n", "\\\\n"));
        builder.append(", role=");
        builder.append(role);
        if (name != null) {
            builder.append(", name=");
            builder.append(name);
        }
        builder.append("]");
        return builder.toString();
    }
    
}
