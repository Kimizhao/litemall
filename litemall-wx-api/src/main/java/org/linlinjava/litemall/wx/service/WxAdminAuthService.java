package org.linlinjava.litemall.wx.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.bcrypt.BCryptPasswordEncoder;
import org.linlinjava.litemall.db.domain.LitemallAdmin;
import org.linlinjava.litemall.db.domain.LitemallSystem;
import org.linlinjava.litemall.db.domain.LitemallSystemExample;
import org.linlinjava.litemall.db.service.LitemallAdminService;
import org.linlinjava.litemall.db.service.LitemallSystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WxAdminAuthService {
    private final Log logger = LogFactory.getLog(WxAdminAuthService.class);

    public static final String KEY_BOUND_USERS = "xiaoman_merchant_bound_users";
    public static final String DEFAULT_SHOP_ADMIN_USERNAME = "xiaoman";

    @Autowired
    private LitemallAdminService adminService;

    @Autowired
    private LitemallSystemConfigService systemConfigService;

    /**
     * 获取当前所有已绑定的微信 userId 列表
     */
    public synchronized Set<Integer> getBoundUserIds() {
        Map<String, String> configs = systemConfigService.queryAll();
        String json = configs.get(KEY_BOUND_USERS);
        if (StringUtils.isEmpty(json)) {
            return new HashSet<>();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Integer> list = mapper.readValue(json, new TypeReference<List<Integer>>() {});
            return new HashSet<>(list != null ? list : Collections.emptyList());
        } catch (Exception e) {
            logger.error("Failed to parse bound users json: " + json, e);
            return new HashSet<>();
        }
    }

    /**
     * 判断指定微信 userId 是否拥有店铺管理员绑定身份
     */
    public boolean isMerchantAdmin(Integer userId) {
        if (userId == null) {
            return false;
        }
        Set<Integer> boundUsers = getBoundUserIds();
        return boundUsers.contains(userId);
    }

    /**
     * 校验账号密码并绑定当前微信 userId (支持 1-to-N 绑定)
     */
    public synchronized boolean bindAdmin(Integer userId, String username, String password) {
        if (userId == null || StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return false;
        }

        List<LitemallAdmin> adminList = adminService.findAdmin(username);
        if (adminList == null || adminList.isEmpty()) {
            return false;
        }
        LitemallAdmin admin = adminList.get(0);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(password, admin.getPassword())) {
            return false;
        }

        // 校验通过，写入绑定列表
        Set<Integer> boundUsers = getBoundUserIds();
        boundUsers.add(userId);
        saveBoundUserIds(boundUsers);
        return true;
    }

    /**
     * 解除当前微信用户的管理员绑定
     */
    public synchronized boolean unbindAdmin(Integer userId) {
        if (userId == null) {
            return false;
        }
        Set<Integer> boundUsers = getBoundUserIds();
        if (boundUsers.remove(userId)) {
            saveBoundUserIds(boundUsers);
            return true;
        }
        return false;
    }

    /**
     * 获取小满店铺管理员公开信息
     */
    public LitemallAdmin getShopAdmin() {
        List<LitemallAdmin> adminList = adminService.findAdmin(DEFAULT_SHOP_ADMIN_USERNAME);
        if (adminList != null && !adminList.isEmpty()) {
            return adminList.get(0);
        }
        return null;
    }

    private synchronized void saveBoundUserIds(Set<Integer> userIds) {
        String json = JacksonUtil.toJson(new ArrayList<>(userIds));
        Map<String, String> configs = systemConfigService.queryAll();
        if (configs.containsKey(KEY_BOUND_USERS)) {
            Map<String, String> updateMap = new HashMap<>();
            updateMap.put(KEY_BOUND_USERS, json);
            systemConfigService.updateConfig(updateMap);
        } else {
            systemConfigService.addConfig(KEY_BOUND_USERS, json);
        }
    }
}
