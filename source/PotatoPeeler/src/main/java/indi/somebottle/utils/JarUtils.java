package indi.somebottle.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 主要用于载入外部 jar 并执行
 */
public class JarUtils {
    /**
     * 在当前 JVM 中运行 jar 包
     *
     * @param jarPath jar 包路径
     * @param args    传给 main 方法的参数
     * @throws IOException IO 异常
     */
    public static void runJarInCurrentJVM(String jarPath, String[] args) throws IOException, NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        File jarFile = new File(jarPath);
        String mainClassName;
        try (JarFile jar = new JarFile(jarFile)) {
            // 开始寻找主类
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                // 清单没有找到
                throw new IOException("MANIFEST not found in jar file " + jarPath);
            }
            // 找到主类名
            mainClassName = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (mainClassName == null) {
                // 主类名没有找到
                throw new IOException("Main class not found in MANIFEST of jar file " + jarPath);
            }
        }
        // 开始载入类并运行
        URL jarUrl = jarFile.toURI().toURL();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl})) {
            // 载入主类
            Class<?> mainClass = classLoader.loadClass(mainClassName);
            // 获取 main 方法
            Method mainMethod = mainClass.getMethod("main", String[].class);
            // 调用 main 方法，开始执行服务端程序，顺带传入参数
            mainMethod.invoke(null, (Object) args);
        }
    }
}
