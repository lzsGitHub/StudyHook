package com.pack.hookapplication;

import android.content.Context;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;


public class LoadUtil {
    private static final String pluginApkPath = "/sdcard/pluginmodule-debug.apk";

    public static void load(Context context) {
        try {
            //获取pathList字段 |||注意这里反射只是定义一个规则
            Class baseDexClassLoader = Class.forName("dalvik.system.BaseDexClassLoader");
            Field pathListField = baseDexClassLoader.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            /**
             * 获取插件的 dexElement[]
             *  同获取DexClassLoader类中的属性pathList的值
             */
            //第一步创建DexClassLoader
            DexClassLoader dexClassLoader = new DexClassLoader(pluginApkPath,context.getCacheDir().getAbsolutePath(),null,
                    context.getClassLoader());
            Object pluginPathList = pathListField.get(dexClassLoader);//这里才是反射的具体处理，通过规则获取dexClassLoader内的pathList值
            //获取pahtList 中的属性 dexElements[] 的值---插件的 dexElement[]
            Class pluginPathListClass =  pluginPathList.getClass();
            Field pluginDexElementsField = pluginPathListClass.getDeclaredField("dexElements");
            pluginDexElementsField.setAccessible(true);
            Object[] pluginElements = (Object[]) pluginDexElementsField.get(pluginPathList);
            /**
             * 获取宿主的dexElement[],也就是应用本身的这里的ClassLoader用PathClassLoader
             */
            PathClassLoader pathClassLoader = (PathClassLoader) context.getClassLoader();
            Object hostPathList = pathListField.get(pathClassLoader);
            //通过pathList属性定义获取Elements规则
            Class hostPathListClass = hostPathList.getClass();
            Field hostDexElementsField = hostPathListClass.getDeclaredField("dexElements");
            hostDexElementsField.setAccessible(true);
            //获取到
            Object[] hostElements = (Object[]) hostDexElementsField.get(hostPathList);
            /**
             * 将插件和宿主的dexElements合并放入一个新的数组中
             */
            //第一步创建一个新的数组
            Object[] newElements = (Object[]) Array.newInstance(hostElements.getClass().getComponentType(),
                    pluginElements.length+hostElements.length);
            //合并操作 这里注意的是我们目的是有同样名字的文件要优先加载插件 所以插件要放在宿主的Element之前
            System.arraycopy(pluginElements,0,newElements,0,pluginElements.length);
            System.arraycopy(hostElements,0,newElements,pluginElements.length,hostElements.length);
            /**
             * 将新的Element数组赋值给dexElements属性
             */
            hostDexElementsField.set(hostPathList,newElements);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
