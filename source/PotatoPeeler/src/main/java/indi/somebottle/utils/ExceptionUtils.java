package indi.somebottle.utils;

public class ExceptionUtils {
    public static void print(Exception e) {
        System.out.println("====== POTATO-PEELER EXCEPTION ======");
        System.out.println(e.getMessage());
    }

    public static void print(Exception e, String supplement) {
        System.out.println("====== POTATO-PEELER EXCEPTION ======");
        System.out.println(supplement + "\n" + e.getMessage());
    }
}
