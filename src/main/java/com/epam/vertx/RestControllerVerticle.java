package com.epam.vertx;

import com.google.common.collect.Sets;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * The Rest Controller Verticle
 * All HTTP requests are handled by a controller verticle.
 */
public class RestControllerVerticle extends AbstractVerticle {
  private static final Logger Logger = LoggerFactory.getLogger(RestControllerVerticle.class);
  private static final String CCY1 = "ccy1";
  private static final String CCY2 = "ccy2";

  private Set<String> trackingCurrency = Sets.newHashSet();

  @Override
  public void start(Future<Void> startFuture) {
    Logger.debug("Starting rest verticle....");

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.get("/").handler(event -> event.response().end("Cryptocurrencys volatility tracker"));
    router.post("/cryptos/").handler(this::addCurrencyPairToTrack);
    router.get("/cryptos/:ccy1/:ccy2").handler(currencyPairGetMinMaxRateHandler);

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080, listenHandler -> {

        if (listenHandler.succeeded()) {
          Logger.debug("Http Server started.");
          startFuture.complete();
        } else {
          startFuture.fail(listenHandler.cause());
        }
      });

  }

  private Handler<RoutingContext> currencyPairGetMinMaxRateHandler = routingContext -> {
    HttpServerRequest request = routingContext.request();
    Logger.debug("Getting min/max rate for currencies: {}", request);

    String ccy1 = request.getParam(CCY1);
    String ccy2 = request.getParam(CCY2);

    if (trackingCurrency.contains(ccy1+ccy2)) {
      vertx.eventBus().send(ccy1 + "_" + ccy2, "get current rates", event -> {
        if (event.succeeded()) {

          routingContext.response()
            .putHeader("content-type", "application/json")
            .end(event.result().body().toString());
        } else {
          if (event.failed()) {
            routingContext.response().setStatusCode(500).end("Internal Error occurred");
          }
        }
      });
    } else {
      routingContext.response().setStatusCode(404).end("The requested currency pair is not tracked by application." +
        "You can add currency pair to be tracked using REST API: method POST, URI: /cryptos/, body example:" +
        " {\n" +
        " \"ccy1\": \"ETH\",\n" +
        " \"ccy2\": \"DASH\"\n" +
        "}.");
    }
  };

  private void addCurrencyPairToTrack(RoutingContext routingContext) {
    JsonObject message = routingContext.getBodyAsJson();

    String ccy1 = message.getString(CCY1);
    String ccy2 = message.getString(CCY2);

    if(trackingCurrency.add(ccy1+ccy2)) {
      Logger.debug("Adding a new currencies pair to track ccy1/cc12: {}/{}",
        ccy1, ccy2);

      vertx.deployVerticle(new CurrencyPairTrackVerticle(ccy1, ccy2));
      routingContext.response().end();
    } else {
      routingContext.response().end("Currencies are already tracking.");
    }
  }
}
