package org.linlinjava.litemall.wx.web.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.domain.LitemallCategory;
import org.linlinjava.litemall.db.domain.LitemallGoods;
import org.linlinjava.litemall.db.service.LitemallCategoryService;
import org.linlinjava.litemall.db.service.LitemallGoodsService;
import org.linlinjava.litemall.wx.annotation.LoginUser;
import org.linlinjava.litemall.wx.service.WxAdminAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/wx/admin/category")
@Validated
public class WxAdminCategoryController {
    private final Log logger = LogFactory.getLog(WxAdminCategoryController.class);

    @Autowired
    private WxAdminAuthService adminAuthService;

    @Autowired
    private LitemallCategoryService categoryService;

    @Autowired
    private LitemallGoodsService goodsService;

    /**
     * 类目树状列表 (一级类目及其下属二级类目)
     */
    @GetMapping("/list")
    public Object list(@LoginUser Integer userId) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        List<LitemallCategory> l1List = categoryService.queryL1();
        List<Map<String, Object>> categoryTree = new ArrayList<>();

        for (LitemallCategory l1 : l1List) {
            Map<String, Object> l1Vo = new HashMap<>();
            l1Vo.put("id", l1.getId());
            l1Vo.put("name", l1.getName());
            l1Vo.put("level", l1.getLevel());
            l1Vo.put("iconUrl", l1.getIconUrl());
            l1Vo.put("picUrl", l1.getPicUrl());
            l1Vo.put("sortOrder", l1.getSortOrder());

            List<LitemallCategory> l2List = categoryService.queryByPid(l1.getId());
            l1Vo.put("children", l2List);
            categoryTree.add(l1Vo);
        }

        return ResponseUtil.ok(categoryTree);
    }

    /**
     * 创建或编辑类目
     */
    @PostMapping("/save")
    public Object save(@LoginUser Integer userId, @RequestBody String body) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        Integer id = JacksonUtil.parseInteger(body, "id");
        String name = JacksonUtil.parseString(body, "name");
        String level = JacksonUtil.parseString(body, "level");
        Integer pid = JacksonUtil.parseInteger(body, "pid");
        String iconUrl = JacksonUtil.parseString(body, "iconUrl");
        String picUrl = JacksonUtil.parseString(body, "picUrl");
        Integer sortOrder = JacksonUtil.parseInteger(body, "sortOrder");

        if (StringUtils.isEmpty(name)) {
            return ResponseUtil.badArgument();
        }

        if (StringUtils.isEmpty(level)) {
            level = (pid != null && pid > 0) ? "L2" : "L1";
        }
        if (pid == null) {
            pid = 0;
        }
        if (sortOrder == null) {
            sortOrder = 50;
        }

        if (id == null || id <= 0) {
            LitemallCategory category = new LitemallCategory();
            category.setName(name);
            category.setLevel(level);
            category.setPid(pid);
            category.setIconUrl(iconUrl != null ? iconUrl : "");
            category.setPicUrl(picUrl != null ? picUrl : "");
            category.setSortOrder(sortOrder.byteValue());
            category.setKeywords("");
            category.setDesc("");
            category.setAddTime(LocalDateTime.now());
            category.setUpdateTime(LocalDateTime.now());
            categoryService.add(category);
            return ResponseUtil.ok(category);
        } else {
            LitemallCategory category = categoryService.findById(id);
            if (category == null) {
                return ResponseUtil.badArgumentValue();
            }

            category.setName(name);
            category.setLevel(level);
            category.setPid(pid);
            if (iconUrl != null) category.setIconUrl(iconUrl);
            if (picUrl != null) category.setPicUrl(picUrl);
            category.setSortOrder(sortOrder.byteValue());
            category.setUpdateTime(LocalDateTime.now());
            categoryService.updateById(category);
            return ResponseUtil.ok(category);
        }
    }

    /**
     * 删除类目 (严格执行防误删与非空校验)
     */
    @PostMapping("/delete")
    public Object delete(@LoginUser Integer userId, @RequestBody String body) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        Integer id = JacksonUtil.parseInteger(body, "id");
        if (id == null) {
            return ResponseUtil.badArgument();
        }

        LitemallCategory category = categoryService.findById(id);
        if (category == null) {
            return ResponseUtil.badArgumentValue();
        }

        // 1. 如果是一级类目，检查是否有子类目
        if ("L1".equalsIgnoreCase(category.getLevel()) || category.getPid() == 0) {
            List<LitemallCategory> subCategories = categoryService.queryByPid(id);
            if (subCategories != null && !subCategories.isEmpty()) {
                return ResponseUtil.fail(608, "该类目下包含 " + subCategories.size() + " 个二级分类，请先移除或删除子分类");
            }
        }

        // 2. 检查是否有商品挂载在该类目下
        List<LitemallGoods> goodsList = goodsService.queryByCategory(id, 0, 5);
        if (goodsList != null && !goodsList.isEmpty()) {
            return ResponseUtil.fail(609, "该类目下仍有关联商品，请先将商品移至其他分类或下架后再删除");
        }

        categoryService.deleteById(id);
        return ResponseUtil.ok();
    }
}
