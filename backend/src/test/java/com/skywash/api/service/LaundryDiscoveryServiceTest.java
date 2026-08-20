package com.skywash.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class LaundryDiscoveryServiceTest {

  @Test
  void parseOverpassNodesAndWays() throws Exception {
    String json = """
        {
          "elements": [
            {
              "type": "node",
              "id": 111,
              "lat": 6.36,
              "lon": 2.42,
              "tags": { "name": "Pressing Marina", "shop": "laundry", "addr:city": "Cotonou", "addr:suburb": "Fidjrossè" }
            },
            {
              "type": "way",
              "id": 222,
              "center": { "lat": 40.75, "lon": -73.99 },
              "tags": { "name": "Midtown Wash", "addr:city": "New York", "addr:street": "7th Ave" }
            }
          ]
        }
        """;
    var root = new ObjectMapper().readTree(json);
    List<Map<String, Object>> places = LaundryDiscoveryService.parseElements(root);
    assertEquals(2, places.size());
    assertEquals("osm-node-111", places.get(0).get("id"));
    assertEquals("Pressing Marina", places.get(0).get("name"));
    assertEquals("Cotonou", places.get(0).get("city"));
    assertEquals("osm-way-222", places.get(1).get("id"));
    assertEquals("New York", places.get(1).get("city"));
    assertTrue(((Number) places.get(1).get("lng")).doubleValue() < 0);
  }

  @Test
  void parseGooglePlacesResults() throws Exception {
    String json = """
        {
          "status": "OK",
          "results": [
            {
              "place_id": "ChIJabc",
              "name": "Ibadan Pressing",
              "vicinity": "Ring Rd, Ibadan",
              "rating": 4.4,
              "geometry": { "location": { "lat": 7.3775, "lng": 3.9470 } }
            },
            {
              "place_id": "ChIJcar",
              "name": "Joe Car Wash",
              "vicinity": "Ring Rd",
              "geometry": { "location": { "lat": 7.38, "lng": 3.95 } }
            }
          ]
        }
        """;
    var root = new ObjectMapper().readTree(json);
    List<Map<String, Object>> places = LaundryDiscoveryService.parseGooglePlaces(root);
    assertEquals(1, places.size());
    assertEquals("gplace-ChIJabc", places.get(0).get("id"));
    assertEquals("Ibadan Pressing", places.get(0).get("name"));
  }

  @Test
  void skipsCarWashNames() {
    assertTrue(LaundryDiscoveryService.skipPlace("Joe's Car Wash"));
    assertTrue(!LaundryDiscoveryService.skipPlace("Ibadan Laundry Services"));
  }
}
