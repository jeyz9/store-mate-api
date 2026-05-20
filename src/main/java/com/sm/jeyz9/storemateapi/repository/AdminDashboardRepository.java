package com.sm.jeyz9.storemateapi.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sm.jeyz9.storemateapi.dto.ActiveUserChartDTO;
import com.sm.jeyz9.storemateapi.dto.AdminDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.LatestOrderDTO;
import com.sm.jeyz9.storemateapi.dto.OrderChannelRateDTO;
import com.sm.jeyz9.storemateapi.dto.ProductDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.RegionalRevenueDTO;
import com.sm.jeyz9.storemateapi.dto.ReviewDashboardDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AdminDashboardRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public AdminDashboardRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }
    
    public Optional<AdminDashboardDTO> findAdminDashboard() {
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
                    SELECT COALESCE(json_agg(t), '[]')
                    FROM (
                        SELECT
                            activity_date AS "activityDate",
                            COUNT(DISTINCT  user_id) AS "totalUsers"
                        FROM user_activity_logs
                        WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
                        GROUP BY activity_date
                        ORDER BY activity_date
                    ) t
                ) AS "weeklyActiveUsersChart",
                (
                    SELECT COALESCE(json_agg(t), '[]')
                    FROM (
                        SELECT
                           o.order_no AS "orderNo",
                           u.name,
                           o.status
                        FROM orders o
                        LEFT JOIN users u ON o.user_id = u.id
                        ORDER BY o.created_at DESC
                        LIMIT 6
                    ) t
                ) AS "latestOrder",
                (
                    SELECT COALESCE(json_agg(t), '[]')
                    FROM (
                     SELECT
                         t.order_channel AS "orderChannel",
                         AVG(order_score)
                     FROM (
                              SELECT
                                  o.order_channel,
                                  ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM orders), 2) AS order_score
                              FROM orders o
                              GROUP BY o.order_channel
                          ) t
                     GROUP BY t.order_channel
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
                List<ActiveUserChartDTO> chart =
                        objectMapper.readValue(
                                weeklyJson,
                                new TypeReference<List<ActiveUserChartDTO>>() {}
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

                return Optional.of(AdminDashboardDTO.builder()
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
}
