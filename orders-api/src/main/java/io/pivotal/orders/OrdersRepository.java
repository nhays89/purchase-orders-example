package io.pivotal.orders;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.uuid.Generators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

@Repository
public class OrdersRepository {

    private static final String TABLE_NAME = "PURCHASE_ORDER";

    private JdbcTemplate jdbcTemplate;
    private SimpleJdbcInsert simpleJdbcInsert;

    @Autowired
    public OrdersRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName(TABLE_NAME);
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        return jdbcTemplate.queryForObject(sql, Integer.class); 
    }

    public Order findById(UUID id) {
        Assert.notNull(id, "Cannot execute findById because id was null!");
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE ID = ?";
        return jdbcTemplate.queryForObject(sql, new Object[] { id.toString() }, new BeanPropertyRowMapper<Order>(Order.class));
    }

    public UUID create(Order order) {
        Assert.notNull(order, "Cannot create a new line item because order must not be null!");
        Assert.isNull(order.getId(), "Cannot execute create new line item because id must be null");
        UUID newId = Generators.timeBasedGenerator().generate();
        order.id(newId);
        SqlParameterSource parameterSource = new BeanPropertySqlParameterSource(order);
        simpleJdbcInsert.execute(parameterSource);
        return newId;
    }

    public int delete(UUID id) {
        Assert.notNull(id, "Cannot execute delete because id was null!");
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE ID = ?";
        return jdbcTemplate.update(sql, new Object[] { id.toString() });
    }

    public List<Order> findByCreatedDate(LocalDate dateCreated) {
        Assert.notNull(dateCreated, "Cannot execute findByCreatedDate because dateCreated was null!");
        SqlParameterSource in = new MapSqlParameterSource().addValue("P_DATE_CREATED", Timestamp.valueOf(dateCreated.atStartOfDay()));
        SimpleJdbcCall simpleJdbcCall = 
            new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("FETCH_POS_BY_DATE_CREATED")
                    .useInParameterNames("P_DATE_CREATED")
                    .returningResultSet("P_RESULT", new BeanPropertyRowMapper<Order>(Order.class));
        Map<String, Object> map = simpleJdbcCall.execute(in);
        return (List<Order>) map.get("P_RESULT");
    }
}