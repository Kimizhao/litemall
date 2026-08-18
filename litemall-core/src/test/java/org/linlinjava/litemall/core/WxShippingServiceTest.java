package org.linlinjava.litemall.core;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.linlinjava.litemall.core.wx.WxShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class WxShippingServiceTest {

    @Autowired
    private WxShippingService wxShippingService;

    @Test
    public void testConvertExpressCompany() {
        Assert.assertEquals("SF", wxShippingService.convertExpressCompany("SF"));
        Assert.assertEquals("SF", wxShippingService.convertExpressCompany("shunfeng"));
        Assert.assertEquals("ZTO", wxShippingService.convertExpressCompany("ZTO"));
        Assert.assertEquals("YTO", wxShippingService.convertExpressCompany("YTO"));
        Assert.assertEquals("YD", wxShippingService.convertExpressCompany("YD"));
        Assert.assertEquals("JTSD", wxShippingService.convertExpressCompany("JTSD"));
    }

    @Test
    public void testRfc3339DateFormat() {
        String uploadTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
        Assert.assertNotNull(uploadTime);
        Assert.assertTrue(uploadTime.contains("+08:00"));
        Assert.assertTrue(uploadTime.contains("T"));
    }
}
