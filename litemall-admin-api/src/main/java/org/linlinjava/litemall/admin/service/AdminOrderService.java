package org.linlinjava.litemall.admin.service;

import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.notify.NotifyService;
import org.linlinjava.litemall.core.notify.NotifyType;
import org.linlinjava.litemall.core.util.DateTimeUtil;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.core.wx.WxShippingService;
import org.linlinjava.litemall.db.domain.*;
import org.linlinjava.litemall.db.service.*;
import org.linlinjava.litemall.db.util.CouponUserConstant;
import org.linlinjava.litemall.db.util.GrouponConstant;
import org.linlinjava.litemall.db.util.OrderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.linlinjava.litemall.admin.util.AdminResponseCode.*;
import static org.linlinjava.litemall.admin.util.AdminResponseCode.ORDER_PAY_FAILED;

@Service

public class AdminOrderService {
    private final Log logger = LogFactory.getLog(AdminOrderService.class);

    @Autowired
    private LitemallOrderGoodsService orderGoodsService;
    @Autowired
    private LitemallOrderService orderService;
    @Autowired
    private LitemallGoodsProductService productService;
    @Autowired
    private LitemallUserService userService;
    @Autowired
    private LitemallCommentService commentService;
    @Autowired
    private WxPayService wxPayService;
    @Autowired
    private NotifyService notifyService;
    @Autowired
    private LogHelper logHelper;
    @Autowired
    private LitemallCouponUserService couponUserService;
    @Autowired
    private org.linlinjava.litemall.core.printer.LitemallPrinterClient printerClient;
    @Autowired
    private WxShippingService wxShippingService;

    public Object list(String nickname, String consignee, String orderSn, LocalDateTime start, LocalDateTime end, List<Short> orderStatusArray,
                       Integer page, Integer limit, String sort, String order) {
        Map<String, Object> data = (Map)orderService.queryVoSelective(nickname, consignee, orderSn, start, end, orderStatusArray, page, limit, sort, order);
        return ResponseUtil.ok(data);
    }

    public Object detail(Integer id) {
        LitemallOrder order = orderService.findById(id);
        List<LitemallOrderGoods> orderGoods = orderGoodsService.queryByOid(id);
        UserVo user = userService.findUserVoById(order.getUserId());
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("orderGoods", orderGoods);
        data.put("user", user);

        return ResponseUtil.ok(data);
    }

    /**
     * 订单退款
     * <p>
     * 1. 检测当前订单是否能够退款;
     * 2. 微信退款操作;
     * 3. 设置订单退款确认状态；
     * 4. 订单商品库存回库。
     * <p>
     * TODO
     * 虽然接入了微信退款API，但是从安全角度考虑，建议开发者删除这里微信退款代码，采用以下两步走步骤：
     * 1. 管理员登录微信官方支付平台点击退款操作进行退款
     * 2. 管理员登录litemall管理后台点击退款操作进行订单状态修改和商品库存回库
     *
     * @param body 订单信息，{ orderId：xxx }
     * @return 订单退款操作结果
     */
    @Transactional
    public Object refund(String body) {
        Integer orderId = JacksonUtil.parseInteger(body, "orderId");
        String refundMoney = JacksonUtil.parseString(body, "refundMoney");
        if (orderId == null) {
            return ResponseUtil.badArgument();
        }
        if (StringUtils.isEmpty(refundMoney)) {
            return ResponseUtil.badArgument();
        }

        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgument();
        }

        if (order.getActualPrice().compareTo(new BigDecimal(refundMoney)) != 0) {
            return ResponseUtil.badArgumentValue();
        }

        // 如果订单不是退款状态，则不能退款
        if (!order.getOrderStatus().equals(OrderUtil.STATUS_REFUND)) {
            return ResponseUtil.fail(ORDER_CONFIRM_NOT_ALLOWED, "订单不能确认收货");
        }

        // 微信退款
        WxPayRefundRequest wxPayRefundRequest = new WxPayRefundRequest();
        wxPayRefundRequest.setOutTradeNo(order.getOrderSn());
        wxPayRefundRequest.setOutRefundNo("refund_" + order.getOrderSn());
        // 元转成分
        Integer totalFee = order.getActualPrice().multiply(new BigDecimal(100)).intValue();
        wxPayRefundRequest.setTotalFee(totalFee);
        wxPayRefundRequest.setRefundFee(totalFee);

        WxPayRefundResult wxPayRefundResult;
        try {
            wxPayRefundResult = wxPayService.refund(wxPayRefundRequest);
        } catch (WxPayException e) {
            logger.error(e.getMessage(), e);
            return ResponseUtil.fail(ORDER_REFUND_FAILED, "订单退款失败");
        }
        if (!wxPayRefundResult.getReturnCode().equals("SUCCESS")) {
            logger.warn("refund fail: " + wxPayRefundResult.getReturnMsg());
            return ResponseUtil.fail(ORDER_REFUND_FAILED, "订单退款失败");
        }
        if (!wxPayRefundResult.getResultCode().equals("SUCCESS")) {
            logger.warn("refund fail: " + wxPayRefundResult.getReturnMsg());
            return ResponseUtil.fail(ORDER_REFUND_FAILED, "订单退款失败");
        }

        LocalDateTime now = LocalDateTime.now();
        // 设置订单取消状态
        order.setOrderStatus(OrderUtil.STATUS_REFUND_CONFIRM);
        order.setEndTime(now);
        // 记录订单退款相关信息
        order.setRefundAmount(order.getActualPrice());
        order.setRefundType("微信退款接口");
        order.setRefundContent(wxPayRefundResult.getRefundId());
        order.setRefundTime(now);
        if (orderService.updateWithOptimisticLocker(order) == 0) {
            throw new RuntimeException("更新数据已失效");
        }

        // 商品货品数量增加
        List<LitemallOrderGoods> orderGoodsList = orderGoodsService.queryByOid(orderId);
        for (LitemallOrderGoods orderGoods : orderGoodsList) {
            Integer productId = orderGoods.getProductId();
            Short number = orderGoods.getNumber();
            if (productService.addStock(productId, number) == 0) {
                throw new RuntimeException("商品货品库存增加失败");
            }
        }

        // 返还优惠券
        List<LitemallCouponUser> couponUsers = couponUserService.findByOid(orderId);
        for (LitemallCouponUser couponUser: couponUsers) {
            // 优惠券状态设置为可使用
            couponUser.setStatus(CouponUserConstant.STATUS_USABLE);
            couponUser.setUpdateTime(LocalDateTime.now());
            couponUserService.update(couponUser);
        }

        //TODO 发送邮件和短信通知，这里采用异步发送
        // 退款成功通知用户, 例如“您申请的订单退款 [ 单号:{1} ] 已成功，请耐心等待到账。”
        // 注意订单号只发后6位
        notifyService.notifySmsTemplate(order.getMobile(), NotifyType.REFUND,
                new String[]{order.getOrderSn().substring(8, 14)});

        logHelper.logOrderSucceed("退款", "订单编号 " + order.getOrderSn());
        return ResponseUtil.ok();
    }

    /**
     * 发货
     * 1. 检测当前订单是否能够发货
     * 2. 设置订单发货状态
     * 3. 联动微信小程序发货信息管理接口 (upload_shipping_info)
     *
     * @param body 订单信息，{ orderId：xxx, shipSn: xxx, shipChannel: xxx, logisticsType: xxx }
     * @return 订单操作结果
     * 成功则 { errno: 0, errmsg: '成功', data: { wxShipping: ... } }
     * 失败则 { errno: XXX, errmsg: XXX }
     */
    public Object ship(String body) {
        Integer orderId = JacksonUtil.parseInteger(body, "orderId");
        String shipSn = JacksonUtil.parseString(body, "shipSn");
        String shipChannel = JacksonUtil.parseString(body, "shipChannel");
        Integer logisticsType = JacksonUtil.parseInteger(body, "logisticsType");

        if (orderId == null) {
            return ResponseUtil.badArgument();
        }

        // 默认物流模式判断: 1快递, 2同城配送, 3虚拟商品, 4用户自提 (默认优先为同城配送)
        if (logisticsType == null || logisticsType < 1 || logisticsType > 4) {
            logisticsType = 2; // 默认同城配送
        }

        // 快递模式必须有快递公司和单号；其他模式如未填则给默认值
        if (logisticsType == 1) {
            if (!StringUtils.hasText(shipSn) || !StringUtils.hasText(shipChannel)) {
                return ResponseUtil.badArgument();
            }
        } else {
            if (!StringUtils.hasText(shipChannel)) {
                if (logisticsType == 2) {
                    shipChannel = "同城配送";
                } else if (logisticsType == 4) {
                    shipChannel = "到店自提";
                } else {
                    shipChannel = "无需物流";
                }
            }
            if (!StringUtils.hasText(shipSn)) {
                shipSn = "NONE";
            }
        }

        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgument();
        }

        // 如果订单不是已付款状态，则不能发货
        if (!order.getOrderStatus().equals(OrderUtil.STATUS_PAY)) {
            return ResponseUtil.fail(ORDER_CONFIRM_NOT_ALLOWED, "订单不能确认收货");
        }

        order.setOrderStatus(OrderUtil.STATUS_SHIP);
        order.setShipSn(shipSn);
        order.setShipChannel(shipChannel);
        order.setShipTime(LocalDateTime.now());
        if (orderService.updateWithOptimisticLocker(order) == 0) {
            return ResponseUtil.updatedDateExpired();
        }

        // 异步发送短信通知
        if (StringUtils.hasText(order.getMobile())) {
            notifyService.notifySmsTemplate(order.getMobile(), NotifyType.SHIP, new String[]{shipChannel, shipSn});
        }

        logHelper.logOrderSucceed("发货", "订单编号 " + order.getOrderSn());

        // 微信小程序发货信息管理接口联动上报
        Map<String, Object> wxShippingResult = null;
        try {
            LitemallUser user = userService.findById(order.getUserId());
            String openid = (user != null) ? user.getWeixinOpenid() : null;
            String itemDesc = buildItemDesc(order.getId());
            wxShippingResult = wxShippingService.uploadShippingInfo(
                    order.getOrderSn(),
                    order.getPayId(),
                    openid,
                    logisticsType,
                    shipChannel,
                    shipSn,
                    itemDesc,
                    order.getMobile()
            );
            logger.info("订单发货微信同步结果 orderSn=" + order.getOrderSn() + ": " + wxShippingResult);
        } catch (Exception e) {
            logger.error("微信发货上报异常: " + e.getMessage(), e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("wxShipping", wxShippingResult);
        return ResponseUtil.ok(data);
    }

    /**
     * 手动重新同步微信小程序发货信息
     */
    public Object syncWxShipping(String body) {
        Integer orderId = JacksonUtil.parseInteger(body, "orderId");
        Integer logisticsType = JacksonUtil.parseInteger(body, "logisticsType");
        String shipSn = JacksonUtil.parseString(body, "shipSn");
        String shipChannel = JacksonUtil.parseString(body, "shipChannel");

        if (orderId == null) {
            return ResponseUtil.badArgument();
        }
        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgument();
        }

        if (!StringUtils.hasText(shipSn)) {
            shipSn = order.getShipSn();
        }
        if (!StringUtils.hasText(shipChannel)) {
            shipChannel = order.getShipChannel();
        }

        LitemallUser user = userService.findById(order.getUserId());
        String openid = (user != null) ? user.getWeixinOpenid() : null;
        String itemDesc = buildItemDesc(order.getId());

        Map<String, Object> wxResult = wxShippingService.uploadShippingInfo(
                order.getOrderSn(),
                order.getPayId(),
                openid,
                logisticsType,
                shipChannel,
                shipSn,
                itemDesc,
                order.getMobile()
        );

        return ResponseUtil.ok(wxResult);
    }

    /**
     * 查询订单在微信侧的发货与收货状态
     */
    public Object getWxShippingStatus(Integer orderId) {
        if (orderId == null) {
            return ResponseUtil.badArgument();
        }
        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgument();
        }

        Map<String, Object> shippingInfo = wxShippingService.getOrderShippingInfo(order.getPayId(), order.getOrderSn());
        Map<String, Object> managedInfo = wxShippingService.isTradeManaged();

        Map<String, Object> data = new HashMap<>();
        data.put("shippingInfo", shippingInfo);
        data.put("managedInfo", managedInfo);
        return ResponseUtil.ok(data);
    }

    /**
     * 获取微信官方支持的运力公司列表
     */
    public Object getWxDeliveryList() {
        return ResponseUtil.ok(wxShippingService.getDeliveryList());
    }

    /**
     * 构建商品简短描述 (最多120字)
     */
    private String buildItemDesc(Integer orderId) {
        try {
            List<LitemallOrderGoods> goodsList = orderGoodsService.queryByOid(orderId);
            if (goodsList != null && !goodsList.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < goodsList.size(); i++) {
                    if (i > 0) sb.append("; ");
                    LitemallOrderGoods g = goodsList.get(i);
                    sb.append(g.getGoodsName());
                    if (g.getNumber() != null && g.getNumber() > 1) {
                        sb.append("x").append(g.getNumber());
                    }
                    if (sb.length() > 90) {
                        sb.append(" 等");
                        break;
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            logger.error("构建商品描述异常: " + e.getMessage(), e);
        }
        return "小满食光商品";
    }

    /**
     * 删除订单
     * 1. 检测当前订单是否能够删除
     * 2. 删除订单
     *
     * @param body 订单信息，{ orderId：xxx }
     * @return 订单操作结果
     * 成功则 { errno: 0, errmsg: '成功' }
     * 失败则 { errno: XXX, errmsg: XXX }
     */
    public Object delete(String body) {
        Integer orderId = JacksonUtil.parseInteger(body, "orderId");
        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgument();
        }

        // 如果订单不是关闭状态(已取消、系统取消、已退款、用户已确认、系统已确认)，则不能删除
        Short status = order.getOrderStatus();
        if (!status.equals(OrderUtil.STATUS_CANCEL) && !status.equals(OrderUtil.STATUS_AUTO_CANCEL) &&
                !status.equals(OrderUtil.STATUS_CONFIRM) &&!status.equals(OrderUtil.STATUS_AUTO_CONFIRM) &&
                !status.equals(OrderUtil.STATUS_REFUND_CONFIRM)) {
            return ResponseUtil.fail(ORDER_DELETE_FAILED, "订单不能删除");
        }
        // 删除订单
        orderService.deleteById(orderId);
        // 删除订单商品
        orderGoodsService.deleteByOrderId(orderId);
        logHelper.logOrderSucceed("删除", "订单编号 " + order.getOrderSn());
        return ResponseUtil.ok();
    }

    /**
     * 回复订单商品
     *
     * @param body 订单信息，{ orderId：xxx }
     * @return 订单操作结果
     * 成功则 { errno: 0, errmsg: '成功' }
     * 失败则 { errno: XXX, errmsg: XXX }
     */
    public Object reply(String body) {
        Integer commentId = JacksonUtil.parseInteger(body, "commentId");
        if (commentId == null || commentId == 0) {
            return ResponseUtil.badArgument();
        }
        // 目前只支持回复一次
        LitemallComment comment = commentService.findById(commentId);
        if(comment == null){
            return ResponseUtil.badArgument();
        }
        if (!StringUtils.isEmpty(comment.getAdminContent())) {
            return ResponseUtil.fail(ORDER_REPLY_EXIST, "订单商品已回复！");
        }
        String content = JacksonUtil.parseString(body, "content");
        if (StringUtils.isEmpty(content)) {
            return ResponseUtil.badArgument();
        }
        // 更新评价回复
        comment.setAdminContent(content);
        commentService.updateById(comment);

        return ResponseUtil.ok();
    }

    public Object pay(String body) {
        Integer orderId = JacksonUtil.parseInteger(body, "orderId");
        String newMoney = JacksonUtil.parseString(body, "newMoney");

        if (orderId == null || StringUtils.isEmpty(newMoney)) {
            return ResponseUtil.badArgument();
        }
        BigDecimal actualPrice = new BigDecimal(newMoney);

        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgument();
        }
        if (!order.getOrderStatus().equals(OrderUtil.STATUS_CREATE)) {
            return ResponseUtil.fail(ORDER_PAY_FAILED, "当前订单状态不支持线下收款");
        }

        order.setActualPrice(actualPrice);
        order.setOrderStatus(OrderUtil.STATUS_PAY);
        if (orderService.updateWithOptimisticLocker(order) == 0) {
            return WxPayNotifyResponse.fail("更新数据已失效");
        }

        // 联动打印机自动打印订单
        try {
            List<LitemallOrderGoods> orderGoods = orderGoodsService.queryByOid(order.getId());
            printerClient.printOrder(order, orderGoods);
        } catch (Exception e) {
            logger.error("订单付款成功自动打印异常: " + e.getMessage(), e);
        }

        return ResponseUtil.ok();
    }
}
