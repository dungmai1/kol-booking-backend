package kolbooking.datn.common.util;

import java.util.function.Consumer;

public final class StringFieldUpdates {

    private StringFieldUpdates() {}

    /** Applies a clearable string field: null/blank clears (sets null), otherwise sets the value. */
    public static void applyClearable(String value, Consumer<String> setter) {
        setter.accept(value == null || value.isBlank() ? null : value);
    }
}
