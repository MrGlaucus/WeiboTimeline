package com.glaucus.weibotimeline

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by GLAUCUS on 2017-08-27.
 */
class XposedModule : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == WEIBO_PACKAGE_NAME) {
            hookWeibo(lpparam)
        }
    }

    private fun hookWeibo(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("start hook weibo")
        reTimeline(lpparam)
    }

    fun reTimeline(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("start reset timeline")
        val FEED_LIST = "$WEIBO_PACKAGE_NAME.models.MBlogListBaseObject"
        val timelineHook = TimelineMethodHook()
        XposedHelpers.findAndHookMethod(FEED_LIST, lpparam.classLoader, "getStatuses", timelineHook)
        XposedHelpers.findAndHookMethod(FEED_LIST, lpparam.classLoader, "getStatusesCopy", timelineHook)

        val insertHook = NeedInsertMethod()
        XposedHelpers.findAndHookMethod(FEED_LIST, lpparam.classLoader, "getNeedInsert", insertHook)

        val sortHook = IfSortByTime()
        XposedHelpers.findAndHookMethod(FEED_LIST, lpparam.classLoader, "sortByTime", sortHook)

    }

    class NeedInsertMethod : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            var needInsert = param.result as Int
            log("need insert:$needInsert")
            needInsert = 0
            param.result = needInsert
        }
    }

    class IfSortByTime : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            log("called sortByTime")
        }
    }


    class TimelineMethodHook : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            //获取原本的timeline
            val origResult = param.result as ArrayList<*>
//            origResult.forEach {
//                //某条微博的ID
//                val id = XposedHelpers.getObjectField(it, "id") as String?
////                log("name:id,value:${id ?: "null"}")
//
//                //某条微博的创建日期**重点在此，将此排序即可
//                val created_at = XposedHelpers.getObjectField(it, "created_at") as String?
////                log("name:created_at,value:${created_at?.time()?.time?.format() ?: "null"}")
//
//                //微博内容
//                val text = XposedHelpers.getObjectField(it, "text") as String?
////                log("name:text,value:${text ?: "null"}")
//
//                //博主昵称
//                val user = XposedHelpers.getObjectField(it, "user")
//                val name = XposedHelpers.getObjectField(user, "name") as String?
////                log("name:name,value:${name ?: "null"}")
//            }

            //按时间排序
            origResult.sortByDescending {
                XposedHelpers.getObjectField(it, "created_at").toString().time()
            }

            //将重新排序的timelime返回
            param.result = origResult
        }
    }
}

fun log(message: String) {
    if (DEBUG) Log.d(MODULE_TAG, message)
}

fun String.time(): Date {
    return SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZ yyyy", Locale.US).parse(this)
}