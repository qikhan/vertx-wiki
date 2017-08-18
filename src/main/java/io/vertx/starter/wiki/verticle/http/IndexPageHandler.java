package io.vertx.starter.wiki.verticle.http;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by qikhan on 7/19/2017.
 */
public class IndexPageHandler {

  private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();

  public void process(RoutingContext context, Vertx vertx, String wikiDbQueue) {

    DeliveryOptions options = new DeliveryOptions().addHeader("action", "all-pages");

    vertx.eventBus()
      .send(
        wikiDbQueue
        , new JsonObject()
        , options
        , reply -> {

          if (reply.succeeded()) {
            JsonObject body = (JsonObject) reply.result().body();
            context.put("title", "Wiki home");
            context.put("pages", body.getJsonArray("pages").getList());
            templateEngine.render(
              context
              , "templates/index.ftl"
              , ar -> {
                  if (ar.succeeded()) {
                    context.response().putHeader("Content-Type", "text/html");
                    context.response().end(ar.result());
                  } else {
                    context.fail(ar.cause());
                  }
                }
            );
          } else {
            context.failed();
          }
        }
      );
  }
}
