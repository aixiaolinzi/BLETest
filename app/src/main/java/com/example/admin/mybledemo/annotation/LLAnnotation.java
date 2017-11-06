package com.example.admin.mybledemo.annotation;

import android.app.Activity;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 自定义注解   替代findViewById
 * @author yzz
 * Created on 2017/11/6 16:25
 */

public class LLAnnotation {

    public static void viewInit(Activity activity){
        dealView(activity);
        dealOnClick(activity);
    }
    private static void dealView(Activity activity) {
        Class<?> cls = activity.getClass();
        Field[] fields = cls.getDeclaredFields();
        if(fields != null && fields.length > 0){
            for (Field field : fields){
                //获取字段的注解，如果没有ViewInit注解，则返回null
                ViewInit viewInit = field.getAnnotation(ViewInit.class);
                if(viewInit != null){
                    //获取字段注解的参数，这就是我们传进去控件ID
                    int viewId = viewInit.value();
                    if(viewId != -1){
                        try {
                            // 获取类中的findViewById方法，参数为int
                            Method method = cls.getMethod("findViewById",int.class);
                            //执行该方法，返回一个Object类型的View实例
                            Object resView = method.invoke(activity,viewId);
                            field.setAccessible(true);
                            //把字段的值设置为该View的实例
                            field.set(activity,resView);
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }catch (IllegalAccessException e){
                            e.printStackTrace();
                        }catch (InvocationTargetException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    private static void dealOnClick(Activity activity) {

        Class<?> cls=activity.getClass();
        Method[] declaredMethods = cls.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            OnClick onClick = declaredMethod.getAnnotation(OnClick.class);
            if (onClick!=null){
                int value = onClick.value();
                View view = activity.findViewById(value);

                if (view!=null){
                    view.setOnClickListener(new DeclaredOnClickListener(declaredMethod,activity));
                }
            }
        }

        
    }




    private static class DeclaredOnClickListener implements View.OnClickListener {
        private Method mMethod;
        private Object mHandlerType;

        public DeclaredOnClickListener(Method method, Object handlerType) {
            mMethod = method;
            mHandlerType = handlerType;
        }

        @Override
        public void onClick(View v) {
            // 4.反射执行方法
            mMethod.setAccessible(true);
            try {
                mMethod.invoke(mHandlerType, v);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    mMethod.invoke(mHandlerType, null);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
