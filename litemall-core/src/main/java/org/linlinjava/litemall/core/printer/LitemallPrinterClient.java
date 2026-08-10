package org.linlinjava.litemall.core.printer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.db.domain.LitemallPrinter;
import org.linlinjava.litemall.db.domain.LitemallOrder;
import org.linlinjava.litemall.db.domain.LitemallOrderGoods;
import org.linlinjava.litemall.db.service.LitemallPrinterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.List;
import java.math.BigDecimal;
import java.util.Arrays;

@Component
public class LitemallPrinterClient {
    private final Log logger = LogFactory.getLog(LitemallPrinterClient.class);

    @Autowired
    private LitemallPrinterProperties properties;

    @Autowired
    private LitemallPrinterService printerService;

    private boolean isConfigured() {
        return !StringUtils.isEmpty(properties.getAppid()) && !StringUtils.isEmpty(properties.getAppsecret());
    }

    public boolean addPrinter(String sn, String pkey, String name) {
        if (!isConfigured()) {
            logger.warn("Printer AppID/AppSecret not configured. Skipping remote registration.");
            return true; // Bypass to allow local testing
        }
        try {
            Api api = new Api(properties.getAppid(), properties.getAppsecret());
            String res = api.addPrinter(sn, pkey, name);
            logger.info("Remote add printer response: " + res);
            Integer errorcode = JacksonUtil.parseInteger(res, "errorcode");
            if (errorcode != null && errorcode == 0) {
                return true;
            } else {
                logger.error("Failed to register printer remotely: " + res);
            }
        } catch (Exception e) {
            logger.error("Remote add printer exception: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean deletePrinter(String sn) {
        if (!isConfigured()) {
            logger.warn("Printer AppID/AppSecret not configured. Skipping remote deletion.");
            return true; // Bypass to allow local testing
        }
        try {
            Api api = new Api(properties.getAppid(), properties.getAppsecret());
            String res = api.deletePrinter(sn);
            logger.info("Remote delete printer response: " + res);
            Integer errorcode = JacksonUtil.parseInteger(res, "errorcode");
            if (errorcode != null && (errorcode == 0 || errorcode == 5)) { // 5: device does not exist, can be considered deleted
                return true;
            }
        } catch (Exception e) {
            logger.error("Remote delete printer exception: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean updatePrinter(String sn, String name) {
        if (!isConfigured()) {
            logger.warn("Printer AppID/AppSecret not configured. Skipping remote update.");
            return true; // Bypass to allow local testing
        }
        try {
            Api api = new Api(properties.getAppid(), properties.getAppsecret());
            String res = api.updatePrinter(sn, name);
            logger.info("Remote update printer response: " + res);
            Integer errorcode = JacksonUtil.parseInteger(res, "errorcode");
            if (errorcode != null && errorcode == 0) {
                return true;
            }
        } catch (Exception e) {
            logger.error("Remote update printer exception: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean print(String sn, String content, int times) {
        if (!isConfigured()) {
            logger.warn("Printer AppID/AppSecret not configured. Skipping remote print.");
            return true; // Bypass to allow local testing
        }
        try {
            Api api = new Api(properties.getAppid(), properties.getAppsecret());
            String res = api.print(sn, content, times);
            logger.info("Remote print response: " + res);
            Integer errorcode = JacksonUtil.parseInteger(res, "errorcode");
            if (errorcode != null && errorcode == 0) {
                return true;
            }
        } catch (Exception e) {
            logger.error("Remote print exception: " + e.getMessage(), e);
        }
        return false;
    }

    public Integer cleanPrinterQueue(String sn) {
        if (!isConfigured()) {
            logger.warn("Printer AppID/AppSecret not configured. Skipping remote clean queue.");
            return 0; // Bypass to allow local testing
        }
        try {
            Api api = new Api(properties.getAppid(), properties.getAppsecret());
            String res = api.deletePrints(sn);
            logger.info("Remote clean queue response: " + res);
            Integer errorcode = JacksonUtil.parseInteger(res, "errorcode");
            if (errorcode != null && errorcode == 0) {
                Integer cleannum = JacksonUtil.parseInteger(res, "cleannum");
                return cleannum != null ? cleannum : 0;
            }
        } catch (Exception e) {
            logger.error("Remote clean queue exception: " + e.getMessage(), e);
        }
        return -1; // Indicates failure
    }

    public LitemallPrinter getPrinterStatus(String sn) {
        if (!isConfigured()) {
            return null; // Return null so caller keeps existing database state
        }
        try {
            Api api = new Api(properties.getAppid(), properties.getAppsecret());
            String res = api.getPrinter(sn);
            logger.info("Remote query printer response: " + res);
            Integer errorcode = JacksonUtil.parseInteger(res, "errorcode");
            if (errorcode != null && errorcode == 0) {
                Integer online = JacksonUtil.parseInteger(res, "online");
                Integer status = JacksonUtil.parseInteger(res, "status");
                LitemallPrinter printer = new LitemallPrinter();
                printer.setSn(sn);
                printer.setOnline(online != null ? online : 0);
                printer.setStatus(status != null ? status : 0);
                return printer;
            }
        } catch (Exception e) {
            logger.error("Remote query printer exception: " + e.getMessage(), e);
        }
        return null;
    }

    public void printOrder(LitemallOrder order, List<LitemallOrderGoods> orderGoods) {
        // 1. 获取所有正常的打印机
        List<LitemallPrinter> printers = printerService.queryAll();
        if (printers == null || printers.isEmpty()) {
            logger.info("没有配置打印机，跳过订单自动打印。");
            return;
        }

        // 2. 拼接订单票据排版内容
        StringBuilder sb = new StringBuilder();
        sb.append("<C><L1>小满食光</L1></C>\n");
        sb.append("<C>-- 订单结账单 --</C>\n");
        String timeStr = "";
        if (order.getAddTime() != null) {
            timeStr = order.getAddTime().toString().replace("T", " ");
        }
        sb.append("下单时间：").append(timeStr).append("\n");
        sb.append("订单编号：").append(order.getOrderSn()).append("\n");
        sb.append("收 货 人：").append(order.getConsignee() != null ? order.getConsignee() : "").append("\n");
        sb.append("联系电话：").append(order.getMobile() != null ? order.getMobile() : "").append("\n");
        sb.append("收货地址：").append(order.getAddress() != null ? order.getAddress() : "").append("\n");
        if (!StringUtils.isEmpty(order.getMessage())) {
            sb.append("订单备注：").append(order.getMessage()).append("\n");
        }
        sb.append("<BR>\n");
        sb.append("--------------------------------\n");
        sb.append("商品名称          数量      单价\n");
        for (LitemallOrderGoods goods : orderGoods) {
            String nameStr = padOrCut(goods.getGoodsName(), 14);
            String numStr = padOrCut("    x" + goods.getNumber(), 10);
            String priceStr = padOrCutRight(goods.getPrice().toString() + "元", 8);
            sb.append(nameStr).append(numStr).append(priceStr).append("\n");
            if (goods.getSpecifications() != null && goods.getSpecifications().length > 0) {
                sb.append(" 规格: ").append(Arrays.toString(goods.getSpecifications())).append("\n");
            }
        }
        sb.append("--------------------------------\n");
        sb.append("商品总价：").append(order.getGoodsPrice() != null ? order.getGoodsPrice() : "0.00").append("元\n");
        sb.append("配送费用：").append(order.getFreightPrice() != null ? order.getFreightPrice() : "0.00").append("元\n");
        if (order.getCouponPrice() != null && order.getCouponPrice().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("优惠金额：-").append(order.getCouponPrice()).append("元\n");
        }
        sb.append("实付总计：").append(order.getActualPrice() != null ? order.getActualPrice() : "0.00").append("元\n");
        sb.append("<BR>\n");
        sb.append("<AUDIO-COMMON>\n"); // 语音播报：你有新的订单，请及时处理

        String content = sb.toString();

        // 3. 遍历所有配置的打印机进行打印
        for (LitemallPrinter printer : printers) {
            if (printer.getStatus() != null && printer.getStatus() == 0) {
                logger.info("开始自动打印订单，打印机 SN: " + printer.getSn() + ", 订单号: " + order.getOrderSn());
                print(printer.getSn(), content, 1);
            }
        }
    }

    private String padOrCut(String str, int targetWidth) {
        if (str == null) {
            str = "";
        }
        StringBuilder sb = new StringBuilder();
        int currentWidth = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int charWidth = (c > 255) ? 2 : 1;
            if (currentWidth + charWidth > targetWidth) {
                break;
            }
            sb.append(c);
            currentWidth += charWidth;
        }
        while (currentWidth < targetWidth) {
            sb.append(' ');
            currentWidth++;
        }
        return sb.toString();
    }

    private String padOrCutRight(String str, int targetWidth) {
        if (str == null) {
            str = "";
        }
        StringBuilder sb = new StringBuilder();
        int currentWidth = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int charWidth = (c > 255) ? 2 : 1;
            if (currentWidth + charWidth > targetWidth) {
                break;
            }
            sb.append(c);
            currentWidth += charWidth;
        }
        StringBuilder result = new StringBuilder();
        while (currentWidth < targetWidth) {
            result.append(' ');
            currentWidth++;
        }
        result.append(sb);
        return result.toString();
    }
}
