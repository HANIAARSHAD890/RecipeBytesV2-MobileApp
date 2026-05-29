package com.example.recipebytes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.recipebytes.R
import com.example.recipebytes.models.OnboardingPage

class OnboardingFragment : Fragment() {

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_IMAGE = "image_res"
        private const val ARG_DESC  = "desc"

        fun newInstance(page: OnboardingPage): OnboardingFragment {
            return OnboardingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, page.titleText)
                    putInt(ARG_IMAGE, page.imageRes)
                    putString(ARG_DESC, page.descText)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_onboarding, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()

        view.findViewById<TextView>(R.id.tvTitle).text = args.getString(ARG_TITLE)

        view.findViewById<ImageView>(R.id.pageImage)
            .setImageResource(args.getInt(ARG_IMAGE))

        view.findViewById<TextView>(R.id.tvDesc).text = args.getString(ARG_DESC)
    }
}
