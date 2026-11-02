package com.mushroom.camgalleryhook;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

/**
 * Redirects Pixel Camera's photo-review intent away from Google Photos.
 *
 * Google Camera launches the review screen with an explicit component
 * (com.google.android.apps.photos/.pager.HostPhotoPagerActivity), action
 * android.provider.action.REVIEW and a content://media URI. This module
 * hooks Instrumentation.execStartActivity inside the camera process and,
 * for intents addressed to Google Photos, strips the explicit component
 * and package and rewrites the action to ACTION_VIEW so any gallery app
 * can receive it through normal intent resolution.
 */
public class MainHook extends XposedModule {

    private static final String TAG = "CamGalleryHook";
    private static final String CAMERA_PKG = "com.google.android.GoogleCamera";
    private static final String PHOTOS_PKG = "com.google.android.apps.photos";
    private static final String ACTION_REVIEW = "android.provider.action.REVIEW";

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!CAMERA_PKG.equals(param.getPackageName())) {
            return;
        }
        try {
            Class<?> instrumentation = Class.forName(
                    "android.app.Instrumentation", false, param.getClassLoader());
            int hooked = 0;
            for (Method m : instrumentation.getDeclaredMethods()) {
                if (!"execStartActivity".equals(m.getName())) {
                    continue;
                }
                boolean hasIntent = false;
                for (Class<?> t : m.getParameterTypes()) {
                    if (t == Intent.class) {
                        hasIntent = true;
                        break;
                    }
                }
                if (!hasIntent) {
                    continue;
                }
                hook(m).intercept(new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        try {
                            for (Object arg : chain.getArgs()) {
                                if (arg instanceof Intent) {
                                    rewriteIfPhotos((Intent) arg);
                                }
                            }
                        } catch (Throwable t) {
                            log(Log.ERROR, TAG, "rewrite failed", t);
                        }
                        return chain.proceed();
                    }
                });
                hooked++;
            }
            log(Log.INFO, TAG, "hooked execStartActivity overloads: " + hooked);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "hook install failed", t);
        }
    }

    private void rewriteIfPhotos(Intent intent) {
        ComponentName comp = intent.getComponent();
        boolean toPhotos = (comp != null && PHOTOS_PKG.equals(comp.getPackageName()))
                || PHOTOS_PKG.equals(intent.getPackage());
        if (!toPhotos) {
            return;
        }
        String oldAction = intent.getAction();
        intent.setComponent(null);
        intent.setPackage(null);
        if (ACTION_REVIEW.equals(oldAction)) {
            intent.setAction(Intent.ACTION_VIEW);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        log(Log.INFO, TAG, "rewrote Photos intent -> action=" + intent.getAction()
                + " data=" + intent.getData());
    }
}
