package net.ssehub.openai_api_proxy.controllers.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "no user specified via x-user header")
public class NoUserSpecifiedException extends Exception {

    private static final long serialVersionUID = 4944524300482358188L;
    
}
