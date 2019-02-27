package me.whizvox.wsite.database;

import me.whizvox.wsite.generated.tables.Users;
import me.whizvox.wsite.generated.tables.records.UsersRecord;
import me.whizvox.wsite.util.Utils;
import org.jooq.DSLContext;
import org.jooq.Result;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UserRepository extends JooqRepository<Users> {

  public UserRepository(DSLContext jooq) {
    super(jooq, Users.USERS);
  }

  public boolean insert(User user) {
    return jooq.insertInto(table)
        .set(PARSER.fromPojo(user))
        .execute() > 0;
  }

  public boolean update(User user) {
    UsersRecord record = PARSER.fromPojo(user);
    return jooq.update(table)
        .set(record)
        .where(table.ID.equal(record.getId()))
        .execute() > 0;
  }

  public boolean delete(UUID id) {
    return jooq.deleteFrom(table)
        .where(table.ID.eq(id.toString()))
        .execute() > 0;
  }

  public User selectFromId(UUID id) {
    UsersRecord record = jooq.selectFrom(table)
        .where(table.ID.eq(id.toString()))
        .fetchAny();
    return PARSER.fromRecord(record);
  }

  public User selectFromUsername(String username) {
    UsersRecord record = jooq.selectFrom(table)
        .where(table.USERNAME.equalIgnoreCase(username))
        .fetchAny();
    return PARSER.fromRecord(record);
  }

  public User selectFromEmailAddress(String emailAddress) {
    UsersRecord record = jooq.selectFrom(table)
        .where(table.EMAIL_ADDRESS.equalIgnoreCase(emailAddress))
        .fetchAny();
    return PARSER.fromRecord(record);
  }

  public List<User> selectWhenCreated(Instant instant, boolean before) {
    Timestamp ts = Timestamp.from(instant);
    Result<UsersRecord> records = jooq.selectFrom(table)
        .where(before ? table.WHEN_CREATED.lessOrEqual(ts) : table.WHEN_CREATED.greaterOrEqual(ts))
        .fetch();
    return PARSER.fromRecords(records);
  }

  public int selectNumberOfUsers() {
    return jooq.fetchCount(table);
  }

  static RecordParser<User, UsersRecord> PARSER = new RecordParser<User, UsersRecord>() {
    @Override
    public User fromRecord(UsersRecord record) {
      if (record == null) {
        return null;
      }
      User user = new User();
      user.id = UUID.fromString(record.getId());
      user.username = record.getUsername();
      user.emailAddress = record.getEmailAddress();
      user.password = record.getPassword();
      user.operator = record.getOperator();
      user.whenCreated = Utils.timestampToInstant(record.getWhenCreated());
      return user;
    }
    @Override
    public UsersRecord fromPojo(User user) {
      if (user == null) {
        return null;
      }
      return new UsersRecord(user.id.toString(), user.username, user.emailAddress, user.password, user.operator,
          Timestamp.from(user.whenCreated));
    }
  };

}
