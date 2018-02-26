package com.epam.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class CurrencyPairTrackVerticleTest {
  private static final String ETH = "ETH";
  private static final String DASH = "DASH";
  private CurrencyPairTrackVerticle verticle = new CurrencyPairTrackVerticle(ETH, DASH);
  private Vertx vertx;

  @Before
  public void setUp(TestContext tc) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(verticle, tc.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext tc) {
    vertx.close(tc.asyncAssertSuccess());
  }

  @Test
  public void testCalculateMinMaxForCurrencyPair(TestContext tc) {
    EventBus eventBus = vertx.eventBus();

    eventBus.publish(ETH, 11.1);
    eventBus.publish(DASH, 22.1);

    Async async = tc.async();
    eventBus.send(ETH + "_" + DASH, "getRates", reply -> {
      JsonObject result = JsonObject.mapFrom(reply.result().body());

      String minRate = result.getString("min");
      String maxRate = result.getString("max");

      tc.assertEquals("0.502262", minRate);
      tc.assertEquals("0.502262", maxRate);
      async.complete();
    });

    eventBus.publish(ETH, 10.0);

    Async async1 = tc.async();
    eventBus.send(ETH + "_" + DASH, "getRates", reply -> {
      JsonObject result = JsonObject.mapFrom(reply.result().body());

      String minRate = result.getString("min");
      String maxRate = result.getString("max");

      tc.assertEquals("0.452489", minRate);
      tc.assertEquals("0.502262", maxRate);
      async1.complete();
    });
  }
}
