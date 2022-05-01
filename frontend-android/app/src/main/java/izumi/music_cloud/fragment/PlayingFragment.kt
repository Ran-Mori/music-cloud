package izumi.music_cloud.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import izumi.music_cloud.R

class PlayingFragment : Fragment() {

    companion object {
        const val TAG = "playing_fragment"

        @JvmStatic
        fun newInstance() = PlayingFragment()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.statusBarColor = resources.getColor(R.color.white, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_playing, container, false)
    }


}