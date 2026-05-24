package com.shanyangcode.infinitechat.authenticationservice.utils;

import java.util.Random;

public class RandomNumUtil {

    public static String getRandomNum(){
        Random random = new Random();

        int num = random.nextInt(900000) + 100000;
        return String.format("%06d", num);
    }
}