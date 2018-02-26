package com.epam.vertx;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The Currency pair track verticle.
 * Current Verticle deployed automatically by {@link RestControllerVerticle} once user
 * adds a new currency pair to track.
 */
public class CurrencyPairTrackVerticle extends AbstractVerticle {
  private static final Logger Logger = LoggerFactory.getLogger(CurrencyPairTrackVerticle.class);
  private static final int SCALE = 6;
  private static final String MIN_KEY = "min";
  private static final String MAX_KEY = "max";
  private static final int FAILURE_CODE = 0;

  private final String ccy1;
  private final String ccy2;

  private BigDecimal ccy1Price;
  private BigDecimal ccy2Price;

  private BigDecimal maxRate;
  private BigDecimal minRate;

  /**
   * Instantiates a new Currency pair track verticle.
   * Currency pair rate is a ratio of currency1 dollar rate to a currency2 dollar rate:
   * CCY1_CCY2_PAIR_RATE = (CCY1/USD) / (CCY2/USD)
   *
   * @param ccy1 the currency1 to track
   * @param ccy2 the currency2 to track
   */
  public CurrencyPairTrackVerticle(String ccy1, String ccy2) {
    this.ccy1 = ccy1;
    this.ccy2 = ccy2;
  }

  @Override
  public void start() {
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(ccy1, event -> {
      BigDecimal newPrice = BigDecimal.valueOf((double) event.body());
      if (ccy1Price == null || !ccy1Price.equals(newPrice)) {
        this.ccy1Price = newPrice;
        updateRates();
      }
    });

    eventBus.consumer(ccy2, event -> {
      BigDecimal newPrice = BigDecimal.valueOf((double) event.body());
      if (ccy2Price == null || !ccy2Price.equals(newPrice)) {
        this.ccy2Price = newPrice;
        updateRates();
      }
    });

    eventBus.consumer(ccy1 + "_" + ccy2).handler(message -> {
      Logger.debug("Getting current rate value for ccy1/ccy2 : {}/{}, min/max : {}/{} ",
        ccy1Price, ccy2Price, minRate, maxRate);

      if (minRate != null && maxRate != null) {
        message.reply(new JsonObject(
          ImmutableMap.of(MIN_KEY, minRate.toString(), MAX_KEY, maxRate.toString())
        ));
      } else {
        //TODO: come up with a proper handling.
        message.fail(FAILURE_CODE, "Unable to process request");
      }
    });
  }

  private void updateRates() {
    if (ccy1Price != null && ccy2Price != null) {
      BigDecimal currentRate = ccy1Price.divide(ccy2Price, SCALE, RoundingMode.HALF_UP);
      minRate = minRate == null ? currentRate : minRate.min(currentRate);
      maxRate = maxRate == null ? currentRate : maxRate.max(currentRate);
      Logger.debug("Rate updated for ccy1/ccy2 : {}/{}, min/max : {}/{} ", ccy1Price, ccy2Price, minRate, maxRate);
    }
  }

}
