package org.linlinjava.litemall.wx.web.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.domain.LitemallAdmin;
import org.linlinjava.litemall.wx.annotation.LoginUser;
import org.linlinjava.litemall.wx.service.WxAdminAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wx/admin/auth")
@Validated
public class WxAdminAuthController {
    private final Log logger = LogFactory.getLog(WxAdminAuthController.class);

    @Autowired
    private WxAdminAuthService adminAuthService;

    /**
     * 查询当前微信用户的管理员绑定状态
     */
    @GetMapping("/status")
    public Object status(@LoginUser Integer userId) {
        if (userId == null) {
            return ResponseUtil.unlogin();
        }

        boolean isBound = adminAuthService.isMerchantAdmin(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("isBound", isBound);

        if (isBound) {
            LitemallAdmin admin = adminAuthService.getShopAdmin();
            if (admin != null) {
                Map<String, Object> adminInfo = new HashMap<>();
                adminInfo.put("username", admin.getUsername());
                adminInfo.put("avatar", admin.getAvatar());
                data.put("admin", adminInfo);
            }
        }
        return ResponseUtil.ok(data);
    }

    /**
     * 输入账号密码进行管理员绑定
     */
    @PostMapping("/login")
    public Object login(@LoginUser Integer userId, @RequestBody String body) {
        if (userId == null) {
            return ResponseUtil.unlogin();
        }

        String username = JacksonUtil.parseString(body, "username");
        String password = JacksonUtil.parseString(body, "password");

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return ResponseUtil.badArgument();
        }

        boolean success = adminAuthService.bindAdmin(userId, username, password);
        if (!success) {
            return ResponseUtil.fail(601, "管理员账号或密码不正确");
        }

        LitemallAdmin admin = adminAuthService.getShopAdmin();
        Map<String, Object> data = new HashMap<>();
        data.put("isBound", true);
        if (admin != null) {
            Map<String, Object> adminInfo = new HashMap<>();
            adminInfo.put("username", admin.getUsername());
            adminInfo.put("avatar", admin.getAvatar());
            data.put("admin", adminInfo);
        }

        return ResponseUtil.ok(data);
    }

    /**
     * 解除当前微信用户的管理员绑定
     */
    @PostMapping("/logout")
    public Object logout(@LoginUser Integer userId) {
        if (userId == null) {
            return ResponseUtil.unlogin();
        }
        adminAuthService.unbindAdmin(userId);
        return ResponseUtil.ok();
    }
}
