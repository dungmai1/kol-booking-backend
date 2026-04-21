package kolbooking.datn.common.exception;

public final class ErrorCode {
    private ErrorCode() {}

    public static final String VALIDATION_FAILED   = "VALIDATION_FAILED";
    public static final String RESOURCE_NOT_FOUND  = "RESOURCE_NOT_FOUND";
    public static final String BUSINESS_ERROR      = "BUSINESS_ERROR";
    public static final String UNAUTHORIZED        = "UNAUTHORIZED";
    public static final String FORBIDDEN           = "FORBIDDEN";
    public static final String INTERNAL_ERROR      = "INTERNAL_ERROR";

    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String INVALID_CREDENTIALS  = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_INACTIVE     = "ACCOUNT_INACTIVE";
    public static final String ACCOUNT_BANNED       = "ACCOUNT_BANNED";
    public static final String TOKEN_INVALID        = "TOKEN_INVALID";
    public static final String TOKEN_EXPIRED        = "TOKEN_EXPIRED";
    public static final String TOKEN_USED           = "TOKEN_USED";
}
