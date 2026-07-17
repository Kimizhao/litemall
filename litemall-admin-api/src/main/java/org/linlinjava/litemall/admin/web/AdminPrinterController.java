package org.linlinjava.litemall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
import org.linlinjava.litemall.core.printer.LitemallPrinterClient;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.core.validator.Order;
import org.linlinjava.litemall.core.validator.Sort;
import org.linlinjava.litemall.db.domain.LitemallPrinter;
import org.linlinjava.litemall.db.service.LitemallPrinterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/admin/printer")
@Validated
public class AdminPrinterController {
    private final Log logger = LogFactory.getLog(AdminPrinterController.class);

    @Autowired
    private LitemallPrinterService printerService;

    @Autowired
    private LitemallPrinterClient printerClient;

    @RequiresPermissions("admin:printer:list")
    @RequiresPermissionsDesc(menu = {"系统管理", "打印机管理"}, button = "查询")
    @GetMapping("/list")
    public Object list(String sn, String name,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        List<LitemallPrinter> printerList = printerService.querySelective(sn, name, page, limit, sort, order);

        // Dynamically sync online and status from the remote server for listed printers
        for (LitemallPrinter printer : printerList) {
            LitemallPrinter remoteStatus = printerClient.getPrinterStatus(printer.getSn());
            if (remoteStatus != null) {
                printer.setOnline(remoteStatus.getOnline());
                printer.setStatus(remoteStatus.getStatus());
                printerService.updateById(printer);
            }
        }

        return ResponseUtil.okList(printerList);
    }

    private Object validate(LitemallPrinter printer, boolean isCreate) {
        String sn = printer.getSn();
        if (StringUtils.isEmpty(sn)) {
            return ResponseUtil.badArgumentValue();
        }
        String name = printer.getName();
        if (StringUtils.isEmpty(name)) {
            return ResponseUtil.badArgumentValue();
        }
        if (isCreate) {
            String pkey = printer.getPkey();
            if (StringUtils.isEmpty(pkey)) {
                return ResponseUtil.badArgumentValue();
            }
        }
        return null;
    }

    @RequiresPermissions("admin:printer:create")
    @RequiresPermissionsDesc(menu = {"系统管理", "打印机管理"}, button = "添加")
    @PostMapping("/create")
    public Object create(@RequestBody LitemallPrinter printer) {
        Object error = validate(printer, true);
        if (error != null) {
            return error;
        }

        // 1. Try registering on the remote cloud print platform
        boolean success = printerClient.addPrinter(printer.getSn(), printer.getPkey(), printer.getName());
        if (!success) {
            return ResponseUtil.fail(501, "注册打印机到云平台失败，请检查SN和PKEY");
        }

        // 2. Query status from remote server to populate initial values
        LitemallPrinter remoteStatus = printerClient.getPrinterStatus(printer.getSn());
        if (remoteStatus != null) {
            printer.setOnline(remoteStatus.getOnline());
            printer.setStatus(remoteStatus.getStatus());
        } else {
            printer.setOnline(0); // Default offline
            printer.setStatus(0); // Default normal
        }

        // 3. Save to local database
        try {
            printerService.add(printer);
        } catch (Exception e) {
            logger.error("Failed to add printer to local database: " + e.getMessage(), e);
            return ResponseUtil.fail(502, "添加打印机失败，设备编号可能重复");
        }
        return ResponseUtil.ok(printer);
    }

    @RequiresPermissions("admin:printer:read")
    @RequiresPermissionsDesc(menu = {"系统管理", "打印机管理"}, button = "详情")
    @GetMapping("/read")
    public Object read(@NotNull Integer id) {
        LitemallPrinter printer = printerService.findById(id);
        if (printer == null) {
            return ResponseUtil.badArgumentValue();
        }
        return ResponseUtil.ok(printer);
    }

    @RequiresPermissions("admin:printer:update")
    @RequiresPermissionsDesc(menu = {"系统管理", "打印机管理"}, button = "编辑")
    @PostMapping("/update")
    public Object update(@RequestBody LitemallPrinter printer) {
        Object error = validate(printer, false);
        if (error != null) {
            return error;
        }

        LitemallPrinter existPrinter = printerService.findById(printer.getId());
        if (existPrinter == null) {
            return ResponseUtil.badArgumentValue();
        }

        // If name changes, update name on the remote platform
        if (!existPrinter.getName().equals(printer.getName())) {
            boolean success = printerClient.updatePrinter(printer.getSn(), printer.getName());
            if (!success) {
                return ResponseUtil.fail(501, "同步打印机名称到云平台失败");
            }
        }

        if (printerService.updateById(printer) == 0) {
            return ResponseUtil.updatedDataFailed();
        }
        return ResponseUtil.ok(printer);
    }

    @RequiresPermissions("admin:printer:delete")
    @RequiresPermissionsDesc(menu = {"系统管理", "打印机管理"}, button = "删除")
    @PostMapping("/delete")
    public Object delete(@RequestBody LitemallPrinter printer) {
        Integer id = printer.getId();
        if (id == null) {
            return ResponseUtil.badArgumentValue();
        }

        LitemallPrinter existPrinter = printerService.findById(id);
        if (existPrinter != null) {
            // Delete from the remote cloud platform
            printerClient.deletePrinter(existPrinter.getSn());
            // Logical delete locally
            printerService.deleteById(id);
        }

        return ResponseUtil.ok();
    }

    @RequiresPermissions("admin:printer:print-test")
    @RequiresPermissionsDesc(menu = {"系统管理", "打印机管理"}, button = "打印测试")
    @PostMapping("/print-test")
    public Object printTest(@RequestBody LitemallPrinter printer) {
        Integer id = printer.getId();
        if (id == null) {
            return ResponseUtil.badArgumentValue();
        }

        LitemallPrinter existPrinter = printerService.findById(id);
        if (existPrinter == null) {
            return ResponseUtil.badArgumentValue();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timeStr = LocalDateTime.now().format(formatter);

        // Build a formatted print test receipt matching standard markup tags
        String printContent = "<C><L1>小满食光</L1></C>\n" +
                "<C>-- 打印测试 --</C>\n" +
                "<BR>\n" +
                "设备编号：" + existPrinter.getSn() + "\n" +
                "设备名称：" + existPrinter.getName() + "\n" +
                "测试时间：" + timeStr + "\n" +
                "通道测试：通讯正常\n" +
                "<BR>\n" +
                "<C>*** 测试结束 ***</C>\n" +
                "<BR>\n" +
                "<BR>\n" +
                "<CUT>\n";

        boolean success = printerClient.print(existPrinter.getSn(), printContent, 1);
        if (success) {
            return ResponseUtil.ok("测试指令发送成功");
        } else {
            return ResponseUtil.fail(503, "发送测试打印指令失败，请检查打印机联网状态");
        }
    }

    public static class PrintRequest {
        private Integer id;
        private String content;
        private Integer times;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getTimes() {
            return times;
        }

        public void setTimes(Integer times) {
            this.times = times;
        }
    }

    @RequiresPermissions("admin:printer:print")
    @RequiresPermissionsDesc(menu = {"系统管理", "打印机管理"}, button = "打印")
    @PostMapping("/print")
    public Object print(@RequestBody PrintRequest printRequest) {
        Integer id = printRequest.getId();
        String content = printRequest.getContent();
        Integer times = printRequest.getTimes();

        if (id == null || StringUtils.isEmpty(content)) {
            return ResponseUtil.badArgumentValue();
        }
        if (times == null || times < 1 || times > 5) {
            times = 1;
        }

        LitemallPrinter existPrinter = printerService.findById(id);
        if (existPrinter == null) {
            return ResponseUtil.badArgumentValue();
        }

        boolean success = printerClient.print(existPrinter.getSn(), content, times);
        if (success) {
            return ResponseUtil.ok("打印指令发送成功");
        } else {
            return ResponseUtil.fail(503, "发送打印指令失败，请检查打印机联网状态");
        }
    }

    @RequiresPermissions("admin:printer:clean-queue")
    @RequiresPermissionsDesc(menu = {"系统管理", "打印机管理"}, button = "清空")
    @PostMapping("/clean-queue")
    public Object cleanQueue(@RequestBody LitemallPrinter printer) {
        Integer id = printer.getId();
        if (id == null) {
            return ResponseUtil.badArgumentValue();
        }

        LitemallPrinter existPrinter = printerService.findById(id);
        if (existPrinter == null) {
            return ResponseUtil.badArgumentValue();
        }

        Integer cleanNum = printerClient.cleanPrinterQueue(existPrinter.getSn());
        if (cleanNum >= 0) {
            return ResponseUtil.ok(cleanNum);
        } else {
            return ResponseUtil.fail(503, "清空云平台待打印队列失败");
        }
    }
}
