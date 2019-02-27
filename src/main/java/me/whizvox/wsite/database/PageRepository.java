package me.whizvox.wsite.database;

import me.whizvox.wsite.generated.tables.Pages;
import me.whizvox.wsite.generated.tables.records.PagesRecord;
import me.whizvox.wsite.util.Utils;
import org.jooq.DSLContext;
import org.jooq.Result;

import java.sql.Timestamp;
import java.util.List;

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

}
