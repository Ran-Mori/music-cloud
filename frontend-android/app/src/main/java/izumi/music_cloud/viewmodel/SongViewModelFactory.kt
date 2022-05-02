package izumi.music_cloud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * 工厂模式获取ViewModel，适用于有参数的ViewModel
 */
class SongViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SongViewModel() as T
    }
}
