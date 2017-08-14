package io.vertx.starter.app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.starter.wiki.DatabaseClientBuilder;
import io.vertx.starter.wiki.IndexPageHandler;
import io.vertx.starter.wiki.RenderPageHandler;
import io.vertx.starter.wiki.UpdatePageHandler;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
  public static final int HTTP_PORT = 8080;

  private JDBCClient dbClient;
  private IndexPageHandler indexPageHandler = new IndexPageHandler();
  private RenderPageHandler renderPageHandler = new RenderPageHandler();
  private UpdatePageHandler updatePageHandler = new UpdatePageHandler();

  @Override
  public void start(Future<Void> startFuture) {
    Future<Void> steps = initDatabase().compose(v -> initHttpServer());
    steps.setHandler(startFuture.completer());
  }

  private Future<Void> initDatabase() {
    Future<Void> future = Future.future();

    DatabaseClientBuilder.build(vertx).setHandler(dbClientFuture -> {
      if (dbClientFuture.failed()) {
        future.fail(dbClientFuture.cause());
      } else {
        dbClient = dbClientFuture.result();
        future.complete();
        LOGGER.debug("DB client creation completed.");
      }
    });
    return future;
  }

  private Future<Void> initHttpServer() {
    Future<Void> future = Future.future();
    HttpServer server = vertx.createHttpServer();
    LOGGER.info("Initializing http server and app router");
    Router router = buildRouter();

    server
      .requestHandler(router::accept)
      .listen(HTTP_PORT, ar -> {
        if (ar.succeeded()) {
          LOGGER.info("HTTP server running on port " + HTTP_PORT);
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
    router.post("/save").handler(this::pageUpdateHandler);
    router.post("/create").handler(this::pageCreateHandler);
    router.delete("/delete").handler(this::pageDeletionHandler);

    return router;
  }

  private void pageDeletionHandler(RoutingContext context) {
    context.response().setStatusCode(200);
    context.response().end();
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
//    String page = context.request().getParam("page");
//    context.response().setStatusCode(200);
//    context.response().end("<h2>pageRenderingHandler ::" +
//      page +"</h2>");
  }

  private void indexHandler(RoutingContext context) {
    indexPageHandler.process(context, dbClient);
    //    context.response().setStatusCode(200);
    //    context.response().end("<h2>indexHandler</h2>");
  }
}
