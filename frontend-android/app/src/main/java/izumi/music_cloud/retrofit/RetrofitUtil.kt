package izumi.music_cloud.retrofit

import izumi.music_cloud.global.GlobalConst
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitUtil {
    private val retrofit = Retrofit.Builder()
        .baseUrl(GlobalConst.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()

    fun getRetrofitService(): Retrofit = retrofit
}