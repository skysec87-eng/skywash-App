package com.skywash.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skywash.api.entity.OrderEntity;
import com.skywash.api.entity.PartnerEntity;
import com.skywash.api.entity.PartnerNotificationEntity;
import com.skywash.api.repo.PartnerNotificationRepository;
import com.skywash.api.repo.PartnerRepository;
import com.skywash.api.support.TestFixtures;

@ExtendWith(MockitoExtension.class)
class PartnerNotifyServiceTest {

  @Mock private PartnerRepository partnerRepository;
  @Mock private PartnerNotificationRepository notificationRepository;
  @Mock private EmailService emailService;

  private PartnerNotifyService service;
  private PartnerEntity partner;
  private OrderEntity order;

  @BeforeEach
  void setUp() {
    service = new PartnerNotifyService(
        partnerRepository,
        notificationRepository,
        emailService,
        new ObjectMapper(),
        "fallback@skywash.test",
        "",
        ""
    );
    partner = TestFixtures.partner("p1", "Laundry Care Lekki", "Lagos", "Lekki", 6.45, 3.47, 5.0);
    partner.setPhone("+2348059303818");
    partner.setEmail("ops@laundry.care.lekki.partner.skywash.app");
    order = TestFixtures.order("order-123", "p1", "delivered");
    order.setStatusLabel("Customer confirmed");
  }

  @Test
  void onDeliveryConfirmedQueuesWhatsAppDeeplinkWhenCloudApiUnset() {
    when(partnerRepository.findById("p1")).thenReturn(Optional.of(partner));
    when(emailService.sendPartnerAlert(anyString(), anyString(), anyString())).thenReturn(true);
    when(notificationRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

    service.onDeliveryConfirmed(order);

    ArgumentCaptor<PartnerNotificationEntity> captor = ArgumentCaptor.forClass(PartnerNotificationEntity.class);
    verify(notificationRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());
    List<PartnerNotificationEntity> saved = captor.getAllValues();

    PartnerNotificationEntity email = saved.stream().filter(n -> "email".equals(n.getChannel())).findFirst().orElseThrow();
    PartnerNotificationEntity wa = saved.stream().filter(n -> "whatsapp".equals(n.getChannel())).findFirst().orElseThrow();

    assertEquals("sent", email.getStatus());
    assertEquals("queued", wa.getStatus());
    assertTrue(wa.getDeeplink() != null && wa.getDeeplink().startsWith("https://wa.me/2348059303818"));
    verify(emailService).sendPartnerAlert(eq(partner.getEmail()), anyString(), anyString());
  }

  @Test
  void toE164DigitsNormalizesNigerianLocalFormat() {
    assertEquals("2348059303818", PartnerNotifyService.toE164Digits("08059303818"));
    assertEquals("2348059303818", PartnerNotifyService.toE164Digits("+234 805 930 3818"));
  }
}
