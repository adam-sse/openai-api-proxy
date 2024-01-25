package net.ssehub.openai_api_proxy.controllers.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "no user with the given name found")
public class NoSuchUserException extends Exception {

    private static final long serialVersionUID = -4886217962518209149L;

}
