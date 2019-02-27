package me.whizvox.wsite.database;

import me.whizvox.wsite.generated.tables.Logins;
import me.whizvox.wsite.generated.tables.records.LoginsRecord;
import me.whizvox.wsite.util.Utils;
import org.jooq.DSLContext;
import org.jooq.Result;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class LoginRepository extends JooqRepository<Logins> {

  public LoginRepository(DSLContext jooq) {
    super(jooq, Logins.LOGINS);
  }

  public boolean insert(Login login) {
    return jooq.insertInto(table)
        .set(PARSER.fromPojo(login))
        .execute() > 0;
  }

  public boolean delete(String token) {
    return jooq.deleteFrom(table)
        .where(table.TOKEN.equal(token))
        .execute() > 0;
  }

  public boolean deleteFromUserId(UUID userId) {
    return jooq.deleteFrom(table)
        .where(table.USER_ID.equal(userId.toString()))
        .execute() > 0;
  }

  public int deleteAllExpired() {
    return jooq.deleteFrom(table)
        .where(table.EXPIRATION_DATE.lessOrEqual(Timestamp.from(Instant.now())))
        .execute();
  }

  public Login selectFromToken(String token) {
    LoginsRecord record = jooq.selectFrom(table)
        .where(table.TOKEN.equal(token))
        .fetchAny();
    return PARSER.fromRecord(record);
  }

  public Login selectFromClientInfo(UUID userId, String userAgent, String ipAddress) {
    LoginsRecord record = jooq.selectFrom(table)
        .where(table.USER_ID.equal(userId.toString())
            .and(table.USER_AGENT.equal(userAgent))
            .and(table.IP_ADDRESS.equal(ipAddress)))
        .fetchAny();
    return PARSER.fromRecord(record);
  }

  public List<Login> selectFromUserId(UUID userId) {
    Result<LoginsRecord> records = jooq.selectFrom(table)
        .where(table.USER_ID.equal(userId.toString()))
        .fetch();
    return PARSER.fromRecords(records);
  }

  public List<Login> selectFromUserAgent(String userAgent) {
    Result<LoginsRecord> records = jooq.selectFrom(table)
        .where(table.USER_AGENT.equal(userAgent))
        .fetch();
    return PARSER.fromRecords(records);
  }

  public List<Login> selectFromIpAddress(String ipAddress) {
    Result<LoginsRecord> records = jooq.selectFrom(table)
        .where(table.IP_ADDRESS.equal(ipAddress))
        .fetch();
    return PARSER.fromRecords(records);
  }

  static RecordParser<Login, LoginsRecord> PARSER = new RecordParser<Login, LoginsRecord>() {
    @Override
    public Login fromRecord(LoginsRecord record) {
      if (record == null) {
        return null;
      }
      Login login = new Login();
      login.token = record.getToken();
      login.userId = UUID.fromString(record.getUserId());
      login.userAgent = record.getUserAgent();
      login.ipAddress = record.getIpAddress();
      login.expirationDate = Utils.timestampToInstant(record.getExpirationDate());
      return login;
    }

    @Override
    public LoginsRecord fromPojo(Login login) {
      if (login == null) {
        return null;
      }
      return new LoginsRecord(login.token, login.userId.toString(), login.userAgent, login.ipAddress, Timestamp.from(login.expirationDate));
    }
  };

}
