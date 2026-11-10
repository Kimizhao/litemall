package org.linlinjava.litemall.wx.web.admin;

import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.service.WxPayService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.notify.NotifyService;
import org.linlinjava.litemall.core.notify.NotifyType;
import org.linlinjava.litemall.core.printer.LitemallPrinterClient;
import org.linlinjava.litemall.core.wx.WxShippingService;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.domain.*;
import org.linlinjava.litemall.db.service.*;
import org.linlinjava.litemall.db.util.CouponUserConstant;
import org.linlinjava.litemall.db.util.OrderUtil;
import org.linlinjava.litemall.wx.annotation.LoginUser;
import org.linlinjava.litemall.wx.service.WxAdminAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/wx/admin/order")
@Validated
public class WxAdminOrderController {
    private final Log logger = LogFactory.getLog(WxAdminOrderController.class);

    @Autowired
    private WxAdminAuthService adminAuthService;

    @Autowired
    private LitemallOrderService orderService;

    @Autowired
    private LitemallOrderGoodsService orderGoodsService;

    @Autowired
    private LitemallGoodsProductService productService;

    @Autowired
    private LitemallUserService userService;

    @Autowired
    private LitemallCouponUserService couponUserService;

    @Autowired
    private LitemallPrinterClient printerClient;

    @Autowired
    private WxShippingService wxShippingService;

    @Autowired
    private WxPayService wxPayService;

    @Autowired
    private NotifyService notifyService;

    /**
     * 订单列表查询 (支持状态筛选与待办数统计)
     */
    @GetMapping("/list")
    public Object list(@LoginUser Integer userId,
                       @RequestParam(required = false) String showType,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        int type = 0;
        if (!StringUtils.isEmpty(showType) && !"null".equalsIgnoreCase(showType) && !"undefined".equalsIgnoreCase(showType)) {
            try {
                type = Integer.parseInt(showType);
            } catch (NumberFormatException ignored) {}
        }

        List<Short> orderStatus = null;
        if (type == 1) { // 待制作
            orderStatus = Collections.singletonList(OrderUtil.STATUS_PAY);
        } else if (type == 2) { // 待取餐 / 配送中
            orderStatus = Collections.singletonList(OrderUtil.STATUS_SHIP);
        } else if (type == 3) { // 已完成
            orderStatus = Arrays.asList(OrderUtil.STATUS_CONFIRM, OrderUtil.STATUS_AUTO_CONFIRM);
        } else if (type == 4) { // 待退款
            orderStatus = Collections.singletonList(OrderUtil.STATUS_REFUND);
        }

        List<LitemallOrder> orderList = orderService.querySelective(null, keyword, null, null, orderStatus, page, limit, "add_time", "desc");
        List<Map<String, Object>> orderVoList = new ArrayList<>(orderList.size());

        for (LitemallOrder order : orderList) {
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", order.getId());
            vo.put("orderSn", order.getOrderSn());
            vo.put("orderStatus", order.getOrderStatus());
            vo.put("orderStatusText", OrderUtil.orderStatusText(order));
            vo.put("actualPrice", order.getActualPrice());
            vo.put("goodsPrice", order.getGoodsPrice());
            vo.put("message", order.getMessage());
            vo.put("consignee", order.getConsignee());
            vo.put("mobile", order.getMobile());
            vo.put("address", order.getAddress());
            vo.put("addTime", order.getAddTime());
            vo.put("payTime", order.getPayTime());
            vo.put("shipTime", order.getShipTime());
            vo.put("confirmTime", order.getConfirmTime());

            // 提货码展示 (通常取订单号后6位)
            String pickupCode = order.getOrderSn().length() >= 6 ? order.getOrderSn().substring(order.getOrderSn().length() - 6) : order.getOrderSn();
            vo.put("pickupCode", pickupCode);

            List<LitemallOrderGoods> goodsList = orderGoodsService.queryByOid(order.getId());
            vo.put("goodsList", goodsList);
            orderVoList.add(vo);
        }

        // 统计实时待办订单数
        int pendingMakingCount = orderService.countByStatus(Collections.singletonList(OrderUtil.STATUS_PAY));
        int pendingPickupCount = orderService.countByStatus(Collections.singletonList(OrderUtil.STATUS_SHIP));
        int pendingRefundCount = orderService.countByStatus(Collections.singletonList(OrderUtil.STATUS_REFUND));

        Map<String, Object> data = new HashMap<>();
        data.put("list", orderVoList);
        data.put("pendingMakingCount", pendingMakingCount);
        data.put("pendingPickupCount", pendingPickupCount);
        data.put("pendingRefundCount", pendingRefundCount);
        return ResponseUtil.ok(data);
    }

    /**
     * 订单详情
     */
    @GetMapping("/detail")
    public Object detail(@LoginUser Integer userId, @RequestParam Integer orderId) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgumentValue();
        }

        List<LitemallOrderGoods> goodsList = orderGoodsService.queryByOid(orderId);
        LitemallUser user = userService.findById(order.getUserId());

        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("orderGoods", goodsList);
        data.put("orderStatusText", OrderUtil.orderStatusText(order));
        data.put("user", user);

        String pickupCode = order.getOrderSn().length() >= 6 ? order.getOrderSn().substring(order.getOrderSn().length() - 6) : order.getOrderSn();
        data.put("pickupCode", pickupCode);

        return ResponseUtil.ok(data);
    }

    /**
     * 完成制作 / 通知自提 / 发货
     */
    @PostMapping("/ready")
    public Object ready(@LoginUser Integer userId, @RequestBody String body) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        Integer orderId = JacksonUtil.parseInteger(body, "orderId");
        if (orderId == null) {
            return ResponseUtil.badArgument();
        }

        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgumentValue();
        }

        if (!order.getOrderStatus().equals(OrderUtil.STATUS_PAY)) {
            return ResponseUtil.fail(602, "当前订单不是待出餐状态");
        }

        order.setOrderStatus(OrderUtil.STATUS_SHIP);
        order.setShipChannel("到店自提");
        order.setShipSn(order.getOrderSn());
        order.setShipTime(LocalDateTime.now());
        if (orderService.updateWithOptimisticLocker(order) == 0) {
            return ResponseUtil.updatedDateExpired();
        }

        // 联动微信发货信息同步
        try {
            LitemallUser user = userService.findById(order.getUserId());
            String openid = (user != null) ? user.getWeixinOpenid() : null;
            wxShippingService.uploadShippingInfo(
                    order.getOrderSn(),
                    order.getPayId(),
                    openid,
                    4, // 用户自提
                    "到店自提",
                    order.getOrderSn(),
                    "小满食光手作餐品已出餐，请凭提货码取餐",
                    order.getMobile() != null ? order.getMobile() : ""
            );
        } catch (Exception e) {
            logger.warn("出餐微信发货信息同步提醒: " + e.getMessage());
        }

        return ResponseUtil.ok();
    }

    /**
     * 批量出餐 (待制作 -> 待取餐)
     */
    @PostMapping("/batchReady")
    public Object batchReady(@LoginUser Integer userId, @RequestBody String body) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        List<Integer> orderIds = JacksonUtil.parseIntegerList(body, "orderIds");
        if (orderIds == null || orderIds.isEmpty()) {
            List<LitemallOrder> payOrders = orderService.querySelective(null, null, null, null, Collections.singletonList(OrderUtil.STATUS_PAY), 1, 100, "id", "asc");
            orderIds = new ArrayList<>();
            for (LitemallOrder o : payOrders) {
                orderIds.add(o.getId());
            }
        }

        int successCount = 0;
        for (Integer orderId : orderIds) {
            LitemallOrder order = orderService.findById(orderId);
            if (order != null && order.getOrderStatus().equals(OrderUtil.STATUS_PAY)) {
                order.setOrderStatus(OrderUtil.STATUS_SHIP);
                order.setShipChannel("到店自提");
                order.setShipSn(order.getOrderSn());
                order.setShipTime(LocalDateTime.now());
                if (orderService.updateWithOptimisticLocker(order) > 0) {
                    successCount++;
                    try {
                        LitemallUser user = userService.findById(order.getUserId());
                        String openid = (user != null) ? user.getWeixinOpenid() : null;
                        wxShippingService.uploadShippingInfo(
                                order.getOrderSn(),
                                order.getPayId(),
                                openid,
                                4,
                                "到店自提",
                                order.getOrderSn(),
                                "小满食光手作餐品已出餐，请凭提货码取餐",
                                order.getMobile() != null ? order.getMobile() : ""
                        );
                    } catch (Exception e) {
                        logger.warn("批量出餐微信发货同步提醒: " + e.getMessage());
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", successCount);
        return ResponseUtil.ok(result);
    }

    /**
     * 自提核销 (支持扫描二维码内容或输入6位提货码/订单号)
     */
    @PostMapping("/verify")
    public Object verify(@LoginUser Integer userId, @RequestBody String body) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        String code = JacksonUtil.parseString(body, "code");
        Integer orderId = JacksonUtil.parseInteger(body, "orderId");

        LitemallOrder order = null;
        if (orderId != null) {
            order = orderService.findById(orderId);
        } else if (!StringUtils.isEmpty(code)) {
            code = code.trim();
            // 如果传入完整 URL 或 JSON，提取其中的纯数字字符串
            if (code.contains("orderSn=")) {
                int idx = code.indexOf("orderSn=") + 8;
                int end = code.indexOf("&", idx);
                code = (end != -1) ? code.substring(idx, end) : code.substring(idx);
            }
            order = orderService.findBySn(code);
            if (order == null && code.length() == 6) {
                // 尝试根据后6位匹配今天的未核销订单
                List<Short> statuses = Arrays.asList(OrderUtil.STATUS_PAY, OrderUtil.STATUS_SHIP);
                List<LitemallOrder> candidates = orderService.querySelective(null, null, LocalDateTime.now().minusDays(2), null, statuses, 1, 50, "id", "desc");
                for (LitemallOrder o : candidates) {
                    if (o.getOrderSn().endsWith(code)) {
                        order = o;
                        break;
                    }
                }
            }
        }

        if (order == null) {
            return ResponseUtil.fail(603, "未找到对应的有效订单，请核对取餐码");
        }

        // 校验订单状态 (必须是已付款或待取餐)
        if (!order.getOrderStatus().equals(OrderUtil.STATUS_PAY) && !order.getOrderStatus().equals(OrderUtil.STATUS_SHIP)) {
            if (order.getOrderStatus().equals(OrderUtil.STATUS_CONFIRM) || order.getOrderStatus().equals(OrderUtil.STATUS_AUTO_CONFIRM)) {
                return ResponseUtil.fail(604, "该订单已核销完成，请勿重复核销");
            }
            return ResponseUtil.fail(605, "订单状态为 " + OrderUtil.orderStatusText(order) + "，无法核销");
        }

        order.setOrderStatus(OrderUtil.STATUS_CONFIRM);
        order.setConfirmTime(LocalDateTime.now());
        if (orderService.updateWithOptimisticLocker(order) == 0) {
            return ResponseUtil.updatedDateExpired();
        }

        List<LitemallOrderGoods> goodsList = orderGoodsService.queryByOid(order.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("goodsList", goodsList);
        return ResponseUtil.ok(data);
    }

    /**
     * 批量自提核销 (待取餐/待制作 -> 已完成)
     */
    @PostMapping("/batchVerify")
    public Object batchVerify(@LoginUser Integer userId, @RequestBody String body) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        List<Integer> orderIds = JacksonUtil.parseIntegerList(body, "orderIds");
        if (orderIds == null || orderIds.isEmpty()) {
            List<LitemallOrder> shipOrders = orderService.querySelective(null, null, null, null, Collections.singletonList(OrderUtil.STATUS_SHIP), 1, 100, "id", "asc");
            orderIds = new ArrayList<>();
            for (LitemallOrder o : shipOrders) {
                orderIds.add(o.getId());
            }
        }

        int successCount = 0;
        for (Integer orderId : orderIds) {
            LitemallOrder order = orderService.findById(orderId);
            if (order != null && (order.getOrderStatus().equals(OrderUtil.STATUS_PAY) || order.getOrderStatus().equals(OrderUtil.STATUS_SHIP))) {
                order.setOrderStatus(OrderUtil.STATUS_CONFIRM);
                order.setConfirmTime(LocalDateTime.now());
                if (orderService.updateWithOptimisticLocker(order) > 0) {
                    successCount++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", successCount);
        return ResponseUtil.ok(result);
    }

    /**
     * 补打小票 (重新触发云热敏打印机)
     */
    @PostMapping("/reprint")
    public Object reprint(@LoginUser Integer userId, @RequestBody String body) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        Integer orderId = JacksonUtil.parseInteger(body, "orderId");
        if (orderId == null) {
            return ResponseUtil.badArgument();
        }

        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgumentValue();
        }

        List<LitemallOrderGoods> goodsList = orderGoodsService.queryByOid(orderId);
        try {
            printerClient.printOrder(order, goodsList);
            return ResponseUtil.ok("补打小票指令已发送至打印机");
        } catch (Exception e) {
            logger.error("补打小票失败: " + e.getMessage(), e);
            return ResponseUtil.fail(606, "打印机通信失败: " + e.getMessage());
        }
    }

    /**
     * 审核退款 (同意或拒绝)
     */
    @PostMapping("/refund")
    public Object refund(@LoginUser Integer userId, @RequestBody String body) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        Integer orderId = JacksonUtil.parseInteger(body, "orderId");
        Boolean agree = JacksonUtil.parseBoolean(body, "agree");
        String reason = JacksonUtil.parseString(body, "reason");

        if (orderId == null || agree == null) {
            return ResponseUtil.badArgument();
        }

        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgumentValue();
        }

        if (!order.getOrderStatus().equals(OrderUtil.STATUS_REFUND)) {
            return ResponseUtil.fail(607, "当前订单不是申请退款状态");
        }

        LocalDateTime now = LocalDateTime.now();

        if (agree) {
            // 同意退款：调用微信原路退款
            try {
                WxPayRefundRequest wxPayRefundRequest = new WxPayRefundRequest();
                wxPayRefundRequest.setOutTradeNo(order.getOrderSn());
                wxPayRefundRequest.setOutRefundNo("refund_" + order.getOrderSn());
                Integer totalFee = order.getActualPrice().multiply(new BigDecimal(100)).intValue();
                wxPayRefundRequest.setTotalFee(totalFee);
                wxPayRefundRequest.setRefundFee(totalFee);
                WxPayRefundResult refundResult = wxPayService.refund(wxPayRefundRequest);

                order.setOrderStatus(OrderUtil.STATUS_REFUND_CONFIRM);
                order.setEndTime(now);
                order.setRefundAmount(order.getActualPrice());
                order.setRefundType("微信退款接口");
                order.setRefundContent(refundResult != null ? refundResult.getRefundId() : "审核通过退款");
                order.setRefundTime(now);
            } catch (Exception e) {
                logger.warn("微信退款接口调用异常，转为手动审核记录: " + e.getMessage());
                order.setOrderStatus(OrderUtil.STATUS_REFUND_CONFIRM);
                order.setEndTime(now);
                order.setRefundAmount(order.getActualPrice());
                order.setRefundType("商家手动退款");
                order.setRefundContent("退款异常转线下处理: " + e.getMessage());
                order.setRefundTime(now);
            }

            if (orderService.updateWithOptimisticLocker(order) == 0) {
                return ResponseUtil.updatedDateExpired();
            }

            // 恢复商品库存
            List<LitemallOrderGoods> orderGoodsList = orderGoodsService.queryByOid(orderId);
            for (LitemallOrderGoods orderGoods : orderGoodsList) {
                productService.addStock(orderGoods.getProductId(), orderGoods.getNumber());
            }

            // 恢复优惠券
            List<LitemallCouponUser> couponUsers = couponUserService.findByOid(orderId);
            for (LitemallCouponUser couponUser : couponUsers) {
                couponUser.setStatus(CouponUserConstant.STATUS_USABLE);
                couponUser.setUpdateTime(now);
                couponUserService.update(couponUser);
            }

            return ResponseUtil.ok("已同意退款并恢复库存");
        } else {
            // 拒绝退款：恢复到已付款状态，并记录拒绝原因
            order.setOrderStatus(OrderUtil.STATUS_PAY);
            order.setRefundContent("商家拒绝退款: " + (StringUtils.isEmpty(reason) ? "餐品已在制作或已出餐" : reason));
            if (orderService.updateWithOptimisticLocker(order) == 0) {
                return ResponseUtil.updatedDateExpired();
            }
            return ResponseUtil.ok("已拒绝退款申请");
        }
    }
}
