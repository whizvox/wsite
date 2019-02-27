package me.whizvox.wsite.database;

import org.jooq.Record;
import org.jooq.Result;

import java.util.ArrayList;
import java.util.List;

public interface RecordParser<POJO, RECORD extends Record> {

  POJO fromRecord(RECORD record);

  RECORD fromPojo(POJO obj);

  default List<POJO> fromRecords(Result<RECORD> records) {
    List<POJO> objs = new ArrayList<>(records.size());
    records.forEach(record -> objs.add(fromRecord(record)));
    return objs;
  }

}
