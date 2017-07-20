package io.vertx.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  private JDBCClient dbClient;
  private IndexPageHandler indexPageHandler = new IndexPageHandler();

  @Override
  public void start(Future<Void> startFuture) {
    Future<Void> steps = initDatabase().compose(v -> initHttpServer());
    steps.setHandler(startFuture.completer());
  }

  private Future<Void> initDatabase() {
    Future<Void> future = Future.future();

    Future<JDBCClient> dbClientFuture = DatabaseClientBuilder.build(vertx);
    if (dbClientFuture.failed()) {
      future.fail(dbClientFuture.cause());
    } else {
      dbClient = dbClientFuture.result();
      future.complete();
      LOGGER.debug("DB client creation completed.");
    }
    return future;
  }

  private Future<Void> initHttpServer() {
    Future<Void> future = Future.future();
    HttpServer server = vertx.createHttpServer();

    Router router = buildRouter();

    server
      .requestHandler(router::accept)
      .listen(8080, ar -> {
        if (ar.succeeded()) {
          LOGGER.info("HTTP server running on port 8080");
          future.complete();
        } else {
          LOGGER.error("Could not start a HTTP server", ar.cause());
          future.fail(ar.cause());
        }
      });
    return future;
  }

  private Router buildRouter() {

    Router router = Router.router(vertx);

    router.get("/").handler(this::indexHandler);
    router.get("/wiki/:page").handler(this::pageRenderingHandler);
    router.post().handler(BodyHandler.create());
    router.put("/save").handler(this::pageUpdateHandler);
    router.post("/create").handler(this::pageCreateHandler);
    router.delete("/delete").handler(this::pageDeletionHandler);

    return router;
  }

  private void pageDeletionHandler(RoutingContext routingContext) {
    routingContext.response().setStatusCode(200);
    routingContext.response().end();
  }

  private void pageCreateHandler(RoutingContext routingContext) {
    routingContext.response().setStatusCode(201);
    routingContext.response().end();
  }

  private void pageUpdateHandler(RoutingContext routingContext) {

    routingContext.response().end("<h2>pageUpdateHandler</h2>");

  }

  private void pageRenderingHandler(RoutingContext routingContext) {
    String page = routingContext.request().getParam("page");
    routingContext.response().setStatusCode(200);
    routingContext.response().end("<h2>pageRenderingHandler ::" +
      page +"</h2>");
  }

  private void indexHandler(RoutingContext routingContext) {
    indexPageHandler.process(routingContext, dbClient);
    //    routingContext.response().setStatusCode(200);
//    routingContext.response().end("<h2>indexHandler</h2>");
  }
}
