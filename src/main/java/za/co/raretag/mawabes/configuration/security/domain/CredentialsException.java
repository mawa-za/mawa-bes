package za.co.raretag.mawabes.configuration.security.domain;

@SuppressWarnings("serial")
public class CredentialsException extends RuntimeException{

    public CredentialsException(String message) {
        super(message);
    }
}