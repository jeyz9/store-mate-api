package com.sm.jeyz9.storemateapi.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sm.jeyz9.storemateapi.dto.ActiveUserChartDTO;
import com.sm.jeyz9.storemateapi.dto.OwnerDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.LatestOrderDTO;
import com.sm.jeyz9.storemateapi.dto.OrderChannelIncomeDTO;
import com.sm.jeyz9.storemateapi.dto.OrderChannelRateDTO;
import com.sm.jeyz9.storemateapi.dto.ProductDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.RegionalOrderAnalyticsDTO;
import com.sm.jeyz9.storemateapi.dto.RegionalRevenueAnalyticsDTO;
import com.sm.jeyz9.storemateapi.dto.RegionalRevenueDTO;
import com.sm.jeyz9.storemateapi.dto.RegionalUserAnalyticsDTO;
import com.sm.jeyz9.storemateapi.dto.ReviewDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.SalesAnalyticsDashboardDTO;
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
    
    public Optional<OwnerDashboardDTO> findAdminDashboard() {
        String sql = """
            SELECT
                COUNT(
                        CASE
                            WHEN last_seen_at >= NOW() - INTERVAL '5 minutes'
                                THEN 1
                            END
                ) AS "activeUsers",
                COUNT(
                        CASE
                            WHEN DATE_TRUNC('month', created_at) = DATE_TRUNC('month', NOW())
                                THEN 1
                            END
                ) AS "newUsers",
                    (
                          SELECT json_build_object(
                                         'thisWeek',
                                         (SELECT COALESCE(json_agg(t), '[]')
                                          FROM (SELECT EXTRACT(ISODOW FROM ual.activity_date) AS "dayOfWeek",
                                                       CASE EXTRACT(ISODOW FROM ual.activity_date)
                                                           WHEN 1 THEN 'วันจันทร์'
                                                           WHEN 2 THEN 'วันอังคาร'
                                                           WHEN 3 THEN 'วันพุธ'
                                                           WHEN 4 THEN 'วันพฤหัสบดี'
                                                           WHEN 5 THEN 'วันศุกร์'
                                                           WHEN 6 THEN 'วันเสาร์'
                                                           WHEN 7 THEN 'วันอาทิตย์'
                                                           END                                AS "activityDate",
                                                       COUNT(DISTINCT ual.user_id)            AS "totalUsers"
                                                FROM user_activity_logs ual
                                                WHERE ual.activity_date >= DATE_TRUNC('week', CURRENT_DATE)
                                                GROUP BY ual.activity_date
                                                ORDER BY "dayOfWeek") t),
                                         'lastWeek',
                                         (SELECT COALESCE(json_agg(t), '[]')
                                          FROM (SELECT EXTRACT(ISODOW FROM ual.activity_date) AS "dayOfWeek",
                                                       CASE EXTRACT(ISODOW FROM ual.activity_date)
                                                           WHEN 1 THEN 'วันจันทร์'
                                                           WHEN 2 THEN 'วันอังคาร'
                                                           WHEN 3 THEN 'วันพุธ'
                                                           WHEN 4 THEN 'วันพฤหัสบดี'
                                                           WHEN 5 THEN 'วันศุกร์'
                                                           WHEN 6 THEN 'วันเสาร์'
                                                           WHEN 7 THEN 'วันอาทิตย์'
                                                           END                                AS "activityDate",
                                                       COUNT(DISTINCT ual.user_id)            AS "totalUsers"
                                                FROM user_activity_logs ual
                                                WHERE ual.activity_date >= DATE_TRUNC('week', CURRENT_DATE) - INTERVAL '7 days'
                                                  AND ual.activity_date < DATE_TRUNC('week', CURRENT_DATE)
                                                GROUP BY ual.activity_date
                                                ORDER BY "dayOfWeek") t)
                          )
                      ) AS "weeklyActiveUsersChart",
                (
                    SELECT COALESCE(json_agg(t), '[]')
                    FROM (
                             SELECT
                                 o.order_no AS "orderNo",
                                 oa.recipient_name AS name,
                                 o.status
                             FROM orders o
                                      LEFT JOIN users u ON o.user_id = u.id
                                      LEFT JOIN order_address oa ON oa.order_id = o.id
                             ORDER BY o.created_at DESC
                             LIMIT 6
                         ) t
                ) AS "latestOrder",
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
                             LIMIT 4
                         ) t
                ) AS "regionalRevenue",
                (
                    SELECT COALESCE(json_agg(t), '[]')
                    FROM (
                             SELECT p.name AS "productName", p.stock_quantity AS "stockQuantity"
                             FROM products p
                             ORDER BY updated_at DESC
                             LIMIT 5
                         ) t
                ) AS products,
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
                ) AS reviews
            FROM users
            LIMIT 1;
        """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            String weeklyJson = rs.getString("weeklyActiveUsersChart");
            String latestOrderJson = rs.getString("latestOrder");
            String channelRateJson = rs.getString("orderChannelRate");
            String regionalJson = rs.getString("regionalRevenue");
            String productsJson = rs.getString("products");
            String reviewsJson = rs.getString("reviews");
            
            try {
                ActiveUserChartDTO chart =
                        objectMapper.readValue(
                                weeklyJson,
                                ActiveUserChartDTO.class
                        );
                
                List<LatestOrderDTO> lastOrder = 
                        objectMapper.readValue(
                                latestOrderJson,
                                new TypeReference<List<LatestOrderDTO>>() {}
                        );

                List<OrderChannelRateDTO> channelRate =
                        objectMapper.readValue(
                                channelRateJson,
                                new TypeReference<List<OrderChannelRateDTO>>() {}
                        );

                List<RegionalRevenueDTO> regional =
                        objectMapper.readValue(
                                regionalJson,
                                new TypeReference<List<RegionalRevenueDTO>>() {}
                        );

                List<ProductDashboardDTO> products =
                        objectMapper.readValue(
                                productsJson,
                                new TypeReference<List<ProductDashboardDTO>>() {}
                        );

                List<ReviewDashboardDTO> reviews =
                        objectMapper.readValue(
                                reviewsJson,
                                new TypeReference<List<ReviewDashboardDTO>>() {}
                        );

                return Optional.of(OwnerDashboardDTO.builder()
                        .activeUsers(rs.getInt("activeUsers"))
                        .newUsers(rs.getInt("newUsers"))
                        .weeklyActiveUsersChart(chart)
                        .latestOrder(lastOrder)
                        .orderChannelRate(channelRate)
                        .regionalRevenue(regional)
                        .products(products)
                        .reviews(reviews)
                        .build());

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    public Optional<SalesAnalyticsDashboardDTO> findSalesAnalyticsDashboard(String period) {
        String sql = """
                SELECT
                    COALESCE(SUM(o.totalPrice), 0) AS "totalPrice",
                    COALESCE(COUNT(DISTINCT o.id), 0) AS "totalOrder",
                    (
                        SELECT COALESCE(json_agg(t), '[]')
                        FROM (
                             SELECT
                                 o2.order_channel AS "orderChannel",
                                 ROUND(COUNT(o2.id) * 100.0 / (SELECT COUNT(*) FROM orders WHERE status NOT IN ('PENDING', 'CANCELLED', 'REFUND')), 2) AS "percentage"
                             FROM orders o2
                             WHERE o2.status NOT IN ('PENDING', 'CANCELLED', 'REFUND')
                             GROUP BY o2.order_channel
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
                                     COALESCE(SUM(o.totalPrice), 0) AS "totalRevenue",
                                     COUNT(DISTINCT o.user_id) AS "totalUser"
                                 FROM orders o
                                          LEFT JOIN order_address oa ON o.id = oa.order_id
                                          LEFT JOIN order_items oi ON oi.order_id = o.id
                                          LEFT JOIN products pd ON pd.id = oi.product_id
                                          LEFT JOIN zipcode z ON z.id = oa.zipcode_id
                                          LEFT JOIN provinces p ON p.id = z.province_id
                                          LEFT JOIN geography g ON g.id = p.geo_id
                                 WHERE o.status NOT IN ('PENDING', 'CANCELLED', 'REFUND')
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
                                             (
                                                 COALESCE(SUM(o.total_price), 0) * 100.0 /
                                                 (
                                                     SELECT COALESCE(SUM(o2.total_price), 0)
                                                     FROM orders o2
                                                     WHERE o2.status NOT IN ('PENDING', 'CANCELLED', 'REFUND')
                                                 )
                                                 )::numeric, 2
                                     ) AS "totalRevenuePercent"
                                 FROM orders o
                                          LEFT JOIN order_address oa ON o.id = oa.order_id
                                          LEFT JOIN order_items oi ON oi.order_id = o.id
                                          LEFT JOIN products pd ON pd.id = oi.product_id
                                          LEFT JOIN zipcode z ON z.id = oa.zipcode_id
                                          LEFT JOIN provinces p ON p.id = z.province_id
                                          LEFT JOIN geography g ON g.id = p.geo_id
                                 WHERE o.status NOT IN ('PENDING', 'CANCELLED', 'REFUND')
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
                     ) t
                    ) AS "regionalUsers"
                FROM orders o
                WHERE o.status NOT IN ('PENDING', 'CANCELLED', 'REFUND');        
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
        });
    }
}
