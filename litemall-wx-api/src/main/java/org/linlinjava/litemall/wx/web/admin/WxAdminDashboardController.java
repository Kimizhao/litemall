package org.linlinjava.litemall.wx.web.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.printer.LitemallPrinterClient;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.domain.LitemallOrder;
import org.linlinjava.litemall.db.domain.LitemallPrinter;
import org.linlinjava.litemall.db.service.LitemallOrderService;
import org.linlinjava.litemall.db.service.LitemallPrinterService;
import org.linlinjava.litemall.db.util.OrderUtil;
import org.linlinjava.litemall.wx.annotation.LoginUser;
import org.linlinjava.litemall.wx.service.WxAdminAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/wx/admin/dashboard")
@Validated
public class WxAdminDashboardController {
    private final Log logger = LogFactory.getLog(WxAdminDashboardController.class);

    @Autowired
    private WxAdminAuthService adminAuthService;

    @Autowired
    private LitemallOrderService orderService;

    @Autowired
    private LitemallPrinterService printerService;

    @Autowired
    private LitemallPrinterClient printerClient;

    /**
     * 商家仪表盘数据概览 (今日营收数据与云打印机状态)
     */
    @GetMapping("/index")
    public Object index(@LoginUser Integer userId) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // 查询今日已支付的订单 (201, 301, 401, 402)
        List<Short> paidStatuses = Arrays.asList(
                OrderUtil.STATUS_PAY,
                OrderUtil.STATUS_SHIP,
                OrderUtil.STATUS_CONFIRM,
                OrderUtil.STATUS_AUTO_CONFIRM
        );

        List<LitemallOrder> todayOrders = orderService.querySelective(null, null, todayStart, now, paidStatuses, 1, 1000, "id", "desc");
        int todayOrderCount = todayOrders.size();
        BigDecimal todayTurnover = BigDecimal.ZERO;
        for (LitemallOrder o : todayOrders) {
            if (o.getActualPrice() != null) {
                todayTurnover = todayTurnover.add(o.getActualPrice());
            }
        }

        // 待办统计
        int pendingMakingCount = orderService.countByStatus(Collections.singletonList(OrderUtil.STATUS_PAY));
        int pendingPickupCount = orderService.countByStatus(Collections.singletonList(OrderUtil.STATUS_SHIP));
        int pendingRefundCount = orderService.countByStatus(Collections.singletonList(OrderUtil.STATUS_REFUND));

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayOrderCount", todayOrderCount);
        stats.put("todayTurnover", todayTurnover);
        stats.put("pendingMakingCount", pendingMakingCount);
        stats.put("pendingPickupCount", pendingPickupCount);
        stats.put("pendingRefundCount", pendingRefundCount);

        // 获取云打印机状态
        Map<String, Object> printerVo = new HashMap<>();
        List<LitemallPrinter> printerList = printerService.querySelective(null, null, 1, 1, "add_time", "desc");
        if (printerList != null && !printerList.isEmpty()) {
            LitemallPrinter printer = printerList.get(0);
            printerVo.put("id", printer.getId());
            printerVo.put("name", printer.getName());
            printerVo.put("sn", printer.getSn());
            try {
                LitemallPrinter remote = printerClient.getPrinterStatus(printer.getSn());
                printerVo.put("online", remote != null ? remote.getOnline() : printer.getOnline());
                printerVo.put("status", remote != null ? remote.getStatus() : printer.getStatus());
            } catch (Exception e) {
                printerVo.put("online", printer.getOnline());
                printerVo.put("status", printer.getStatus());
            }
        } else {
            printerVo.put("online", 0);
            printerVo.put("name", "未配置打印机");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("stats", stats);
        data.put("printer", printerVo);
        return ResponseUtil.ok(data);
    }

    /**
     * 云打印机测试出票
     */
    @PostMapping("/print-test")
    public Object printTest(@LoginUser Integer userId) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        List<LitemallPrinter> printerList = printerService.querySelective(null, null, 1, 1, "add_time", "desc");
        if (printerList == null || printerList.isEmpty()) {
            return ResponseUtil.fail(610, "系统中未配置云打印机");
        }

        LitemallPrinter printer = printerList.get(0);
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String printContent = "<C><L1>小满食光</L1></C>\n" +
                "<C>-- 商家工作台测试出票 --</C>\n" +
                "<BR>\n" +
                "设备编号：" + printer.getSn() + "\n" +
                "设备名称：" + printer.getName() + "\n" +
                "测试时间：" + timeStr + "\n" +
                "通道状态：小程序移动端远程测试成功\n" +
                "<BR>\n" +
                "<C>*** 测试完毕 ***</C>\n" +
                "<BR>\n" +
                "<BR>\n" +
                "<CUT>\n";

        try {
            boolean success = printerClient.print(printer.getSn(), printContent, 1);
            if (success) {
                return ResponseUtil.ok("测试出票指令已发送至打印机");
            } else {
                return ResponseUtil.fail(611, "发送测试打印失败，请确认打印机在线");
            }
        } catch (Exception e) {
            logger.error("测试出票异常: " + e.getMessage(), e);
            return ResponseUtil.fail(612, "打印机通信异常: " + e.getMessage());
        }
    }
}
