package com.epam.vertx;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

  private static final Logger Logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Future<Void> startFuture) {
    Logger.debug("Starting main verticle ..");

    ConfigRetriever retriever = ConfigRetriever.create(vertx,
      new ConfigRetrieverOptions().addStore(
        new ConfigStoreOptions().setType("file")
          .setConfig(new JsonObject().put("path", "config/config.json"))));

    retriever.getConfig(result -> {
      if (result.failed()) {
        Logger.error("Unable to load configuration...");
        startFuture.failed();
      } else {
        vertx.deployVerticle(new CurrencyUpdateVerticle(), new DeploymentOptions().setConfig(result.result()));
        vertx.deployVerticle(new RestControllerVerticle());
      }
    });
    startFuture.complete();
  }
}
