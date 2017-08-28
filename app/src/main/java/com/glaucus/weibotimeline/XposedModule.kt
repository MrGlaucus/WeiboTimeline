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

    fun hookWeibo(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("start hook weibo")
        reTimeline(lpparam)
    }

    fun reTimeline(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("start reset timeline")
        val FEED_LIST = "$WEIBO_PACKAGE_NAME.models.MBlogListBaseObject"
        val hook = TimelineMetohdHook()
        XposedHelpers.findAndHookMethod(FEED_LIST, lpparam.classLoader, "getStatuses", hook)
        XposedHelpers.findAndHookMethod(FEED_LIST, lpparam.classLoader, "getStatusesCopy", hook)
    }

    class TimelineMetohdHook : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            //获取原本的timeline
            val origResult = param.result as ArrayList<*>
            origResult.forEach {
                //某条微博的ID
                val id = XposedHelpers.getObjectField(it, "id") as String?
                log("name:id,value:${id ?: "null"}")

                //某条微博的创建日期**重点在此，将此排序即可
                val created_at = XposedHelpers.getObjectField(it, "created_at") as String?
                log("name:created_at,value:${created_at?.time()?.time?.format() ?: "null"}")

                //微博内容
                val text = XposedHelpers.getObjectField(it, "text") as String?
                log("name:text,value:${text ?: "null"}")

                //博主昵称
                val user = XposedHelpers.getObjectField(it, "user")
                val name = XposedHelpers.getObjectField(user, "name") as String?
                log("name:name,value:${name ?: "null"}")
            }

            //按时间排序
            origResult.sortBy {
                XposedHelpers.getObjectField(it, "created_at").toString().time()
            }

            //再倒一下序
            origResult.reverse()

            //将重新排序的timelime返回
            param.result = origResult
        }
    }
}

fun log(message: String) {
    if (DEBUG) Log.d(MODULE_TAG, message)
}

//扩展方法，将时间毫秒数转换成可读字符串
fun Long.format(): String {
    return SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(this)
}

//扩展方法，将微博的时间格式解析成可排序的Date
fun String.time(): Date {
    return SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZ yyyy", Locale.US).parse(this)
}