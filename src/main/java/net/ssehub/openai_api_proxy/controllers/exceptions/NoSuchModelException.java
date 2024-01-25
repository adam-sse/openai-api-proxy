package net.ssehub.openai_api_proxy.controllers.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "no model with the given name found")
public class NoSuchModelException extends Exception {

    private static final long serialVersionUID = 9028356940722344196L;

}
