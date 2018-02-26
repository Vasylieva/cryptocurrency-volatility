package com.epam.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Currency update verticle.
 * Current verticle reads dollar rates for all specified crypto-currencies on schedule (e.g. every 5 seconds) and publishes
 * an update per each currency separately. To subscribe to updates for a specific currency can be made by listening on a
 * topic that equals to the currency name.
 * For example to listen to updates for ETH currency you need to yse "ETH" as address value.
 *
 */
public class CurrencyUpdateVerticle extends AbstractVerticle {
  private static final Logger Logger = LoggerFactory.getLogger(CurrencyUpdateVerticle.class);

  @Override
  public void start() {
    Logger.debug("Starting periodic HttpClient to request latest price...");

    WebClient client = WebClient.create(vertx);

    vertx.setPeriodic(5000, timerId -> {

      String cryptoCurrencies = config().getString("cryptoCurrencies");
      String currencies = config().getString("currencies");

      client.getAbs("https://min-api.cryptocompare.com/data/pricemulti")
        .addQueryParam("fsyms", cryptoCurrencies)
        .addQueryParam("tsyms", currencies)
        .as(BodyCodec.jsonObject())
        .send(asyncResultHandler);
    });
  }

  private final Handler<AsyncResult<HttpResponse<JsonObject>>> asyncResultHandler = responseResult -> {
    if (responseResult.succeeded()) {

      HttpResponse<JsonObject> response = responseResult.result();
      JsonObject body = response.body();

      Logger.debug("Data update received: {}", body);

      body.fieldNames().forEach(currency ->
        vertx.eventBus().publish(currency, body.getJsonObject(currency).getDouble("USD"))
      );
    } else {
      Logger.error("Something went wrong " + responseResult.cause().getMessage());
    }
  };
}
