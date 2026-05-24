package com.shanyangcode.infinitechat.authenticationservice.utils;

import java.util.Random;

public class NickNameGeneratorUtil {
    static String [] adjectives = {"粘人的", "聪明的", "可爱的", "勇敢的", "懒散的", "活泼的", "温柔的", "狡猾的", "快乐的", "笨拙的"};

    static String [] animals = {"猫", "狗", "兔子", "熊猫", "老虎", "狮子", "长颈鹿", "大象", "企鹅", "吗喽", "乌鸦", "牛马"};

    public static String generateNickName(){
        Random random = new Random();
        String adj = adjectives[random.nextInt(adjectives.length)];
        String a = animals[random.nextInt(animals.length)];

        return adj + a;
    }
}
