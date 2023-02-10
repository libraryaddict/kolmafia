package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withCampgroundItem;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withNextResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CampgroundRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("CampgroundRequest");
    Preferences.reset("CampgroundRequest");
  }

  @Test
  void canDetectExhaustedMedicineCabinet() {
    String html = html("request/test_campground_medicine_cabinet_out_of_consults.html");
    CampgroundRequest.parseResponse("campground.php?action=workshed", html);
    assertEquals(
        CampgroundRequest.getCurrentWorkshedItem().getItemId(), ItemPool.COLD_MEDICINE_CABINET);
  }

  @Test
  void canDetectMeatMaid() {
    String html = html("request/test_campground_inspect_dwelling.html");
    CampgroundRequest.parseResponse("campground.php?action=inspectdwelling", html);
    assertCampgroundItemCount(ItemPool.CLOCKWORK_MAID, 1);
  }

  @Nested
  class RockGarden {
    @AfterEach
    public void afterEach() {
      CampgroundRequest.reset();
    }

    @Test
    void canDetectNoGarden() {
      String html = html("request/test_campground_no_garden.html");
      CampgroundRequest.parseResponse("campground.php", html);
      assertThat(CampgroundRequest.getCrop(), nullValue());
      assertThat(CampgroundRequest.getCrops(), empty());
    }

    @Test
    void canDetectNoRockGarden() {
      String html = html("request/test_campground_rock_garden_0.html");
      CampgroundRequest.parseResponse("campground.php", html);
      assertCampgroundItemCount(ItemPool.GROVELING_GRAVEL, 0);
      assertCampgroundItemCount(ItemPool.MILESTONE, 0);
      assertCampgroundItemCount(ItemPool.WHETSTONE, 0);
      assertCampgroundItemCount(ItemPool.ROCK_SEEDS, 0);
    }

    @Test
    void canDetectPartialRockGarden() {
      String html = html("request/test_campground_rock_garden_0_1.html");
      CampgroundRequest.parseResponse("campground.php", html);
      assertCampgroundItemCount(ItemPool.GROVELING_GRAVEL, 1);
      assertCampgroundItemCount(ItemPool.MILESTONE, 0);
      assertCampgroundItemCount(ItemPool.WHETSTONE, 0);
      assertCampgroundItemCount(ItemPool.ROCK_SEEDS, 1);
    }

    @Test
    void canDetectRockGarden() {
      String html = html("request/test_campground_rock_garden_5.html");
      CampgroundRequest.parseResponse("campground.php", html);
      assertCampgroundItemCount(ItemPool.FRUITY_PEBBLE, 2);
      assertCampgroundItemCount(ItemPool.BOLDER_BOULDER, 2);
      assertCampgroundItemCount(ItemPool.HARD_ROCK, 2);
      assertCampgroundItemCount(ItemPool.ROCK_SEEDS, 1);
    }

    @Test
    void canTrackRockGardenHarvest() {
      var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.MILESTONE),
              withCampgroundItem(ItemPool.GROVELING_GRAVEL),
              withCampgroundItem(ItemPool.WHETSTONE),
              withNextResponse(
                  200, html("request/test_campground_tracks_rock_garden_harvest.html")),
              withItem(ItemPool.WHETSTONE, 0));

      try (cleanups) {
        new GenericRequest("campground.php?action=rgarden3&pwd").run();

        assertThat(KoLConstants.inventory, hasItem(ItemPool.get(ItemPool.WHETSTONE)));
        assertCampgroundItemCount(ItemPool.MILESTONE, 1);
        assertCampgroundItemCount(ItemPool.GROVELING_GRAVEL, 1);
        assertCampgroundItemCount(ItemPool.WHETSTONE, 0);
      }
    }

    @Test
    void canStopTrackingRockGarden() {
      CampgroundRequest.parseResponse(
          "campground.php", html("request/test_campground_rock_garden_5.html"));

      assertThat(KoLConstants.campground, hasItem(ItemPool.get(ItemPool.ROCK_SEEDS)));
      assertThat(KoLConstants.campground, hasItem(ItemPool.get(ItemPool.FRUITY_PEBBLE)));

      CampgroundRequest.parseResponse(
          "campground.php", html("request/test_campground_no_garden.html"));

      assertThat(KoLConstants.campground, not(hasItem(ItemPool.get(ItemPool.ROCK_SEEDS))));
      assertThat(KoLConstants.campground, not(hasItem(ItemPool.get(ItemPool.FRUITY_PEBBLE))));
    }
  }

  private void assertCampgroundItemCount(int itemId, int count) {
    var resOpt = KoLConstants.campground.stream().filter(x -> x.getItemId() == itemId).findAny();
    assertThat(resOpt.isPresent(), equalTo(true));
    var res = resOpt.get();
    assertThat(res.getCount(), equalTo(count));
  }
}
