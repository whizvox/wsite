package me.whizvox.wsite.database;

import org.jooq.DSLContext;
import org.jooq.Table;

public class JooqRepository<TABLE extends Table> {

  protected DSLContext jooq;
  protected TABLE table;

  public JooqRepository(DSLContext jooq, TABLE table) {
    this.jooq = jooq;
    this.table = table;
  }

  public boolean create() {
    return jooq.createTableIfNotExists(table)
        .columns(table.fields())
        .execute() > 0;
  }

  public boolean drop() {
    return jooq.dropTableIfExists(table)
        .execute() > 0;
  }

}
