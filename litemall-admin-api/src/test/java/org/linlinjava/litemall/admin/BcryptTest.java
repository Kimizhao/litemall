package org.linlinjava.litemall.admin;

import org.junit.Assert;
import org.junit.Test;
import org.linlinjava.litemall.core.util.bcrypt.BCryptPasswordEncoder;

public class BcryptTest {

    @Test
    public void test() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String passMall = "mall123@Abc";
        String hashMall = encoder.encode(passMall);
        String passPromo = "promotion123@Abc";
        String hashPromo = encoder.encode(passPromo);

        System.out.println("MALL_HASH=" + hashMall);
        System.out.println("PROMO_HASH=" + hashPromo);

        Assert.assertTrue(encoder.matches(passMall, hashMall));
        Assert.assertTrue(encoder.matches(passPromo, hashPromo));
    }
}
