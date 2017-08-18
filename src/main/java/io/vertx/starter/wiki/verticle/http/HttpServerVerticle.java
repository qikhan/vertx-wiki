package io.vertx.starter.wiki.verticle.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.starter.wiki.verticle.http.DeletePageHandler;
import io.vertx.starter.wiki.verticle.http.IndexPageHandler;
import io.vertx.starter.wiki.verticle.http.RenderPageHandler;
import io.vertx.starter.wiki.verticle.http.UpdatePageHandler;

/**
 * Created by qikhan on 8/16/2017.
 */
public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  // inject conf
  //  java -jar target/my-first-app-1.0-SNAPSHOT-fat.jar -conf src/main/conf/my-application-conf.json
  public static final int HTTP_PORT = 8080;
  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";

  private String wikiDbQueue = "wikidb.queue";

  private IndexPageHandler indexPageHandler = new IndexPageHandler();
  private RenderPageHandler renderPageHandler = new RenderPageHandler();
  private UpdatePageHandler updatePageHandler = new UpdatePageHandler();
  private DeletePageHandler deletePageHandler = new DeletePageHandler();

  @Deprecated
  private JDBCClient dbClient;

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    LOGGER.info("Initializing http server and app router");

    wikiDbQueue = config().getString(CONFIG_WIKIDB_QUEUE, "wikidb.queue");

    HttpServer server = vertx.createHttpServer();
    Router router = buildRouter();
    Integer httpPort = config().getInteger(CONFIG_HTTP_SERVER_PORT, HTTP_PORT);
    server
      .requestHandler(router::accept)
      .listen(
        httpPort
        , ar -> {
          if (ar.succeeded()) {
            LOGGER.info("HTTP server running on port " + httpPort);
            startFuture.complete();
          } else {
            LOGGER.error("Could not start a HTTP server", ar.cause());
            startFuture.fail(ar.cause());
          }
        }
      );
  }

  private Router buildRouter() {

    Router router = Router.router(vertx);

    router.get("/").handler(this::indexHandler);
    router.get("/wiki/:page").handler(this::pageRenderingHandler);
    router.post().handler(BodyHandler.create());
    router.post("/save").handler(this::pageUpdateHandler);
    router.post("/create").handler(this::pageCreateHandler);
    router.post("/delete").handler(this::pageDeletionHandler);

    return router;

  }

  private void pageDeletionHandler(RoutingContext context) {
    deletePageHandler.process(context, dbClient);
  }

  private void pageCreateHandler(RoutingContext context) {
    String pageName = context.request().getParam("name");
    String location = "/wiki/" + pageName;
    if (pageName == null || pageName.isEmpty()) {
      location = "/";
    }
    context.response().setStatusCode(303);
    context.response().putHeader("Location", location);
    context.response().end();
  }

  private void pageUpdateHandler(RoutingContext context) {

    updatePageHandler.process(context, dbClient);
  }

  private void pageRenderingHandler(RoutingContext context) {

    renderPageHandler.process(context, dbClient);
  }

  private void indexHandler(RoutingContext context) {
    indexPageHandler.process(context, vertx, wikiDbQueue);
  }
}
