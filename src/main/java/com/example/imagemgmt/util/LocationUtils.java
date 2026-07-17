package com.example.imagemgmt.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public class LocationUtils {

    // 使用 OpenStreetMap Nominatim API (免费，无需 Key，但在生产环境中建议使用高德/百度地图 API)
    private static final String NOMINATIM_API_URL = "https://nominatim.openstreetmap.org/reverse?format=json&lat={lat}&lon={lon}&zoom=18&addressdetails=1&accept-language=zh-CN";

    /**
     * 根据经纬度获取中文地址
     * @param lat 纬度
     * @param lon 经度
     * @return 中文地址字符串（省市区），如果获取失败则返回经纬度字符串
     */
    public static String getAddress(double lat, double lon) {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(3000); // 3秒连接超时
            factory.setReadTimeout(3000);    // 3秒读取超时
            RestTemplate restTemplate = new RestTemplate(factory);

            HttpHeaders headers = new HttpHeaders();
            // Nominatim 要求必须设置 User-Agent
            headers.set("User-Agent", "ImageManagementApp/1.0"); 

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    NOMINATIM_API_URL,
                    HttpMethod.GET,
                    entity,
                    String.class,
                    lat,
                    lon
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode address = root.path("address");
                
                // 提取地址信息
                String state = address.path("state").asText(""); // 省/州
                String province = address.path("province").asText(""); // 省
                String city = address.path("city").asText(""); // 市
                String district = address.path("district").asText(""); // 区
                String county = address.path("county").asText(""); // 县/区
                String town = address.path("town").asText(""); // 镇
                String village = address.path("village").asText(""); // 村
                
                // 优先使用 state 或 province
                String finalProvince = !state.isEmpty() ? state : province;
                
                // 拼接地址
                StringBuilder sb = new StringBuilder();
                
                // 添加省
                if (!finalProvince.isEmpty()) {
                    sb.append(finalProvince);
                }
                
                // 添加市 (如果市名与省名不同)
                if (!city.isEmpty() && !city.equals(finalProvince)) {
                    sb.append(city);
                }
                
                // 添加区/县
                if (!district.isEmpty()) {
                    sb.append(district);
                } else if (!county.isEmpty()) {
                    sb.append(county);
                }
                
                // 如果还没有区县信息，尝试更细的级别
                if (sb.length() == (finalProvince.length() + (city.equals(finalProvince) ? 0 : city.length()))) {
                     if (!town.isEmpty()) sb.append(town);
                     else if (!village.isEmpty()) sb.append(village);
                }
                
                // 如果拼接结果为空，或者只有省，尝试使用 display_name 的一部分或返回经纬度
                if (sb.length() == 0) {
                     // 简单的回退策略
                     return String.format("%.4f, %.4f", lat, lon);
                }

                return sb.toString();
            }
        } catch (Exception e) {
            // 记录日志，但在工具类中通常直接打印或忽略
            System.err.println("获取地理位置失败: " + e.getMessage());
        }
        // 失败时返回经纬度
        return String.format("%.4f, %.4f", lat, lon); 
    }
}
