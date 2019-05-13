package me.whizvox.wsite.database;

import me.whizvox.wsite.generated.tables.Pages;
import me.whizvox.wsite.generated.tables.records.PagesRecord;
import me.whizvox.wsite.util.Utils;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static me.whizvox.wsite.generated.tables.Pages.PAGES;

public class PageRepository extends JooqRepository<Pages> {

  public PageRepository(DSLContext jooq) {
    super(jooq, Pages.PAGES);
  }

  public boolean insert(Page page) {
    return jooq.insertInto(table)
        .set(PARSER.fromPojo(page))
        .execute() > 0;
  }

  public boolean update(Page page) {
    return jooq.update(table)
        .set(PARSER.fromPojo(page))
        .where(table.PATH.equalIgnoreCase(page.path))
        .execute() > 0;
  }

  public boolean delete(String path) {
    return jooq.deleteFrom(table)
        .where(table.PATH.equalIgnoreCase(path))
        .execute() > 0;
  }

  public Page selectFromPath(String path) {
    PagesRecord record = jooq.selectFrom(table)
        .where(table.PATH.equalIgnoreCase(path))
        .fetchAny();
    return PARSER.fromRecord(record);
  }

  public List<Page> selectFromSyntax(Page.Syntax syntax) {
    Result<PagesRecord> records = jooq.selectFrom(table)
	.where(table.SYNTAX.equalIgnoreCase(syntax.toString()))
	.fetch();
    return PARSER.fromRecords(records);
  }

  public List<PageSummary> selectList(int limit, int page, OrderingScheme orderingScheme, boolean descending) {
    Result<Record6<String, String, Integer, String, Timestamp, Timestamp>> records =
        jooq.select(PAGES.PATH, PAGES.TITLE, PAGES.CONTENTS.length(), PAGES.SYNTAX, PAGES.PUBLISHED, PAGES.LAST_EDITED)
        .from(table)
        .orderBy(descending ? orderingScheme.descFields : orderingScheme.ascFields)
        .limit(limit)
        .offset(limit * page)
        .fetch();
    List<PageSummary> summaries = new ArrayList<>(records.size());
    records.forEach(r -> summaries.add(
        new PageSummary(r.value1(), r.value2(), r.value3(), r.value4(), r.value5(), r.value6())
    ));
    return summaries;
  }

  public int getCount() {
    return jooq.fetchCount(table);
  }

  static RecordParser<Page, PagesRecord> PARSER = new RecordParser<Page, PagesRecord>() {
    @Override
    public Page fromRecord(PagesRecord record) {
      if (record == null) {
        return null;
      }
      Page page = new Page();
      page.path = record.getPath();
      page.title = record.getTitle();
      page.contents = record.getContents();
      page.syntax = Page.Syntax.fromString(record.getSyntax());
      page.published = Utils.timestampToInstant(record.getPublished());
      if (record.getLastEdited() != null) {
        page.lastEdited = Utils.timestampToInstant(record.getLastEdited());
      }
      return page;
    }
    @Override
    public PagesRecord fromPojo(Page page) {
      if (page == null) {
        return null;
      }
      return new PagesRecord(page.path, page.title, page.contents, page.syntax.toString(),
          Timestamp.from(page.published), page.lastEdited == null ? null : Timestamp.from(page.lastEdited));
    }
  };

  public static class PageSummary {
    public String path;
    public String title;
    public int contentLength;
    public String syntax;
    public String published;
    public String lastEdited;
    public PageSummary(String path, String title, int contentLength, String syntax, Timestamp published, Timestamp lastEdited) {
      this.path = path;
      this.title = title;
      this.contentLength = contentLength;
      this.syntax = syntax;
      this.published = Utils.formatFileSafeInstant(Utils.timestampToInstant(published));
      this.lastEdited = lastEdited == null ? null : Utils.formatFileSafeInstant(Utils.timestampToInstant(lastEdited));
    }
    public PageSummary() {
    }
  }

  public enum OrderingScheme {
    PATH(Pages.PAGES.PATH),
    TITLE(Pages.PAGES.TITLE, Pages.PAGES.PATH),
    CONTENTS_LENGTH(DSL.charLength(Pages.PAGES.CONTENTS), Pages.PAGES.PATH),
    SYNTAX(Pages.PAGES.SYNTAX, Pages.PAGES.PATH),
    PUBLISHED(Pages.PAGES.PUBLISHED, Pages.PAGES.PATH),
    LAST_EDITED(Pages.PAGES.LAST_EDITED, Pages.PAGES.PATH);

    public final OrderField[] ascFields;
    public final OrderField[] descFields;

    OrderingScheme(Field... fields) {
      ascFields = fields;
      descFields = new OrderField[fields.length];
      descFields[0] = fields[0].desc();
      System.arraycopy(fields, 1, descFields, 1, descFields.length - 1);
    }

    public static OrderingScheme fromString(String str) {
      for (OrderingScheme value : values()) {
        if (value.toString().equalsIgnoreCase(str)) {
          return value;
        }
      }
      return null;
    }

  }

}
