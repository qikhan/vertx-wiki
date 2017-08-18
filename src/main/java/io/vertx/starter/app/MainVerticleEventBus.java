package io.vertx.starter.app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.starter.wiki.*;
import io.vertx.starter.wiki.verticle.db.DatabaseClientVerticle;

public class MainVerticleEventBus extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Future<String> dbVerticleDeployment = Future.future();

    vertx.deployVerticle(new DatabaseClientVerticle(), dbVerticleDeployment.completer());

    dbVerticleDeployment
      .compose(id -> {
        Future<String> httpVerticleDeployment = Future.future();
        vertx.deployVerticle(
          "io.vertx.starter.wiki.verticle.http.HttpServerVerticle"
          , new DeploymentOptions().setInstances(2)
          , httpVerticleDeployment.completer()
        );
        return httpVerticleDeployment;
      }
      )
    .setHandler(ar -> {
      if (ar.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(ar.cause());
      }
    });
  }
}
