package me.pectics.securetransfer.core;

/** JWT 未通过校验(签名 / 时间 / iss / aud / 身份绑定任一失败)。 */
public class TokenVerificationException extends Exception {

    public TokenVerificationException(final String message) {
        super(message);
    }

    public TokenVerificationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
