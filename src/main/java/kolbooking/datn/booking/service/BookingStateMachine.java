package kolbooking.datn.booking.service;

import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class BookingStateMachine {

    private static final Map<BookingStatus, Set<BookingStatus>> TRANSITIONS = Map.of(
            BookingStatus.PENDING, EnumSet.of(
                    BookingStatus.ACCEPTED, BookingStatus.REJECTED,
                    BookingStatus.CANCELLED, BookingStatus.CANCELLED_BY_ADMIN),
            BookingStatus.ACCEPTED, EnumSet.of(
                    BookingStatus.IN_PROGRESS, BookingStatus.CANCELLED_BY_ADMIN),
            BookingStatus.IN_PROGRESS, EnumSet.of(
                    BookingStatus.DELIVERED, BookingStatus.CANCELLED_BY_ADMIN),
            BookingStatus.DELIVERED, EnumSet.of(
                    BookingStatus.COMPLETED, BookingStatus.DISPUTED, BookingStatus.CANCELLED_BY_ADMIN),
            BookingStatus.DISPUTED, EnumSet.of(
                    BookingStatus.COMPLETED, BookingStatus.CANCELLED_BY_ADMIN),
            BookingStatus.COMPLETED, EnumSet.noneOf(BookingStatus.class),
            BookingStatus.REJECTED, EnumSet.noneOf(BookingStatus.class),
            BookingStatus.CANCELLED, EnumSet.noneOf(BookingStatus.class),
            BookingStatus.CANCELLED_BY_ADMIN, EnumSet.noneOf(BookingStatus.class)
    );

    private BookingStateMachine() {}

    public static void ensureTransition(BookingStatus from, BookingStatus to) {
        if (from == to) {
            throw new BusinessException("Booking already in " + to,
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        Set<BookingStatus> allowed = TRANSITIONS.getOrDefault(from, EnumSet.noneOf(BookingStatus.class));
        if (!allowed.contains(to)) {
            throw new BusinessException(
                    "Cannot transition booking from " + from + " to " + to,
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
    }
}
