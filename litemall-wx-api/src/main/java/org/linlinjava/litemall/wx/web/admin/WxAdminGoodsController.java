package org.linlinjava.litemall.wx.web.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.pagehelper.PageHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.dao.LitemallGoodsMapper;
import org.linlinjava.litemall.db.domain.LitemallCategory;
import org.linlinjava.litemall.db.domain.LitemallGoods;
import org.linlinjava.litemall.db.domain.LitemallGoodsExample;
import org.linlinjava.litemall.db.domain.LitemallGoodsProduct;
import org.linlinjava.litemall.db.domain.LitemallGoodsSpecification;
import org.linlinjava.litemall.db.service.LitemallCategoryService;
import org.linlinjava.litemall.db.service.LitemallGoodsProductService;
import org.linlinjava.litemall.db.service.LitemallGoodsService;
import org.linlinjava.litemall.db.service.LitemallGoodsSpecificationService;
import org.linlinjava.litemall.wx.annotation.LoginUser;
import org.linlinjava.litemall.wx.service.WxAdminAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/wx/admin/goods")
@Validated
public class WxAdminGoodsController {
    private final Log logger = LogFactory.getLog(WxAdminGoodsController.class);

    @Autowired
    private WxAdminAuthService adminAuthService;

    @Autowired
    private LitemallGoodsService goodsService;

    @Autowired
    private LitemallGoodsMapper goodsMapper;

    @Autowired
    private LitemallGoodsProductService productService;

    @Autowired
    private LitemallGoodsSpecificationService specificationService;

    @Autowired
    private LitemallCategoryService categoryService;

    /**
     * 商品列表 (支持按类目、在售状态、关键词筛选，聚合总库存)
     */
    @GetMapping("/list")
    public Object list(@LoginUser Integer userId,
                       @RequestParam(required = false) String categoryId,
                       @RequestParam(required = false) Boolean isOnSale,
                       @RequestParam(required = false) String name,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "100") Integer limit) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        Integer catId = null;
        if (!StringUtils.isEmpty(categoryId) && !"null".equalsIgnoreCase(categoryId) && !"undefined".equalsIgnoreCase(categoryId)) {
            try {
                catId = Integer.parseInt(categoryId);
            } catch (NumberFormatException ignored) {}
        }

        Set<Integer> targetCategoryIds = null;
        if (catId != null) {
            targetCategoryIds = new HashSet<>();
            targetCategoryIds.add(catId);
            List<LitemallCategory> subCategories = categoryService.queryByPid(catId);
            if (subCategories != null) {
                for (LitemallCategory sub : subCategories) {
                    targetCategoryIds.add(sub.getId());
                }
            }
        }

        LitemallGoodsExample example = new LitemallGoodsExample();
        LitemallGoodsExample.Criteria criteria = example.createCriteria();
        criteria.andDeletedEqualTo(false);

        if (targetCategoryIds != null && !targetCategoryIds.isEmpty()) {
            if (targetCategoryIds.size() == 1) {
                criteria.andCategoryIdEqualTo(targetCategoryIds.iterator().next());
            } else {
                criteria.andCategoryIdIn(new ArrayList<>(targetCategoryIds));
            }
        }
        if (isOnSale != null) {
            criteria.andIsOnSaleEqualTo(isOnSale);
        }
        if (!StringUtils.isEmpty(name)) {
            criteria.andNameLike("%" + name + "%");
        }

        example.setOrderByClause("update_time desc, id desc");
        PageHelper.startPage(page, limit);
        List<LitemallGoods> goodsList = goodsMapper.selectByExample(example);

        // 批量提取 Goods IDs 以查询各规格库存
        List<Integer> gids = new ArrayList<>(goodsList.size());
        for (LitemallGoods g : goodsList) {
            gids.add(g.getId());
        }

        Map<Integer, Integer> stockMap = new HashMap<>();
        if (!gids.isEmpty()) {
            List<LitemallGoodsProduct> products = productService.queryByGids(gids);
            for (LitemallGoodsProduct p : products) {
                int curr = stockMap.getOrDefault(p.getGoodsId(), 0);
                stockMap.put(p.getGoodsId(), curr + (p.getNumber() != null ? p.getNumber() : 0));
            }
        }

        List<Map<String, Object>> voList = new ArrayList<>(goodsList.size());
        for (LitemallGoods goods : goodsList) {
            int totalStock = stockMap.getOrDefault(goods.getId(), 0);
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", goods.getId());
            vo.put("goodsSn", goods.getGoodsSn());
            vo.put("name", goods.getName());
            vo.put("categoryId", goods.getCategoryId());
            vo.put("picUrl", goods.getPicUrl());
            vo.put("brief", goods.getBrief());
            vo.put("isOnSale", goods.getIsOnSale());
            vo.put("sortOrder", goods.getSortOrder());
            vo.put("retailPrice", goods.getRetailPrice());
            vo.put("counterPrice", goods.getCounterPrice());
            vo.put("totalStock", totalStock);
            vo.put("isSoldOut", totalStock <= 0);
            voList.add(vo);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", voList);
        data.put("total", voList.size());
        return ResponseUtil.ok(data);
    }

    /**
     * 商品详情 (包含多规格货品列表，用于移动端编辑)
     */
    @GetMapping("/detail")
    public Object detail(@LoginUser Integer userId, @RequestParam Integer goodsId) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        LitemallGoods goods = goodsService.findById(goodsId);
        if (goods == null) {
            return ResponseUtil.badArgumentValue();
        }

        List<LitemallGoodsProduct> products = productService.queryByGid(goodsId);
        List<LitemallGoodsSpecification> specifications = specificationService.queryByGid(goodsId);

        int totalStock = 0;
        for (LitemallGoodsProduct p : products) {
            totalStock += (p.getNumber() != null ? p.getNumber() : 0);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("goods", goods);
        data.put("products", products);
        data.put("specifications", specifications);
        data.put("totalStock", totalStock);
        return ResponseUtil.ok(data);
    }

    /**
     * 快速改价改库与沽清/上下架
     */
    @PostMapping("/quick-update")
    public Object quickUpdate(@LoginUser Integer userId, @RequestBody String body) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        Integer goodsId = JacksonUtil.parseInteger(body, "goodsId");
        if (goodsId == null) {
            return ResponseUtil.badArgument();
        }

        LitemallGoods goods = goodsService.findById(goodsId);
        if (goods == null) {
            return ResponseUtil.badArgumentValue();
        }

        Boolean isOnSale = JacksonUtil.parseBoolean(body, "isOnSale");
        if (isOnSale != null) {
            goods.setIsOnSale(isOnSale);
        }

        BigDecimal retailPrice = null;
        String priceStr = JacksonUtil.parseString(body, "retailPrice");
        if (!StringUtils.isEmpty(priceStr)) {
            retailPrice = new BigDecimal(priceStr);
            goods.setRetailPrice(retailPrice);
        }

        String picUrl = JacksonUtil.parseString(body, "picUrl");
        if (picUrl != null) {
            goods.setPicUrl(picUrl);
            goods.setGallery(new String[]{picUrl});
        }

        // 更新规格货品列表的库存和价格
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(body);
            com.fasterxml.jackson.databind.JsonNode productsNode = rootNode.get("products");
            if (productsNode != null && productsNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode pNode : productsNode) {
                    if (pNode.has("id")) {
                        Integer pid = pNode.get("id").asInt();
                        LitemallGoodsProduct product = productService.findById(pid);
                        if (product != null) {
                            if (pNode.has("number")) {
                                product.setNumber(pNode.get("number").asInt());
                            }
                            if (pNode.has("price")) {
                                product.setPrice(new BigDecimal(pNode.get("price").asText()));
                            }
                            productService.updateById(product);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析 products 规格列表失败: " + e.getMessage(), e);
        }

        goods.setUpdateTime(LocalDateTime.now());
        goodsService.updateById(goods);
        return ResponseUtil.ok();
    }

    /**
     * 创建或保存商品 (支持标准单规格与多规格 SKU 配置)
     */
    @PostMapping("/save")
    @Transactional
    public Object save(@LoginUser Integer userId, @RequestBody String body) {
        if (!adminAuthService.isMerchantAdmin(userId)) {
            return ResponseUtil.unauthz();
        }

        Integer id = JacksonUtil.parseInteger(body, "id");
        String name = JacksonUtil.parseString(body, "name");
        Integer categoryId = JacksonUtil.parseInteger(body, "categoryId");
        String brief = JacksonUtil.parseString(body, "brief");
        String picUrl = JacksonUtil.parseString(body, "picUrl");
        String retailPriceStr = JacksonUtil.parseString(body, "retailPrice");
        String counterPriceStr = JacksonUtil.parseString(body, "counterPrice");
        Boolean isOnSale = JacksonUtil.parseBoolean(body, "isOnSale");
        Integer stock = JacksonUtil.parseInteger(body, "stock");
        String specType = JacksonUtil.parseString(body, "specType");

        if (StringUtils.isEmpty(name) || categoryId == null) {
            return ResponseUtil.badArgument();
        }

        BigDecimal retailPrice = !StringUtils.isEmpty(retailPriceStr) ? new BigDecimal(retailPriceStr) : BigDecimal.ZERO;
        BigDecimal counterPrice = !StringUtils.isEmpty(counterPriceStr) ? new BigDecimal(counterPriceStr) : retailPrice;
        if (isOnSale == null) isOnSale = true;
        if (stock == null) stock = 100;

        // 解析 products 规格货品列表
        List<Map<String, Object>> incomingProducts = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(body);
            com.fasterxml.jackson.databind.JsonNode productsNode = rootNode.get("products");
            if (productsNode != null && productsNode.isArray() && productsNode.size() > 0) {
                for (com.fasterxml.jackson.databind.JsonNode pNode : productsNode) {
                    Map<String, Object> pMap = new HashMap<>();
                    if (pNode.has("id") && !pNode.get("id").isNull()) {
                        pMap.put("id", pNode.get("id").asInt());
                    }
                    if (pNode.has("specifications") && pNode.get("specifications").isArray() && pNode.get("specifications").size() > 0) {
                        List<String> specs = new ArrayList<>();
                        for (com.fasterxml.jackson.databind.JsonNode sNode : pNode.get("specifications")) {
                            specs.add(sNode.asText());
                        }
                        pMap.put("specifications", specs.toArray(new String[0]));
                    } else if (pNode.has("specName") && !StringUtils.isEmpty(pNode.get("specName").asText())) {
                        pMap.put("specifications", new String[]{ pNode.get("specName").asText().trim() });
                    } else {
                        pMap.put("specifications", new String[]{ "标准" });
                    }

                    if (pNode.has("price") && !StringUtils.isEmpty(pNode.get("price").asText())) {
                        pMap.put("price", new BigDecimal(pNode.get("price").asText()));
                    } else {
                        pMap.put("price", retailPrice);
                    }

                    if (pNode.has("number")) {
                        pMap.put("number", pNode.get("number").asInt());
                    } else {
                        pMap.put("number", stock);
                    }
                    incomingProducts.add(pMap);
                }
            }
        } catch (Exception e) {
            logger.error("解析 products 列表异常: " + e.getMessage(), e);
        }

        // 如果未提供 products，根据单规格构建标准项
        if (incomingProducts.isEmpty()) {
            Map<String, Object> standardP = new HashMap<>();
            standardP.put("specifications", new String[]{"标准"});
            standardP.put("price", retailPrice);
            standardP.put("number", stock);
            incomingProducts.add(standardP);
            specType = "single";
        }

        // 计算所有 SKU 的最低售价作为 SPU 的起步售价 retailPrice
        BigDecimal minPrice = retailPrice;
        if (!incomingProducts.isEmpty()) {
            BigDecimal lowest = null;
            for (Map<String, Object> pMap : incomingProducts) {
                BigDecimal pPrice = (BigDecimal) pMap.get("price");
                if (pPrice != null) {
                    if (lowest == null || pPrice.compareTo(lowest) < 0) {
                        lowest = pPrice;
                    }
                }
            }
            if (lowest != null) {
                minPrice = lowest;
            }
        }
        retailPrice = minPrice;
        if (StringUtils.isEmpty(counterPriceStr)) {
            counterPrice = retailPrice;
        }

        String productUrl = (picUrl != null && picUrl.length() <= 125) ? picUrl : "";

        if (id == null || id <= 0) {
            // 新增商品
            LitemallGoods goods = new LitemallGoods();
            goods.setName(name);
            goods.setCategoryId(categoryId);
            goods.setBrief(brief != null ? brief : "");
            goods.setPicUrl(picUrl != null ? picUrl : "");
            goods.setGallery(new String[]{picUrl != null ? picUrl : ""});
            goods.setRetailPrice(retailPrice);
            goods.setCounterPrice(counterPrice);
            goods.setIsOnSale(isOnSale);
            goods.setSortOrder((short) 100);
            goods.setGoodsSn("XM-" + (System.currentTimeMillis() % 1000000));
            goods.setUnit("份");
            goods.setIsNew(true);
            goods.setIsHot(false);
            goods.setAddTime(LocalDateTime.now());
            goods.setUpdateTime(LocalDateTime.now());
            goodsService.add(goods);

            // 写入规格定义表 (LitemallGoodsSpecification)
            Set<String> addedSpecValues = new HashSet<>();
            for (Map<String, Object> pMap : incomingProducts) {
                String[] specs = (String[]) pMap.get("specifications");
                if (specs != null) {
                    for (String val : specs) {
                        if (!addedSpecValues.contains(val)) {
                            LitemallGoodsSpecification spec = new LitemallGoodsSpecification();
                            spec.setGoodsId(goods.getId());
                            spec.setSpecification("规格");
                            spec.setValue(val);
                            spec.setPicUrl("");
                            spec.setAddTime(LocalDateTime.now());
                            spec.setUpdateTime(LocalDateTime.now());
                            specificationService.add(spec);
                            addedSpecValues.add(val);
                        }
                    }
                }
            }

            // 写入货品表 (LitemallGoodsProduct)
            for (Map<String, Object> pMap : incomingProducts) {
                LitemallGoodsProduct product = new LitemallGoodsProduct();
                product.setGoodsId(goods.getId());
                product.setSpecifications((String[]) pMap.get("specifications"));
                product.setPrice((BigDecimal) pMap.get("price"));
                product.setNumber((Integer) pMap.get("number"));
                product.setUrl(productUrl);
                product.setAddTime(LocalDateTime.now());
                product.setUpdateTime(LocalDateTime.now());
                productService.add(product);
            }

            return ResponseUtil.ok(goods);
        } else {
            // 更新商品
            LitemallGoods goods = goodsService.findById(id);
            if (goods == null) {
                return ResponseUtil.badArgumentValue();
            }

            goods.setName(name);
            goods.setCategoryId(categoryId);
            if (brief != null) goods.setBrief(brief);
            if (picUrl != null) {
                goods.setPicUrl(picUrl);
                goods.setGallery(new String[]{picUrl});
            }
            goods.setRetailPrice(retailPrice);
            goods.setCounterPrice(counterPrice);
            goods.setIsOnSale(isOnSale);
            goods.setUpdateTime(LocalDateTime.now());
            goodsService.updateById(goods);

            // 重新同步规格表
            specificationService.deleteByGid(id);
            Set<String> addedSpecValues = new HashSet<>();
            for (Map<String, Object> pMap : incomingProducts) {
                String[] specs = (String[]) pMap.get("specifications");
                if (specs != null) {
                    for (String val : specs) {
                        if (!addedSpecValues.contains(val)) {
                            LitemallGoodsSpecification spec = new LitemallGoodsSpecification();
                            spec.setGoodsId(goods.getId());
                            spec.setSpecification("规格");
                            spec.setValue(val);
                            spec.setPicUrl("");
                            spec.setAddTime(LocalDateTime.now());
                            spec.setUpdateTime(LocalDateTime.now());
                            specificationService.add(spec);
                            addedSpecValues.add(val);
                        }
                    }
                }
            }

            // 同步货品表 (更新已存在的，新增新建的，删除被移除的)
            List<LitemallGoodsProduct> existingProducts = productService.queryByGid(id);
            Set<Integer> keptProductIds = new HashSet<>();

            for (Map<String, Object> pMap : incomingProducts) {
                Integer pid = (Integer) pMap.get("id");
                if (pid != null && pid > 0) {
                    LitemallGoodsProduct product = productService.findById(pid);
                    if (product != null && product.getGoodsId().equals(id)) {
                        product.setSpecifications((String[]) pMap.get("specifications"));
                        product.setPrice((BigDecimal) pMap.get("price"));
                        product.setNumber((Integer) pMap.get("number"));
                        if (!StringUtils.isEmpty(productUrl)) {
                            product.setUrl(productUrl);
                        }
                        product.setUpdateTime(LocalDateTime.now());
                        productService.updateById(product);
                        keptProductIds.add(pid);
                        continue;
                    }
                }

                // 新增规格项
                LitemallGoodsProduct product = new LitemallGoodsProduct();
                product.setGoodsId(goods.getId());
                product.setSpecifications((String[]) pMap.get("specifications"));
                product.setPrice((BigDecimal) pMap.get("price"));
                product.setNumber((Integer) pMap.get("number"));
                product.setUrl(productUrl);
                product.setAddTime(LocalDateTime.now());
                product.setUpdateTime(LocalDateTime.now());
                productService.add(product);
            }

            // 删除未被保留的旧货品
            for (LitemallGoodsProduct oldP : existingProducts) {
                if (!keptProductIds.contains(oldP.getId())) {
                    productService.deleteById(oldP.getId());
                }
            }

            return ResponseUtil.ok(goods);
        }
    }

    /**
     * 删除商品 (逻辑删除)
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

        goodsService.deleteById(id);
        productService.deleteByGid(id);
        specificationService.deleteByGid(id);
        return ResponseUtil.ok();
    }
}
