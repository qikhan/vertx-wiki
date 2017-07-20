package io.vertx.starter;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;


/**
 * Created by qikhan on 7/13/2017.
 */
public class DatabaseClientBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseClientBuilder.class);

  private static final String SQL_CREATE_PAGES_TABLE = "create table " +
    " if not exists Pages " +
    " ( " +
    "  Id integer identity primary key " +
    ", Name varchar(255) unique " +
    ", Content clob " +
    ")";

  public static final String SQL_GET_PAGE = "select Id, Content from Pages where Name = ?";
  public static final String SQL_CREATE_PAGE = "insert into Pages values (NULL, ?, ?)";
  public static final String SQL_SAVE_PAGE = "update Pages set Content = ? where Id = ?";
  public static final String SQL_ALL_PAGES = "select Name from Pages";
  public static final String SQL_DELETE_PAGE = "delete from Pages where Id = ?";

  public static Future<JDBCClient> build(Vertx vertx) {

    Future<JDBCClient> future = Future.future();

    JDBCClient jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", "jdbc:hsqldb:file:db/wiki")
      .put("driver_class", "org.hsqldb.jdbcDriver")
      .put("max_pool_size", 30));

    jdbcClient.getConnection(ar -> {
      if (ar.failed()) {
        LOGGER.error("Could not open a database connection", ar.cause());
        future.fail(ar.cause());
      } else {
        SQLConnection connection = ar.result();
        connection.execute(SQL_CREATE_PAGES_TABLE, create -> {
          connection.close();
          if (create.failed()) {
            LOGGER.error("Database preparation error", create.cause());
            future.fail(create.cause());
          }
        });
      }
    });
    future.complete(jdbcClient);
    return future;
  }
}
