package izumi.music_cloud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * 工厂模式获取ViewModel，适用于有参数的ViewModel
 */
class MainPageViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainPageViewModel() as T
    }
}
