package kolbooking.datn.booking.service;

import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.domain.BookingMessage;
import kolbooking.datn.booking.dto.BookingMessageResponse;
import kolbooking.datn.booking.dto.BookingResponse;

public final class BookingMapper {

    private BookingMapper() {}

    public static BookingResponse toDto(Booking b) {
        return new BookingResponse(
                b.getId(), b.getBrandProfileId(), b.getKolProfileId(),
                b.getCampaignTitle(), b.getCampaignBrief(), b.getDeliverables(),
                b.getBudget(), b.getStartDate(), b.getEndDate(),
                b.getStatus(), b.getRejectReason(), b.getCancelReason(),
                b.getInvoiceUrl(), b.getCreatedAt(), b.getUpdatedAt()
        );
    }

    public static BookingMessageResponse toDto(BookingMessage m) {
        return new BookingMessageResponse(
                m.getId(), m.getBookingId(), m.getSenderUserId(),
                m.getContent(), m.getAttachmentUrl(), m.getCreatedAt()
        );
    }
}
