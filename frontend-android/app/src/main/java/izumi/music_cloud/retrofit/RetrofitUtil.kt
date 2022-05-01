package izumi.music_cloud.retrofit

import izumi.music_cloud.global.GlobalConst
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitUtil {
    private val retrofit = Retrofit.Builder()
        .baseUrl(GlobalConst.BASE_URL) //获取全局BASEURL
        .addConverterFactory(GsonConverterFactory.create())//添加GSON解析器
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())//添加RXJava相关内容
        .build()

    fun getRetrofitService(): Retrofit = retrofit

}