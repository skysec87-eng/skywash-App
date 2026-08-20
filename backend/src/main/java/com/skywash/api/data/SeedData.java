package com.skywash.api.data;

import java.util.List;
import java.util.UUID;

import com.skywash.api.model.Partner;
import com.skywash.api.model.ServiceType;

public final class SeedData {
  private SeedData() {}

  public static final int BASE_FEE = 500;
  public static final double PLATFORM_FEE_RATE = 0.10;
  public static final double NEARBY_RADIUS_KM = 40;
  public static final int NEARBY_LIMIT = 8;

  public static List<ServiceType> services() {
    return List.of(
        new ServiceType("wash", "Wash & Fold", 500, "kg", "🧺"),
        new ServiceType("dry", "Dry Cleaning", 1500, "item", "🧥"),
        new ServiceType("iron", "Iron Only", 300, "kg", "👔"),
        new ServiceType("express", "Express (4h)", 900, "kg", "⚡")
    );
  }

  public static List<Partner> partners() {
    return List.of(
        p("WashRyte Laundry Service", "Lagos", "Lekki", "The Lennox Mall, Phase 1 Admiralty Wy, Lekki Phase 1, Lagos", 6.4390679, 3.4555214, 4.1, "+2348059303818"),
        p("The Ultimate Standard Laundry And Cleaning", "Lagos", "Ikoyi", "Falomo Roundabout, Bourdillon Rd, Ikoyi, Lagos", 6.444105, 3.4279112, 5.0, "+2347019248933"),
        p("Get It Right Laundry Services", "Lagos", "Ikoyi", "33 Turnbull Rd, Ikoyi, Lagos", 6.4563677, 3.4470903, 5.0, "+2348083698659"),
        p("Kenza Laundry & Dry Cleaning Services", "Lagos", "Ikoyi", "109A Awolowo Rd, Ikoyi, Lagos", 6.4441325, 3.4207871, 5.0, "+2347069178360"),
        p("Payless Laundry Services", "Lagos", "Victoria Island", "11 Sir Samuel Manuwa St, Victoria Island, Lagos", 6.4369464, 3.4355656, 3.1, "+2348097208338"),
        p("Ace Wash N Dry", "Lagos", "Ikeja", "Kudirat Abiola Way, Oregun, Ikeja, Lagos", 6.6031369, 3.362752, 4.8, "+2349057184682"),
        p("Washyard Laundromat | Allen", "Lagos", "Ikeja", "47 Allen Ave, Allen, Ikeja, Lagos", 6.6011041, 3.3521018, 4.5, "+2347025650057"),
        p("LaunderLand Dry Cleaners", "Lagos", "Ikeja", "15 Toyin St, Allen, Ikeja, Lagos", 6.5965116, 3.3488582, 4.8, "+2348144174436"),
        p("True Wash Laundromat Akoka", "Lagos", "Yaba", "5/7 St Finbarr's College Rd, Akoka, Lagos", 6.5244543, 3.3855239, 5.0, "+2348037905707"),
        p("Aroaic Laundry & Dry Cleaning Services", "Lagos", "Ikeja", "14/16 Ladipo Kuku St, Allen, Ikeja, Lagos", 6.5999076, 3.3521886, 4.0, "+2348034546161"),
        p("Dee Clean Laundry Lekki", "Lagos", "Lekki", "1 Kayode Otitoju St, Lekki Phase 1, Lagos", 6.4505182, 3.4707559, 4.9, "+2349117266758"),
        p("Laundry Care Lekki", "Lagos", "Lekki", "1A Kayode Otitoju St, Eti-Osa, Lagos", 6.450511, 3.4704056, 5.0, "+2349072564972"),
        p("Wasche Point Laundry Service & Dry Cleaner", "Lagos", "Lekki", "Plot 12 Emma Abimbola Cole, off Fola Osibo Rd, Lekki Phase I, Lagos", 6.4424118, 3.4782121, 4.4, "+2348188882013"),
        p("LaundrybyTIMESIGNATURE", "Lagos", "Lekki", "6B Admiralty Rd, Lekki Phase 1, Lagos", 6.4571233, 3.4709205, 4.8, "+23412919486"),
        p("GozzyCee Laundry Services", "Lagos", "Surulere", "60 Sanya St, Surulere, Lagos", 6.4871993, 3.3324632, 5.0, "+2348160843136"),
        p("Alpha's Touch Laundry Service", "Lagos", "Surulere", "4 Tafawa Balewa Cres, off Adeniran Ogunsanya, Surulere, Lagos", 6.4951617, 3.3568973, 4.9, "+2348055472703"),
        p("Quickwash Laundromat", "Lagos", "Surulere", "62 Adeniran Ogunsanya St, Surulere, Lagos", 6.4944357, 3.3568915, 4.9, "+2347076130688"),
        p("D-way Laundry", "Lagos", "Surulere", "2 Tayo-Oyefeko St, off Shaki Crescent, Surulere, Lagos", 6.4972416, 3.3389073, 5.0, "+2349082933639"),
        p("Snowyclean Laundromat", "Lagos", "Surulere", "Adeniran Ogunsanya Mall (ShopRite), Surulere, Lagos", 6.4909473, 3.3568883, 3.8, null),
        p("Renee Laundromat and Dry Cleaning", "Lagos", "Yaba", "44 Olonode St, Alagomeji, Yaba, Lagos", 6.4987637, 3.3779721, 5.0, "+2348126328691"),
        p("EzWashnDry Laundromat", "Lagos", "Yaba", "E-Centre (Ozone Cinemas), Commercial Ave, Sabo Yaba, Lagos", 6.5062713, 3.3743661, 4.4, "+2348148728762"),
        p("Astra Cleaners Ltd", "Lagos", "Yaba", "300 Herbert Macaulay Wy, Yaba, Lagos", 6.5047348, 3.3780282, 4.9, "+2349090030003"),
        p("Skywhite Drycleaners, Laundry & Cleaning", "Lagos", "Yaba", "2 Ogabi St, Abule Ijesha Rd, Yaba, Lagos", 6.5219455, 3.379573, 5.0, "+2348025653564"),
        p("Your Laundry Guy", "Lagos", "Yaba", "Yaba-Onike Rd, Yaba, Lagos", 6.5058386, 3.3779722, 4.0, "+2348120931602"),
        p("Capital Wash Hub", "Abuja", "Wuse", "Plot 2147 Aminu Kano Cres, Wuse II, Abuja", 9.0765, 7.3986, 4.7, "+2348011110001"),
        p("Maitama Fresh Laundry", "Abuja", "Maitama", "Aguiyi Ironsi St, Maitama, Abuja", 9.0882, 7.4951, 4.9, "+2348011110002"),
        p("Garki Clean Express", "Abuja", "Garki", "Area 3, Garki, Abuja", 9.0354, 7.4832, 4.5, "+2348011110003"),
        p("Asokoro Press & Fold", "Abuja", "Asokoro", "Yakubu Gowon Cres, Asokoro, Abuja", 9.0418, 7.5146, 4.8, "+2348011110004"),
        p("Garden City Laundry", "Port Harcourt", "GRA", "Tombia St, GRA Phase 2, Port Harcourt", 4.8241, 7.0336, 4.6, "+2348022220001"),
        p("Trans Amadi Wash Co", "Port Harcourt", "Trans Amadi", "Trans Amadi Industrial Layout, Port Harcourt", 4.8156, 7.0498, 4.4, "+2348022220002"),
        p("Rumuola Quick Clean", "Port Harcourt", "Rumuola", "Rumuola Rd, Port Harcourt", 4.8472, 7.0169, 4.8, "+2348022220003")
    );
  }

  private static Partner p(String name, String city, String area, String address,
                           double lat, double lng, double rating, String phone) {
    String slug = name.toLowerCase()
        .replaceAll("[^a-z0-9]+", ".")
        .replaceAll("^\\.|\\.$", "");
    if (slug.length() > 28) slug = slug.substring(0, 28).replaceAll("\\.$", "");
    String email = slug.isBlank() ? null : "ops@" + slug + ".partner.skywash.app";
    return new Partner(UUID.randomUUID().toString(), name, city, area, address, lat, lng, rating, phone, email, true);
  }

  public static final List<StatusDef> STATUSES = List.of(
      new StatusDef("confirmed", "Request confirmed", 3_000),
      new StatusDef("enroute", "Partner heading to you", 6_000),
      new StatusDef("pickedup", "Picked up from you", 3_000),
      new StatusDef("washing", "Washing at the laundromat", 7_000),
      new StatusDef("delivering", "Out for delivery", 6_000),
      new StatusDef("delivered", "Delivered", 0)
  );

  public record StatusDef(String key, String label, long durationMs) {}
}
