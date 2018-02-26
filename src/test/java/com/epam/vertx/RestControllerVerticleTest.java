package com.epam.vertx;

  import io.vertx.core.Vertx;
  import io.vertx.core.http.HttpClient;
  import io.vertx.ext.unit.Async;
  import io.vertx.ext.unit.TestContext;
  import io.vertx.ext.unit.junit.VertxUnitRunner;
  import org.junit.After;
  import org.junit.Before;
  import org.junit.Test;
  import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class RestControllerVerticleTest {
  private Vertx vertx;

  @Before
  public void setUp(TestContext tc) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(RestControllerVerticle.class.getName(), tc.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext tc) {
    vertx.close(tc.asyncAssertSuccess());
  }

  @Test
  public void testThatTheServerIsStarted(TestContext tc) {
    Async async = tc.async();
    vertx.createHttpClient().getNow(8080, "localhost", "/", response -> {
      tc.assertEquals(response.statusCode(), 200);
      response.bodyHandler(body -> {
        tc.assertTrue(body.length() > 0);
        tc.assertEquals("Cryptocurrencys volatility tracker", body.toString());
        async.complete();
      });
    });
  }

  @Test
  public void testThatTheServerReturnsNotFoundForNonTrackedCurrenciesRequest(TestContext tc) {
    Async async = tc.async();
    vertx.createHttpClient().getNow(8080, "localhost", "/cryptos/ETH/DASH", response -> {
      tc.assertEquals(response.statusCode(), 404);
      response.bodyHandler(body -> {
        tc.assertTrue(body.length() > 0);
        async.complete();
      });
    });
  }

  @Test
  public void testCurrencyPairAdded(TestContext tc) {
    Async async = tc.async();
    String requestBody = "{\"ccy1\":\"ETH\",\"ccy2\":\"DASH\"}";

    vertx.createHttpClient().post(8080, "localhost", "/cryptos/", response -> {
      tc.assertEquals(response.statusCode(), 200);
      response.bodyHandler(body ->
        async.complete()
      );
    }).putHeader("Content-Type", "application/json")
      .putHeader("Content-Length", String.valueOf(requestBody.length()))
      .write(requestBody)
      .end();
  }

  @Test
  public void testSameCurrencyPairIsNotAddedTwice(TestContext tc) {
    Async async = tc.async();
    String requestBody = "{\"ccy1\":\"ETH\",\"ccy2\":\"DASH\"}";

    HttpClient httpClient = vertx.createHttpClient();
    httpClient.post(8080, "localhost", "/cryptos/", response -> {
      tc.assertEquals(response.statusCode(), 200);
      response.bodyHandler(body -> {
        tc.assertTrue(body.length() == 0);
        async.complete();
      });
    }).putHeader("Content-Type", "application/json")
      .putHeader("Content-Length", String.valueOf(requestBody.length()))
      .write(requestBody)
      .end();

    Async async1 = tc.async();
    httpClient.post(8080, "localhost", "/cryptos/", response -> {
      tc.assertEquals(response.statusCode(), 200);
      response.bodyHandler(body -> {
        tc.assertTrue(body.length() > 0);
        tc.assertEquals("Currencies are already tracking.", body.toString());
        async1.complete();
      });
    }).putHeader("Content-Type", "application/json")
      .putHeader("Content-Length", String.valueOf(requestBody.length()))
      .write(requestBody)
      .end();
  }
}
