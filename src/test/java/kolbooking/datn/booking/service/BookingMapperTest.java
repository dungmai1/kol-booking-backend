package kolbooking.datn.booking.service;

import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.booking.dto.BookingResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingMapperTest {

    @Test
    void toDto_includesAttachmentUrl() {
        Booking booking = Booking.builder()
                .id(394L)
                .brandProfileId(10L)
                .brandCompanyName("Demo Brand")
                .kolProfileId(20L)
                .kolDisplayName("Demo KOL")
                .campaignTitle("Summer Launch")
                .campaignBrief("Campaign brief with enough detail")
                .deliverables("One TikTok video")
                .budget(new BigDecimal("5000000"))
                .platformFeePercent(BigDecimal.TEN)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(7))
                .status(BookingStatus.PENDING)
                .attachmentUrl("/uploads/2026/06/brief.pdf")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        BookingResponse response = BookingMapper.toDto(booking);

        assertEquals("/uploads/2026/06/brief.pdf", response.attachmentUrl());
    }
}
