package org.linlinjava.litemall.core.wx;

import cn.binarywang.wx.miniapp.api.WxMaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.config.WxProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 微信小程序发货信息管理服务
 * 官方文档: https://developers.weixin.qq.com/miniprogram/dev/platform-capabilities/business-capabilities/order-shipping/order-shipping.html
 */
@Service
public class WxShippingService {

    private static final Log logger = LogFactory.getLog(WxShippingService.class);

    private static final String UPLOAD_SHIPPING_INFO_URL = "https://api.weixin.qq.com/wxa/sec/order/upload_shipping_info";
    private static final String IS_TRADE_MANAGED_URL = "https://api.weixin.qq.com/wxa/sec/order/is_trade_managed";
    private static final String GET_ORDER_URL = "https://api.weixin.qq.com/wxa/sec/order/get_order";
    private static final String GET_DELIVERY_LIST_URL = "https://api.weixin.qq.com/wxa/sec/order/get_delivery_list";
    private static final String NOTIFY_CONFIRM_RECEIVE_URL = "https://api.weixin.qq.com/wxa/sec/order/notify_confirm_receive";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 常用物流编码映射 (将内部或快递鸟编码映射到微信小程序官方运力编码)
    private static final Map<String, String> EXPRESS_MAP = new HashMap<>();

    static {
        EXPRESS_MAP.put("SF", "SF"); // 顺丰
        EXPRESS_MAP.put("SHUNFENG", "SF");
        EXPRESS_MAP.put("ZTO", "ZTO"); // 中通
        EXPRESS_MAP.put("ZHONGTONG", "ZTO");
        EXPRESS_MAP.put("YTO", "YTO"); // 圆通
        EXPRESS_MAP.put("YUANTONG", "YTO");
        EXPRESS_MAP.put("YD", "YD"); // 韵达
        EXPRESS_MAP.put("YUNDA", "YD");
        EXPRESS_MAP.put("STO", "STO"); // 申通
        EXPRESS_MAP.put("SHENTONG", "STO");
        EXPRESS_MAP.put("JTSD", "JTSD"); // 极兔
        EXPRESS_MAP.put("JITU", "JTSD");
        EXPRESS_MAP.put("EMS", "EMS"); // EMS
        EXPRESS_MAP.put("YZPY", "YZPY"); // 邮政快递包裹
        EXPRESS_MAP.put("DBL", "DBL"); // 德邦
        EXPRESS_MAP.put("DEBANG", "DBL");
        EXPRESS_MAP.put("JD", "JD"); // 京东
        EXPRESS_MAP.put("JINGDONG", "JD");
        EXPRESS_MAP.put("DANNIAO", "DANNIAO"); // 丹鸟
    }

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private WxProperties wxProperties;

    /**
     * 发货信息录入
     *
     * @param orderSn        商户订单号
     * @param payId          微信支付交易号 (transaction_id)
     * @param openid         买家 openid
     * @param logisticsType  物流模式: 1:实体物流配送(快递), 2:同城配送, 3:虚拟商品, 4:用户自提
     * @param shipChannel    物流公司 (如 SF, ZTO 等)
     * @param shipSn         物流单号
     * @param itemDesc       商品信息描述 (不超过120字)
     * @param receiverPhone  收件人手机号 (可选)
     * @return 接口调用结果 Map, 包含 success(boolean), errcode(int), errmsg(String), rawResponse(String)
     */
    public Map<String, Object> uploadShippingInfo(String orderSn,
                                                 String payId,
                                                 String openid,
                                                 Integer logisticsType,
                                                 String shipChannel,
                                                 String shipSn,
                                                 String itemDesc,
                                                 String receiverPhone) {
        Map<String, Object> result = new HashMap<>();

        if (!StringUtils.hasText(orderSn)) {
            result.put("success", false);
            result.put("errcode", -1);
            result.put("errmsg", "订单号 orderSn 不能为空");
            return result;
        }

        // 默认物流模式判断: 默认优先为同城配送(2)
        if (logisticsType == null || logisticsType < 1 || logisticsType > 4) {
            logisticsType = 2; // 默认同城配送
        }

        try {
            Map<String, Object> payload = new HashMap<>();

            // 1. 构建 order_key
            Map<String, Object> orderKey = new HashMap<>();
            if (StringUtils.hasText(payId) && !"0".equals(payId)) {
                orderKey.put("order_number_type", 2);
                orderKey.put("transaction_id", payId.trim());
            } else {
                orderKey.put("order_number_type", 1);
                orderKey.put("mchid", wxProperties.getMchId());
                orderKey.put("out_trade_no", orderSn);
            }
            payload.put("order_key", orderKey);

            // 2. logistics_type & delivery_mode
            payload.put("logistics_type", logisticsType);
            payload.put("delivery_mode", 1); // 1: 统一发货
            payload.put("is_all_delivered", true);

            // 3. shipping_list
            List<Map<String, Object>> shippingList = new ArrayList<>();
            Map<String, Object> shippingItem = new HashMap<>();

            // 商品描述截取120字符
            if (!StringUtils.hasText(itemDesc)) {
                itemDesc = "小满食光商品";
            } else if (itemDesc.length() > 120) {
                itemDesc = itemDesc.substring(0, 117) + "...";
            }
            shippingItem.put("item_desc", itemDesc);

            // 实体快递需要快递单号和快递公司编码
            if (logisticsType == 1) {
                shippingItem.put("tracking_no", StringUtils.hasText(shipSn) ? shipSn.trim() : "");
                String expressCompany = convertExpressCompany(shipChannel);
                shippingItem.put("express_company", expressCompany);
            }

            if (StringUtils.hasText(receiverPhone)) {
                Map<String, String> contact = new HashMap<>();
                contact.put("receiver_contact", receiverPhone.trim());
                shippingItem.put("contact", contact);
            }

            shippingList.add(shippingItem);
            payload.put("shipping_list", shippingList);

            // 4. upload_time 遵循 RFC 3339 规范 (例如 2026-08-18T16:20:00.123+08:00)
            String uploadTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
            payload.put("upload_time", uploadTime);

            // 5. payer
            if (StringUtils.hasText(openid)) {
                Map<String, String> payer = new HashMap<>();
                payer.put("openid", openid.trim());
                payload.put("payer", payer);
            }

            String requestJson = objectMapper.writeValueAsString(payload);
            logger.info("微信发货信息录入请求: " + requestJson);

            String responseString = wxMaService.post(UPLOAD_SHIPPING_INFO_URL, requestJson);
            logger.info("微信发货信息录入响应: " + responseString);

            JsonNode respNode = objectMapper.readTree(responseString);
            int errcode = respNode.has("errcode") ? respNode.get("errcode").asInt() : -1;
            String errmsg = respNode.has("errmsg") ? respNode.get("errmsg").asText() : "";

            result.put("rawResponse", responseString);
            result.put("errcode", errcode);
            result.put("errmsg", errmsg);
            result.put("success", errcode == 0);

            if (errcode != 0) {
                logger.warn("微信发货信息录入失败 orderSn=" + orderSn + ", errcode=" + errcode + ", errmsg=" + errmsg);
            }
        } catch (Exception e) {
            logger.error("微信发货信息录入异常: " + e.getMessage(), e);
            result.put("success", false);
            result.put("errcode", -999);
            result.put("errmsg", "微信发货调用异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 查询小程序是否已开通/受发货信息管理监管
     */
    public Map<String, Object> isTradeManaged() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("appid", wxProperties.getAppId());
            String requestJson = objectMapper.writeValueAsString(payload);
            String responseString = wxMaService.post(IS_TRADE_MANAGED_URL, requestJson);
            logger.info("微信发货管理开通状态查询响应: " + responseString);

            JsonNode respNode = objectMapper.readTree(responseString);
            int errcode = respNode.has("errcode") ? respNode.get("errcode").asInt() : -1;
            boolean isTradeManaged = respNode.has("is_trade_managed") && respNode.get("is_trade_managed").asBoolean();

            result.put("success", errcode == 0);
            result.put("errcode", errcode);
            result.put("errmsg", respNode.has("errmsg") ? respNode.get("errmsg").asText() : "");
            result.put("isTradeManaged", isTradeManaged);
        } catch (Exception e) {
            logger.error("查询发货管理开通状态异常: " + e.getMessage(), e);
            result.put("success", false);
            result.put("errcode", -999);
            result.put("errmsg", e.getMessage());
            result.put("isTradeManaged", false);
        }
        return result;
    }

    /**
     * 查询微信侧订单发货与收货状态
     *
     * @param transactionId 微信支付单号
     * @param orderSn       商户订单号
     */
    public Map<String, Object> getOrderShippingInfo(String transactionId, String orderSn) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> payload = new HashMap<>();
            if (StringUtils.hasText(transactionId) && !"0".equals(transactionId)) {
                payload.put("transaction_id", transactionId.trim());
            } else if (StringUtils.hasText(orderSn)) {
                payload.put("merchant_id", wxProperties.getMchId());
                payload.put("merchant_trade_no", orderSn.trim());
            } else {
                result.put("success", false);
                result.put("errcode", -1);
                result.put("errmsg", "transactionId 与 orderSn 不能同时为空");
                return result;
            }

            String requestJson = objectMapper.writeValueAsString(payload);
            String responseString = wxMaService.post(GET_ORDER_URL, requestJson);
            logger.info("查询微信发货状态响应: " + responseString);

            JsonNode respNode = objectMapper.readTree(responseString);
            int errcode = respNode.has("errcode") ? respNode.get("errcode").asInt() : -1;

            result.put("success", errcode == 0);
            result.put("errcode", errcode);
            result.put("errmsg", respNode.has("errmsg") ? respNode.get("errmsg").asText() : "");
            if (respNode.has("order")) {
                result.put("order", objectMapper.convertValue(respNode.get("order"), Map.class));
            }
        } catch (Exception e) {
            logger.error("查询微信发货状态异常: " + e.getMessage(), e);
            result.put("success", false);
            result.put("errcode", -999);
            result.put("errmsg", e.getMessage());
        }
        return result;
    }

    /**
     * 获取微信官方支持的运力公司列表
     */
    public Map<String, Object> getDeliveryList() {
        Map<String, Object> result = new HashMap<>();
        try {
            String responseString = wxMaService.post(GET_DELIVERY_LIST_URL, "{}");
            JsonNode respNode = objectMapper.readTree(responseString);
            int errcode = respNode.has("errcode") ? respNode.get("errcode").asInt() : -1;

            result.put("success", errcode == 0);
            result.put("errcode", errcode);
            result.put("errmsg", respNode.has("errmsg") ? respNode.get("errmsg").asText() : "");
            if (respNode.has("delivery_list")) {
                result.put("deliveryList", objectMapper.convertValue(respNode.get("delivery_list"), List.class));
            }
        } catch (Exception e) {
            logger.error("获取微信运力公司列表异常: " + e.getMessage(), e);
            result.put("success", false);
            result.put("errcode", -999);
            result.put("errmsg", e.getMessage());
        }
        return result;
    }

    /**
     * 转换运力公司编码
     */
    public String convertExpressCompany(String shipChannel) {
        if (!StringUtils.hasText(shipChannel)) {
            return "SF"; // 默认顺丰
        }
        String upper = shipChannel.trim().toUpperCase();
        if (EXPRESS_MAP.containsKey(upper)) {
            return EXPRESS_MAP.get(upper);
        }
        return upper;
    }
}
