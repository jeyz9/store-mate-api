package com.sm.jeyz9.storemateapi.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sm.jeyz9.storemateapi.dto.OrderChannelReteDTO;
import com.sm.jeyz9.storemateapi.dto.OwnerDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.OrderChannelIncomeDTO;
import com.sm.jeyz9.storemateapi.dto.ProductAlertDTO;
import com.sm.jeyz9.storemateapi.dto.RegionalOrderAnalyticsDTO;
import com.sm.jeyz9.storemateapi.dto.RegionalRevenueAnalyticsDTO;
import com.sm.jeyz9.storemateapi.dto.RegionalRevenueDTO;
import com.sm.jeyz9.storemateapi.dto.RegionalUserAnalyticsDTO;
import com.sm.jeyz9.storemateapi.dto.ReviewDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.SalesAnalyticsDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.SalesPercentageDTO;
import com.sm.jeyz9.storemateapi.dto.UserChartDTO;
import com.sm.jeyz9.storemateapi.dto.WeeklyActiveIncomeChartDTO;
import com.sm.jeyz9.storemateapi.dto.YearActiveIncomeChartDTO;
import com.sm.jeyz9.storemateapi.dto.YearActiveOrderChartDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class OwnerDashboardRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public OwnerDashboardRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }
    
//    public Optional<OwnerDashboardDTO> findOwnerDashboard1() {
//        String sql = """
//            SELECT
//                COUNT(
//                        CASE
//                            WHEN last_seen_at >= NOW() - INTERVAL '5 minutes'
//                                THEN 1
//                            END
//                ) AS "activeUsers",
//                COUNT(
//                        CASE
//                            WHEN DATE_TRUNC('month', created_at) = DATE_TRUNC('month', NOW())
//                                THEN 1
//                            END
//                ) AS "newUsers",
//                    (
//                          SELECT json_build_object(
//                                         'thisWeek',
//                                         (SELECT COALESCE(json_agg(t), '[]')
//                                          FROM (SELECT EXTRACT(ISODOW FROM ual.activity_date) AS "dayOfWeek",
//                                                       CASE EXTRACT(ISODOW FROM ual.activity_date)
//                                                           WHEN 1 THEN 'วันจันทร์'
//                                                           WHEN 2 THEN 'วันอังคาร'
//                                                           WHEN 3 THEN 'วันพุธ'
//                                                           WHEN 4 THEN 'วันพฤหัสบดี'
//                                                           WHEN 5 THEN 'วันศุกร์'
//                                                           WHEN 6 THEN 'วันเสาร์'
//                                                           WHEN 7 THEN 'วันอาทิตย์'
//                                                           END                                AS "activityDate",
//                                                       COUNT(DISTINCT ual.user_id)            AS "totalUsers"
//                                                FROM user_activity_logs ual
//                                                WHERE ual.activity_date >= DATE_TRUNC('week', CURRENT_DATE)
//                                                GROUP BY ual.activity_date
//                                                ORDER BY "dayOfWeek") t),
//                                         'lastWeek',
//                                         (SELECT COALESCE(json_agg(t), '[]')
//                                          FROM (SELECT EXTRACT(ISODOW FROM ual.activity_date) AS "dayOfWeek",
//                                                       CASE EXTRACT(ISODOW FROM ual.activity_date)
//                                                           WHEN 1 THEN 'วันจันทร์'
//                                                           WHEN 2 THEN 'วันอังคาร'
//                                                           WHEN 3 THEN 'วันพุธ'
//                                                           WHEN 4 THEN 'วันพฤหัสบดี'
//                                                           WHEN 5 THEN 'วันศุกร์'
//                                                           WHEN 6 THEN 'วันเสาร์'
//                                                           WHEN 7 THEN 'วันอาทิตย์'
//                                                           END                                AS "activityDate",
//                                                       COUNT(DISTINCT ual.user_id)            AS "totalUsers"
//                                                FROM user_activity_logs ual
//                                                WHERE ual.activity_date >= DATE_TRUNC('week', CURRENT_DATE) - INTERVAL '7 days'
//                                                  AND ual.activity_date < DATE_TRUNC('week', CURRENT_DATE)
//                                                GROUP BY ual.activity_date
//                                                ORDER BY "dayOfWeek") t)
//                          )
//                      ) AS "weeklyActiveUsersChart",
//                (
//                    SELECT COALESCE(json_agg(t), '[]')
//                    FROM (
//                             SELECT
//                                 o.order_no AS "orderNo",
//                                 oa.recipient_name AS name,
//                                 o.status
//                             FROM orders o
//                                      LEFT JOIN users u ON o.user_id = u.id
//                                      LEFT JOIN order_address oa ON oa.order_id = o.id
//                             ORDER BY o.created_at DESC
//                             LIMIT 6
//                         ) t
//                ) AS "latestOrder",
//                (
//                    SELECT COALESCE(json_agg(t), '[]')
//                    FROM (
//                             SELECT
//                                 t.order_channel AS "orderChannel",
//                                 t.order_score AS "avg"
//                             FROM (
//                                      SELECT
//                                          o.order_channel,
//                                          ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM orders), 2) AS order_score
//                                      FROM orders o
//                                      GROUP BY o.order_channel
//                                  ) t
//                             GROUP BY t.order_channel, t.order_score
//                             ORDER BY avg DESC
//                         ) t
//                ) AS "orderChannelRate",
//                (
//                    SELECT COALESCE(json_agg(t), '[]')
//                    FROM (
//                             SELECT
//                                 CASE WHEN p.name IN (
//                                                      'กรุงเทพมหานคร',
//                                                      'นนทบุรี',
//                                                      'ปทุมธานี',
//                                                      'สมุทรปราการ',
//                                                      'นครปฐม',
//                                                      'สมุทรสาคร'
//                                     )
//                                          THEN 'กรุงเทพและปริมณฑล'
//                                      ELSE g.name
//                                     END AS geography,
//                                 COUNT(o) AS "totalOrders"
//                             FROM orders o
//                                      LEFT JOIN order_address oa ON o.id = oa.order_id
//                                      LEFT JOIN zipcode z ON z.id = oa.zipcode_id
//                                      LEFT JOIN provinces p ON p.id = z.province_id
//                                      LEFT JOIN geography g ON g.id = p.geo_id
//                             GROUP BY CASE WHEN p.name IN (
//                                                           'กรุงเทพมหานคร',
//                                                           'นนทบุรี',
//                                                           'ปทุมธานี',
//                                                           'สมุทรปราการ',
//                                                           'นครปฐม',
//                                                           'สมุทรสาคร'
//                                 )
//                                               THEN 'กรุงเทพและปริมณฑล'
//                                           ELSE g.name
//                                          END
//                             LIMIT 4
//                         ) t
//                ) AS "regionalRevenue",
//                (
//                    SELECT COALESCE(json_agg(t), '[]')
//                    FROM (
//                             SELECT p.name AS "productName", p.stock_quantity AS "stockQuantity"
//                             FROM products p
//                             ORDER BY updated_at DESC
//                             LIMIT 5
//                         ) t
//                ) AS products,
//                (
//                    SELECT COALESCE(json_agg(t), '[]')
//                    FROM (
//                             SELECT
//                                 r.review_score AS score,
//                                 ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM reviews), 2) AS "reviewScore"
//                             FROM reviews r
//                             GROUP BY r.review_score
//                             ORDER BY r.review_score
//                         ) t
//                ) AS reviews
//            FROM users
//            LIMIT 1;
//        """;
//        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
//            String weeklyJson = rs.getString("weeklyActiveUsersChart");
//            String latestOrderJson = rs.getString("latestOrder");
//            String channelRateJson = rs.getString("orderChannelRate");
//            String regionalJson = rs.getString("regionalRevenue");
//            String productsJson = rs.getString("products");
//            String reviewsJson = rs.getString("reviews");
//            
//            try {
//                ActiveUserChartDTO chart =
//                        objectMapper.readValue(
//                                weeklyJson,
//                                ActiveUserChartDTO.class
//                        );
//                
//                List<LatestOrderDTO> lastOrder = 
//                        objectMapper.readValue(
//                                latestOrderJson,
//                                new TypeReference<List<LatestOrderDTO>>() {}
//                        );
//
//                List<OrderChannelRateDTO> channelRate =
//                        objectMapper.readValue(
//                                channelRateJson,
//                                new TypeReference<List<OrderChannelRateDTO>>() {}
//                        );
//
//                List<RegionalRevenueDTO> regional =
//                        objectMapper.readValue(
//                                regionalJson,
//                                new TypeReference<List<RegionalRevenueDTO>>() {}
//                        );
//
//                List<ProductDashboardDTO> products =
//                        objectMapper.readValue(
//                                productsJson,
//                                new TypeReference<List<ProductDashboardDTO>>() {}
//                        );
//
//                List<ReviewDashboardDTO> reviews =
//                        objectMapper.readValue(
//                                reviewsJson,
//                                new TypeReference<List<ReviewDashboardDTO>>() {}
//                        );
//
//                return Optional.of(OwnerDashboardDTO.builder()
//                        .activeUsers(rs.getInt("activeUsers"))
//                        .newUsers(rs.getInt("newUsers"))
//                        .weeklyActiveUsersChart(chart)
//                        .latestOrder(lastOrder)
//                        .orderChannelRate(channelRate)
//                        .regionalRevenue(regional)
//                        .products(products)
//                        .reviews(reviews)
//                        .build());
//
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
//            }
//        });
//    }
    
    public Optional<SalesAnalyticsDashboardDTO> findSalesAnalyticsDashboard(String period) {
        String sql = """
                WITH date_filter AS (
                    SELECT CASE ?
                               WHEN 'TODAY' THEN DATE_TRUNC('day', CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Bangkok')
                               WHEN 'WEEK' THEN DATE_TRUNC('week', CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Bangkok')
                               WHEN 'MONTH' THEN DATE_TRUNC('month', CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Bangkok')
                               ELSE DATE_TRUNC('month', CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Bangkok')
                               END AS start_date,
                           CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Bangkok' AS end_date
                )
                SELECT
                    COALESCE(SUM(o.total_price), 0) AS "totalPrice",
                    COALESCE(COUNT(DISTINCT o.id), 0) AS "totalOrder",
                    (
                        SELECT COALESCE(json_agg(t), '[]')
                        FROM (
                                 SELECT
                                     o2.order_channel AS "orderChannel",
                                     ROUND(COUNT(o2.id) * 100.0 / NULLIF((SELECT COUNT(*) FROM orders WHERE status NOT IN ('PENDING', 'CANCELLED', 'REFUND')), 0), 2) AS "percentage"
                                 FROM orders o2
                                 WHERE o2.status IN ('COMPLETED')
                                   AND o2.created_at >= (SELECT start_date FROM date_filter)
                                   AND o2.created_at <= (SELECT end_date FROM date_filter)
                                 GROUP BY o2.order_channel
                                 ORDER BY "percentage" DESC
                             ) t
                    ) AS orderChannelIncome,
                    (
                        SELECT COALESCE(json_agg(t), '[]')
                        FROM (
                                 SELECT
                                     CASE WHEN p.name IN (
                                                          'กรุงเทพมหานคร',
                                                          'นนทบุรี',
                                                          'ปทุมธานี',
                                                          'สมุทรปราการ',
                                                          'นครปฐม',
                                                          'สมุทรสาคร'
                                         )
                                              THEN 'กรุงเทพและปริมณฑล'
                                          ELSE g.name
                                         END AS geography,
                                     COUNT(DISTINCT o.id) AS "totalOrder",
                                     COALESCE(SUM(DISTINCT o.total_price), 0) AS "totalRevenue",
                                     COUNT(DISTINCT oa) AS "totalUser"
                                 FROM orders o
                                          LEFT JOIN order_address oa ON o.id = oa.order_id
                                          LEFT JOIN order_items oi ON oi.order_id = o.id
                                          LEFT JOIN products pd ON pd.id = oi.product_id
                                          LEFT JOIN zipcode z ON z.id = oa.zipcode_id
                                          LEFT JOIN provinces p ON p.id = z.province_id
                                          LEFT JOIN geography g ON g.id = p.geo_id
                                 WHERE o.status IN ('COMPLETED')
                                   AND o.created_at >= (SELECT start_date FROM date_filter)
                                   AND o.created_at <= (SELECT end_date FROM date_filter)
                                 GROUP BY
                                     CASE
                                         WHEN p.name IN (
                                                         'กรุงเทพมหานคร',
                                                         'นนทบุรี',
                                                         'ปทุมธานี',
                                                         'สมุทรปราการ',
                                                         'นครปฐม',
                                                         'สมุทรสาคร'
                                             )
                                             THEN 'กรุงเทพและปริมณฑล'
                                         ELSE g.name
                                         END
                                 ORDER BY "totalRevenue" DESC
                             ) t
                    ) AS "regionalOrders",
                    (
                        SELECT COALESCE(json_agg(t), '[]')
                        FROM (
                                 SELECT
                                     CASE WHEN p.name IN (
                                                          'กรุงเทพมหานคร',
                                                          'นนทบุรี',
                                                          'ปทุมธานี',
                                                          'สมุทรปราการ',
                                                          'นครปฐม',
                                                          'สมุทรสาคร'
                                         )
                                              THEN 'กรุงเทพและปริมณฑล'
                                          ELSE g.name
                                         END AS geography,
                                     ROUND(
                                             COUNT(DISTINCT o.id) * 100.0 / (
                                                 SELECT COUNT(*)
                                                 FROM orders o2
                                                 WHERE o2.status IN ('COMPLETED')
                                                   AND o2.created_at >= (SELECT start_date FROM date_filter)
                                                   AND o2.created_at <= (SELECT end_date FROM date_filter)
                                             )
                                     ) AS "totalRevenuePercent"
                                 FROM orders o
                                          LEFT JOIN order_address oa ON o.id = oa.order_id
                                          LEFT JOIN order_items oi ON oi.order_id = o.id
                                          LEFT JOIN products pd ON pd.id = oi.product_id
                                          LEFT JOIN zipcode z ON z.id = oa.zipcode_id
                                          LEFT JOIN provinces p ON p.id = z.province_id
                                          LEFT JOIN geography g ON g.id = p.geo_id
                                 WHERE o.status IN ('COMPLETED')
                                   AND o.created_at >= (SELECT start_date FROM date_filter)
                                   AND o.created_at <= (SELECT end_date FROM date_filter)
                                 GROUP BY
                                     CASE
                                         WHEN p.name IN (
                                                         'กรุงเทพมหานคร',
                                                         'นนทบุรี',
                                                         'ปทุมธานี',
                                                         'สมุทรปราการ',
                                                         'นครปฐม',
                                                         'สมุทรสาคร'
                                             )
                                             THEN 'กรุงเทพและปริมณฑล'
                                         ELSE g.name
                                         END
                                 ORDER BY "totalRevenuePercent" DESC
                             ) t
                    ) AS "regionalRevenue",
                
                    (
                        SELECT COALESCE(json_agg(t), '[]')
                        FROM (
                                 SELECT
                                     CASE WHEN p.name IN (
                                                          'กรุงเทพมหานคร',
                                                          'นนทบุรี',
                                                          'ปทุมธานี',
                                                          'สมุทรปราการ',
                                                          'นครปฐม',
                                                          'สมุทรสาคร'
                                         )
                                              THEN 'กรุงเทพและปริมณฑล'
                                          ELSE g.name
                                         END AS geography,
                                     ROUND(
                                             COUNT(DISTINCT u.id) * 100.0 / (
                                                 SELECT COUNT(*) FROM users u2
                                                                          LEFT JOIN user_address ua2 ON ua2.user_id = u2.id
                                                 WHERE ua2.is_default = TRUE
                                             ),2
                                     ) AS "totalUserPercent"
                                 FROM users u
                                          LEFT JOIN user_address ua ON ua.user_id = u.id
                                          LEFT JOIN zipcode z ON z.id = ua.zipcode_id
                                          LEFT JOIN provinces p ON p.id = z.province_id
                                          LEFT JOIN geography g ON g.id = p.geo_id
                                 WHERE ua.is_default = TRUE
                                 GROUP BY
                                     CASE
                                         WHEN p.name IN (
                                                         'กรุงเทพมหานคร',
                                                         'นนทบุรี',
                                                         'ปทุมธานี',
                                                         'สมุทรปราการ',
                                                         'นครปฐม',
                                                         'สมุทรสาคร'
                                             )
                                             THEN 'กรุงเทพและปริมณฑล'
                                         ELSE g.name
                                         END
                                 ORDER BY "totalUserPercent" DESC
                             ) t
                    ) AS "regionalUsers"
                FROM orders o
                WHERE o.status IN ('COMPLETED')
                  AND o.created_at >= (SELECT start_date FROM date_filter)
                  AND o.created_at <= (SELECT end_date FROM date_filter);
        """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            BigDecimal totalPrice = rs.getBigDecimal("totalPrice");
            Long totalOrder = rs.getLong("totalOrder");
            String orderCannelJson = rs.getString("orderChannelIncome");
            String regionalOrderJson = rs.getString("regionalOrders");
            String regionalRevenueJson = rs.getString("regionalRevenue");
            String regionalUsersJson = rs.getString("regionalUsers");
            
            try {
                List<OrderChannelIncomeDTO> orderChannelIncome = 
                        objectMapper.readValue(
                                orderCannelJson, new TypeReference<List<OrderChannelIncomeDTO>>() {
                        });
                
                List<RegionalOrderAnalyticsDTO> regionalOrders = 
                        objectMapper.readValue(
                        regionalOrderJson,
                        new TypeReference<List<RegionalOrderAnalyticsDTO>>() {
                        });
                
                List<RegionalRevenueAnalyticsDTO> regionalRevenue =
                        objectMapper.readValue(
                                regionalRevenueJson,
                                new TypeReference<List<RegionalRevenueAnalyticsDTO>>() {
                                });
                
                List<RegionalUserAnalyticsDTO> regionalUsers = 
                        objectMapper.readValue(
                                regionalUsersJson,
                                new TypeReference<List<RegionalUserAnalyticsDTO>>() {
                                });
                
                return Optional.of(SalesAnalyticsDashboardDTO.builder()
                                .totalPrice(totalPrice)
                                .totalOrder(totalOrder)
                                .orderChannelIncome(orderChannelIncome)
                                .regionalOrders(regionalOrders)
                                .regionalRevenue(regionalRevenue)
                                .regionalUsers(regionalUsers)
                        .build()
                );
            }catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, period);
    }

    public Optional<OwnerDashboardDTO> findOwnerDashboard() {
        String sql = """
            WITH month AS (
                SELECT *
                FROM generate_series(
                             date_trunc('year', CURRENT_DATE),
                             date_trunc('year', CURRENT_DATE) + INTERVAL '11 months',
                             INTERVAL '1 month'
                     ) AS m(month)
            )
            SELECT
                COUNT(
                        CASE
                            WHEN last_seen_at >= NOW() - INTERVAL '5 minutes'
                                THEN 1
                            END
                ) AS "activeUsers",
                COUNT(
                        CASE
                            WHEN DATE_TRUNC('day', created_at) = DATE_TRUNC('day', NOW())
                                THEN 1
                            END
                ) AS "newUserToday",
                (
                        SELECT SUM(total_price) FROM orders WHERE status in ('COMPLETED')
                ) AS "totalRevenue",
                (
                    SELECT COUNT(id) FROM orders WHERE status in ('COMPLETED')
                ) AS "totalOrder",
                COUNT (
                        CASE
                            WHEN DATE_TRUNC('month', created_at) = DATE_TRUNC('month', NOW())
                                THEN 1
                            END
                ) AS "newUsers",
                (
                    SELECT COUNT(oi2.quantity) FROM orders o2
                    LEFT JOIN order_items oi2 ON oi2.order_id = o2.id
                    WHERE o2.status in ('COMPLETED')
                ) AS "totalProductSale",
            
                (
                    WITH
                        summary AS (
                            SELECT
                                SUM(CASE WHEN o2.created_at >= date_trunc('year', current_date) THEN o2.total_price ELSE 0 END) AS "thisYear",
                                SUM(CASE WHEN o2.created_at >= date_trunc('year', current_date) - INTERVAL '1 year' AND o2.created_at < date_trunc('year', current_date) THEN o2.total_price ELSE 0 END) AS "lastYear"
                            FROM orders o2
                        ),
            
                        totalIncome AS (
                             SELECT
                                 SUM(CASE WHEN o2.created_at >= date_trunc('year', current_date) THEN o2.total_price ELSE 0 END) AS "thisYear",
                                 SUM(CASE WHEN o2.created_at >= date_trunc('year', current_date) - INTERVAL '1 year' AND o2.created_at < date_trunc('year', current_date) THEN o2.total_price ELSE 0 END) AS "lastYear"
                             FROM orders o2
                        )
                    SELECT json_build_object(
                                   'growthRate',
                                   (
                                       SELECT
                                           CASE WHEN "lastYear" != 0 THEN (("thisYear" - "lastYear") / "lastYear") * 100 ELSE 100 END
                                       FROM summary
                                       LIMIT 1
                                   ),
            
                                   'thisYear',
                                   (
                                       SELECT json_build_object(
                                                      'year',
                                                      (
                                                          SELECT extract(YEAR FROM CURRENT_DATE) AS "thisYear"
                                                      ),
                                                      'totalIncome',
                                                      (
                                                          SELECT "thisYear" FROM totalIncome
                                                      ),
                                                      'graph',
                                                      (
                                                          SELECT COALESCE(json_agg(t ORDER BY t."monthNo"), '[]')
                                                          FROM (
                                                                   SELECT
                                                                       EXTRACT(MONTH FROM m.month)::INT AS "monthNo",
            
                                                                       CASE EXTRACT(MONTH FROM m.month)
                                                                           WHEN 1 THEN 'มกราคม'
                                                                           WHEN 2 THEN 'กุมภาพันธ์'
                                                                           WHEN 3 THEN 'มีนาคม'
                                                                           WHEN 4 THEN 'เมษายน'
                                                                           WHEN 5 THEN 'พฤษภาคม'
                                                                           WHEN 6 THEN 'มิถุนายน'
                                                                           WHEN 7 THEN 'กรกฎาคม'
                                                                           WHEN 8 THEN 'สิงหาคม'
                                                                           WHEN 9 THEN 'กันยายน'
                                                                           WHEN 10 THEN 'ตุลาคม'
                                                                           WHEN 11 THEN 'พฤศจิกายน'
                                                                           WHEN 12 THEN 'ธันวาคม'
                                                                           END AS "month",
            
                                                                       COALESCE(SUM(o2.total_price), 0) AS "totalMonthlyIncome"
            
                                                                   FROM month m
            
                                                                            LEFT JOIN orders o2
                                                                                      ON o2.created_at >= m.month
                                                                                          AND o2.created_at < m.month + INTERVAL '1 month'
                                                                                          AND o2.status = 'COMPLETED'
            
                                                                   GROUP BY m.month
            
                                                                   ORDER BY m.month
                                                               ) t
                                                      )
                                              )
                                   ),
                                   'lastYear',
                                   (
                                       SELECT json_build_object(
                                                      'year',
                                                      (
                                                          SELECT extract(YEAR FROM CURRENT_DATE  - INTERVAL '1 year') AS "lastYear"
                                                      ),
                                                      'totalIncome',
                                                      (
                                                          SELECT "lastYear" FROM totalIncome
                                                      ),
                                                      'graph',
                                                      (
                                                          SELECT COALESCE(json_agg(t ORDER BY t."monthNo"), '[]')
                                                          FROM (
                                                                   SELECT
                                                                       EXTRACT(MONTH FROM m.month)::INT AS "monthNo",
            
                                                                       CASE EXTRACT(MONTH FROM m.month)
                                                                           WHEN 1 THEN 'มกราคม'
                                                                           WHEN 2 THEN 'กุมภาพันธ์'
                                                                           WHEN 3 THEN 'มีนาคม'
                                                                           WHEN 4 THEN 'เมษายน'
                                                                           WHEN 5 THEN 'พฤษภาคม'
                                                                           WHEN 6 THEN 'มิถุนายน'
                                                                           WHEN 7 THEN 'กรกฎาคม'
                                                                           WHEN 8 THEN 'สิงหาคม'
                                                                           WHEN 9 THEN 'กันยายน'
                                                                           WHEN 10 THEN 'ตุลาคม'
                                                                           WHEN 11 THEN 'พฤศจิกายน'
                                                                           WHEN 12 THEN 'ธันวาคม'
                                                                           END AS "month",
            
                                                                       COALESCE(SUM(o2.total_price), 0) AS "totalMonthlyIncome"
            
                                                                   FROM month m
            
                                                                            LEFT JOIN orders o2
                                                                                      ON o2.created_at >=
                                                                                         date_trunc('year', CURRENT_DATE) - INTERVAL '1 year'
                                                                                             + (EXTRACT(MONTH FROM m.month)::INT - 1) * INTERVAL '1 month'
            
                                                                                          AND o2.created_at <
                                                                                              date_trunc('year', CURRENT_DATE) - INTERVAL '1 year'
                                                                                                  + EXTRACT(MONTH FROM m.month)::INT * INTERVAL '1 month'
            
                                                                                          AND o2.status = 'COMPLETED'
            
                                                                   GROUP BY m.month
            
                                                                   ORDER BY m.month
                                                               ) t
                                                      )
                                              )
                                   )
                           )
                ) AS "yearActiveIncomeChart",
            
                (
                  SELECT json_build_object(
                    'totalWeeklyIncome',
                    (
                        SELECT SUM(o2.total_price) FROM orders o2
                        WHERE o2.created_at >= DATE_TRUNC('week', CURRENT_DATE)
                        GROUP BY o2.created_at
                    ),
                    'graph',
                    (SELECT COALESCE(json_agg(t), '[]')
                     FROM (SELECT EXTRACT(ISODOW FROM o2.created_at) AS "dayOfWeek",
                                  CASE EXTRACT(ISODOW FROM o2.created_at)
                                      WHEN 1 THEN 'วันจันทร์'
                                      WHEN 2 THEN 'วันอังคาร'
                                      WHEN 3 THEN 'วันพุธ'
                                      WHEN 4 THEN 'วันพฤหัสบดี'
                                      WHEN 5 THEN 'วันศุกร์'
                                      WHEN 6 THEN 'วันเสาร์'
                                      WHEN 7 THEN 'วันอาทิตย์'
                                      END AS "date",
                                  SUM(o2.total_price) AS "totalSummary"
                           FROM orders o2
                           WHERE o2.created_at >= DATE_TRUNC('week', CURRENT_DATE)
                           GROUP BY o2.created_at
                           ORDER BY "dayOfWeek") t)
                  )
                ) AS weeklyActiveIncomeChart,
            
                (
                    SELECT COALESCE(json_agg(t), '[]') FROM (
                        SELECT
                            EXTRACT(MONTH FROM m.month)::INT AS "monthNo",
                            CASE EXTRACT(MONTH FROM m.month)
                                WHEN 1 THEN 'มกราคม'
                                WHEN 2 THEN 'กุมภาพันธ์'
                                WHEN 3 THEN 'มีนาคม'
                                WHEN 4 THEN 'เมษายน'
                                WHEN 5 THEN 'พฤษภาคม'
                                WHEN 6 THEN 'มิถุนายน'
                                WHEN 7 THEN 'กรกฎาคม'
                                WHEN 8 THEN 'สิงหาคม'
                                WHEN 9 THEN 'กันยายน'
                                WHEN 10 THEN 'ตุลาคม'
                                WHEN 11 THEN 'พฤศจิกายน'
                                WHEN 12 THEN 'ธันวาคม'
                                END AS "month",
                            COALESCE(COUNT(o2), 0) AS "total"
                        FROM month m
                         LEFT JOIN orders o2
                           ON o2.created_at >= m.month
                               AND o2.created_at < m.month + INTERVAL '1 month'
                               AND o2.status = 'COMPLETED'
                        GROUP BY m.month
                        ORDER BY m.month
                    ) t
                ) AS "yearActiveOrderChart",
            
                (
                    SELECT COALESCE(json_agg(t), '[]')
                    FROM (
                             SELECT
                                 t.order_channel AS "orderChannel",
                                 t.order_score AS "avg"
                             FROM (
                                      SELECT
                                          o.order_channel,
                                          ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM orders), 2) AS order_score
                                      FROM orders o
                                      GROUP BY o.order_channel
                                  ) t
                             GROUP BY t.order_channel, t.order_score
                             ORDER BY avg DESC
                         ) t
                ) AS "orderChannelRate",
            
                (
                    SELECT COALESCE(json_agg(t), '[]') FROM(
                        SELECT
                            p2.id,
                            p2.name,
                            ROUND((SUM(oi2.quantity) * 100.0) / (SELECT SUM(oi3.quantity) FROM orders o3 JOIN order_items oi3 ON oi3.order_id = o3.id AND o3.status = 'COMPLETED'), 2) AS "avg"
                        FROM orders o2
                                 JOIN order_items oi2 ON oi2.order_id = o2.id AND o2.status = 'COMPLETED'
                                 JOIN products p2 ON p2.id = oi2.product_id
                        GROUP BY p2.id, p2.name
                        ORDER BY "avg" DESC
                    ) t
                ) AS "salesPercentage",
            
                (
                    SELECT json_build_object(
                                   'oldUser',
                                   (SELECT COUNT(*) FROM(
                                                            SELECT
                                                                u2.id
                                                            FROM users u2
                                                                     LEFT JOIN orders o2 ON u2.id = o2.user_id
                                                            WHERE o2.created_at >= DATE_TRUNC('day', CURRENT_DATE - INTERVAL '30 day') AND o2.status = 'COMPLETED'
                                                            GROUP BY u2.id
                                                            HAVING COUNT(o2.user_id) > 2
                                                        ) AS "oldUser"),
            
                                   'newUser',
                                   (SELECT COUNT(*) FROM(
                                                            SELECT
                                                                u2.id
                                                            FROM users u2
                                                                     LEFT JOIN orders o2 ON u2.id = o2.user_id
                                                            WHERE o2.created_at >= DATE_TRUNC('day', CURRENT_DATE - INTERVAL '7 day') AND u2.created_at >= DATE_TRUNC('day', CURRENT_DATE - INTERVAL '7 day') AND o2.status = 'COMPLETED'
                                                            GROUP BY u2.id
                                                        ) AS "newUser"),
            
                                   'inactiveUser',
                                   (SELECT COUNT(*) FROM(
                                                            SELECT u.id FROM users u LEFT JOIN orders o ON o.user_id = u.id AND o.status = 'COMPLETED'
                                                            GROUP BY u.id
                                                            HAVING COUNT(o.id) = 0 OR (SELECT o2.created_at FROM orders o2 WHERE user_id = u.id  AND o2.status = 'COMPLETED' ORDER BY o2.created_at DESC LIMIT 1) <= date_trunc('day', CURRENT_DATE - INTERVAL '30 day')
                                                        ) AS "inactiveUser")
                           )
                ) AS "userChart",
            
                (
                    SELECT COALESCE(json_agg(t), '[]')
                    FROM (
                             SELECT
                                 CASE WHEN p.name IN (
                                                      'กรุงเทพมหานคร',
                                                      'นนทบุรี',
                                                      'ปทุมธานี',
                                                      'สมุทรปราการ',
                                                      'นครปฐม',
                                                      'สมุทรสาคร'
                                     )
                                          THEN 'กรุงเทพและปริมณฑล'
                                      ELSE g.name
                                     END AS geography,
                                 COUNT(o) AS "totalOrders"
                             FROM orders o
                                      LEFT JOIN order_address oa ON o.id = oa.order_id
                                      LEFT JOIN zipcode z ON z.id = oa.zipcode_id
                                      LEFT JOIN provinces p ON p.id = z.province_id
                                      LEFT JOIN geography g ON g.id = p.geo_id
                             GROUP BY CASE WHEN p.name IN (
                                                           'กรุงเทพมหานคร',
                                                           'นนทบุรี',
                                                           'ปทุมธานี',
                                                           'สมุทรปราการ',
                                                           'นครปฐม',
                                                           'สมุทรสาคร'
                                 )
                                               THEN 'กรุงเทพและปริมณฑล'
                                           ELSE g.name
                                          END
                             ORDER BY "totalOrders" DESC
                             LIMIT 4
                         ) t
                ) AS "regionalRevenue",
            
                (
                    SELECT COALESCE(json_agg(t), '[]')
                    FROM (
                             SELECT
                                 r.review_score AS score,
                                 ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM reviews), 2) AS "reviewScore"
                             FROM reviews r
                             GROUP BY r.review_score
                             ORDER BY r.review_score
                         ) t
                ) AS reviews,
                (
                    SELECT json_agg(t) FROM (
                        SELECT
                            p.id,
                            p.name,
                            pi.image_url AS "imageUrl",
                            p.stock_quantity AS "stockQuantity",
                            CASE
                                WHEN p.stock_quantity >= 50 THEN 'ปกติ'
                                WHEN p.stock_quantity > 35 THEN 'ต่ำ'
                                WHEN p.stock_quantity > 0 THEN 'ต่ำมาก'
                                WHEN p.stock_quantity = 0 THEN 'หมดสต็อก'
                                END AS status
                        FROM products p
                                 LEFT JOIN LATERAL (SELECT * FROM product_images WHERE product_id = p.id LIMIT 1) pi ON TRUE
                                 LEFT JOIN product_status ps ON ps.id = p.status_id
                        WHERE ps.status <> 'DELETED'
                        ORDER BY stock_quantity ASC
                    ) t
                ) AS "productAlert"
            FROM users
            LIMIT 1;
        """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            String yearActiveIncomeChartJson = rs.getString("yearActiveIncomeChart");
            String weeklyActiveIncomeChartJson = rs.getString("weeklyActiveIncomeChart");
            String yearActiveOrderChartJson = rs.getString("yearActiveOrderChart");
            String orderChannelRateJson = rs.getString("orderChannelRate");
            String salesPercentageJson = rs.getString("salesPercentage");
            String userChartJson = rs.getString("userChart");
            String regionalJson = rs.getString("regionalRevenue");
            String reviewsJson = rs.getString("reviews");
            String productAlertJson = rs.getString("productAlert");

            try {
                YearActiveIncomeChartDTO yearActiveIncome =
                        objectMapper.readValue(
                                yearActiveIncomeChartJson,
                                YearActiveIncomeChartDTO.class
                        );
                
                WeeklyActiveIncomeChartDTO weeklyActiveIncome =
                        objectMapper.readValue(
                                weeklyActiveIncomeChartJson,
                                WeeklyActiveIncomeChartDTO.class
                        );

                List< YearActiveOrderChartDTO> yearActiveOrder =
                        objectMapper.readValue(
                                yearActiveOrderChartJson,
                                new TypeReference<List<YearActiveOrderChartDTO>>() {}
                        );

                List<OrderChannelReteDTO> channelRate =
                        objectMapper.readValue(
                                orderChannelRateJson,
                                new TypeReference<List<OrderChannelReteDTO>>() {}
                        );

                List<SalesPercentageDTO> salesPercentage =
                        objectMapper.readValue(
                                salesPercentageJson,
                                new TypeReference<List<SalesPercentageDTO>>() {}
                        );
                
                UserChartDTO userChart =
                        objectMapper.readValue(
                                userChartJson,
                                UserChartDTO.class
                        );
                
                List<RegionalRevenueDTO> regional =
                        objectMapper.readValue(
                                regionalJson,
                                new TypeReference<List<RegionalRevenueDTO>>() {}
                        );


                List<ReviewDashboardDTO> reviews =
                        objectMapper.readValue(
                                reviewsJson,
                                new TypeReference<List<ReviewDashboardDTO>>() {}
                        );

                List<ProductAlertDTO> productAlert =
                        objectMapper.readValue(
                                productAlertJson,
                                new TypeReference<List<ProductAlertDTO>>() {}
                        );

                return Optional.of(OwnerDashboardDTO.builder()
                                .activeUsers(rs.getInt("activeUsers"))
                                .newUserToday(rs.getInt("newUserToday"))
                                .totalRevenue(rs.getInt("totalRevenue"))
                                .totalOrder(rs.getInt("totalOrder"))
                                .newUsers(rs.getInt("newUsers"))
                                .totalProductSale(rs.getInt("totalProductSale"))
                                .yearActiveIncomeChart(yearActiveIncome)
                                .weeklyActiveIncomeChart(weeklyActiveIncome)
                                .yearActiveOrderChart(yearActiveOrder)
                                .orderChannelRete(channelRate)
                                .salesPercentage(salesPercentage)
                                .userChart(userChart)
                                .regionalRevenue(regional)
                                .reviews(reviews)
                                .productAlert(productAlert)
                        .build());

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
