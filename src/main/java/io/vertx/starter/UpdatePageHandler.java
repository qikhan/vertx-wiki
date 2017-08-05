package io.vertx.starter;

import com.github.rjeschke.txtmark.Processor;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;

import java.util.Date;

/**
 * Created by qikhan on 7/19/2017.
 */
public class UpdatePageHandler {

  public void process(RoutingContext context, JDBCClient dbClient) {

      String id = context.request().getParam("id");
      String title = context.request().getParam("title");
      String markdown = context.request().getParam("markdown");
      boolean newPage = "yes".equals(context.request().getParam("newPage"));

      dbClient.getConnection(car -> {

        if (car.succeeded()) {
          SQLConnection connection = car.result();
          String sql = newPage ? DatabaseClientBuilder.SQL_CREATE_PAGE : DatabaseClientBuilder.SQL_SAVE_PAGE;

          JsonArray params = new JsonArray();

          if (newPage) {
            params.add(title).add(markdown);
          } else {
            params.add(markdown).add(id);
          }

          connection.updateWithParams(sql, params, res -> {
            connection.close();
            if (res.succeeded()) {
              context.response().setStatusCode(303);
              context.response().putHeader("Location", "/wiki/" + title);
              context.response().end();
            } else {
              context.fail(res.cause());
            }
          });
        } else {
          context.fail(car.cause());
        }
      });
  }
}
