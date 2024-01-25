package net.ssehub.openai_api_proxy.controllers.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY, reason = "the given json request is invalid")
public class InvalidRequestException extends Exception {

    private static final long serialVersionUID = -8140856987513544554L;

}
