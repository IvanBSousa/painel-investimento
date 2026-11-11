package caixaverso.application.exception;

public class EntradaInvalidaException extends RuntimeException {

    public EntradaInvalidaException(String message) {
        super(message);
    }

    public EntradaInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}
