package com.example.healthycamera.ui.home

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.healthycamera.data.Nutrients
import com.example.healthycamera.data.User
import com.example.healthycamera.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    //private var _binding: FragmentHomeBinding? = null

    private lateinit var databaseRef: DatabaseReference

    private lateinit var textViews: Array<TextView>
    private lateinit var progressBars: Array<ProgressBar>

    // This property is only valid between onCreateView and
    // onDestroyView.
    //private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        //_binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.homeTitle
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // 유저 노드의 참조 가져오기
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        // 현재 유저의 ID에 해당하는 오늘 섭취한 영양소 데이터 가져오기
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        //val nutrientsRef = usersRef.child(userId!!).child("Nutrients")
        if (userId != null) {
            val nutrientsRef = usersRef.child(userId).child("Nutrients")
            // 영양소 정보 읽기
            val nutrientsListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    // Nutrients 데이터 가져오기
                    val nutrients = dataSnapshot.getValue(Nutrients::class.java)
                    // ...
                    if (nutrients == null) {
                        binding.sCalories.setText("0.0")
                        binding.sCarbohydrate.setText("0.0")
                        binding.sProtein.setText("0.0")
                        binding.sFat.setText("0.0")
                        binding.sSugars.setText("0.0")
                        binding.sSodium.setText("0.0")
                    } else {
                        binding.sCalories.setText(nutrients.calorie!!.toString())
                        binding.sCarbohydrate.setText(nutrients.carbohydrate!!.toString())
                        binding.sProtein.setText(nutrients.protein!!.toString())
                        binding.sFat.setText(nutrients.fat!!.toString())
                        binding.sSugars.setText(nutrients.sugar!!.toString())
                        binding.sSodium.setText(nutrients.sodium!!.toString())
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // 유저 가져오기 실패시 메시지 기록
                    Log.w(ContentValues.TAG, "loadUser:onCancelled", databaseError.toException())
                }
            }
            nutrientsRef.addValueEventListener(nutrientsListener)
            // do something with nutrientsRef
        } else {
            // handle the case where userId is null
        }


/*        // 영양소 정보 읽기
        val nutrientsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                // Nutrients 데이터 가져오기
                val nutrients = dataSnapshot.getValue(Nutrients::class.java)
                // ...
                if (nutrients == null) {
                    binding.sCalories.setText("0.0")
                    binding.sCarbohydrate.setText("0.0")
                    binding.sProtein.setText("0.0")
                    binding.sFat.setText("0.0")
                    binding.sSugars.setText("0.0")
                    binding.sSodium.setText("0.0")
                } else {
                    binding.sCalories.setText(nutrients.calorie!!.toString())
                    binding.sCarbohydrate.setText(nutrients.carbohydrate!!.toString())
                    binding.sProtein.setText(nutrients.protein!!.toString())
                    binding.sFat.setText(nutrients.fat!!.toString())
                    binding.sSugars.setText(nutrients.sugar!!.toString())
                    binding.sSodium.setText(nutrients.sodium!!.toString())
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // 유저 가져오기 실패시 메시지 기록
                Log.w(ContentValues.TAG, "loadUser:onCancelled", databaseError.toException())
            }
        }
        nutrientsRef.addValueEventListener(nutrientsListener)*/

        // 유저의 정보 가져오기
        databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId")
//++++
        // 유저의 정보 읽기
        val userListener = object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // User 개체 가져오기 및 값을 사용하여 하루 기준 섭취량 계산
                val user = dataSnapshot.getValue<User>() ?: return
                // ...
                val age: Int? = user.age ?: return
                val gender = user.gender ?: return
                val height = user.height ?: return
                val weight = user.weight ?: return
                var pa = 0.0

                when (user.pa) {
                    "Inactive" -> {
                        // 비활동적일 때 실행할 코드
                        pa = 1.0
                    }
                    "Underactive"-> {
                        // 저활동적일 때 실행할 코드
                        pa = if (gender == "Man") {
                            1.11
                        } else {
                            1.12
                        }
                    }
                    "Active" -> {
                        // 활동적일 때 실행할 코드
                        pa = if (gender == "Man") {
                            1.25
                        } else {
                            1.27
                        }
                    }
                    "Very Active" -> {
                        // 매우 활동적일 때 실행할 코드
                        pa = if (gender == "Man") {
                            1.48
                        } else {
                            1.45
                        }
                    }
                    else -> {
                        // 아무것도 선택하지 않았거나, 다른 값을 선택한 경우 실행할 코드
                        pa = 0.0
                    }
                }

                // 하루 열량 섭취량 계산
                var kcal = 0.0
                kcal = when (gender) {
                    "Man" -> {
                        662 - 9.53 * age!!.toFloat() + pa * (15.91 * weight.toFloat() + 539.6 * (height / 100))
                    }
                    "Woman" -> {
                        354 - 6.91 * age!!.toFloat() + pa * (9.36 * weight.toFloat() + 726 * (height / 100))
                    }
                    else -> {
                        0.0
                    }
                }
                kcal = (kcal * 100.0).roundToInt() / 100.0

                // 하루 탄수화물 섭취량 계산
                val carbo = (kcal * 0.53 / 4.0 * 100.0).roundToInt() / 100.0
                var pro = 0.0
                // 하루 단백질 섭취량 계산
                pro = (weight * 0.8 * 100.0).roundToInt() / 100.0

                // 하루 지방 섭취량 계산
                val fat = (kcal * 0.27 / 9.0 * 100.0).roundToInt() / 100.0

                // 오늘 섭취한 영양소 퍼센트
                val p_cal = (binding.sCalories.text.toString().toFloat() / kcal * 1000.0).roundToInt() / 10.0
                val p_car = (binding.sCarbohydrate.text.toString().toFloat() / carbo * 1000.0).roundToInt() / 10.0
                var p_pro = 0.0
                if (pro != 0.0) {
                    p_pro = (binding.sProtein.text.toString().toFloat() / pro * 1000.0).roundToInt() / 10.0
                }
                val p_fat = (binding.sFat.text.toString().toFloat() / fat * 1000.0).roundToInt() / 10.0
                val p_sug = (binding.sSugars.text.toString().toFloat() / 50.0 * 1000.0).roundToInt() / 10.0
                val p_sod = (binding.sSodium.text.toString().toFloat() / 2000.0 * 1000.0).roundToInt() / 10.0

                binding.kcal.setText("/" + kcal + "kcal")
                binding.g1.setText("/" + carbo + "g")
                binding.g2.setText("/" + pro + "g")
                binding.g3.setText("/" + fat + "g")
                binding.g4.setText("/50g")
                binding.mg.setText("/2000mg")
                binding.pCalories.setText("$p_cal%")
                binding.pCarbohydrate.setText("$p_car%")
                binding.pProtein.setText("$p_pro%")
                binding.pFat.setText("$p_fat%")
                binding.pSugar.setText("$p_sug%")
                binding.pSodium.setText("$p_sod%")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // 유저 가져오기 실패시 메시지 기록
                Log.w(ContentValues.TAG, "loadUser:onCancelled", databaseError.toException())
            }
        }
        databaseRef.addValueEventListener(userListener)
//++++
        // 오늘 섭취한 영양소 프로그래스바로 표현하는 부분
        textViews = arrayOf(
            binding.pCalories,
            binding.pCarbohydrate,
            binding.pProtein,
            binding.pFat,
            binding.pSugar,
            binding.pSodium
        )
        progressBars = arrayOf(
            binding.caloriesBar,
            binding.carbohydrateBar,
            binding.proteinBar,
            binding.fatBar,
            binding.sugarsBar,
            binding.sodiumBar
        )

        textViews.forEach { it.addTextChangedListener(textWatcher) }

        return root
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Not used
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            try {
                val progressText = s?.toString()?.substringBefore("%") ?: "0"
                val progressValue = progressText.toFloatOrNull() ?: 0f

                val index = when (s) {
                    textViews[0].text -> 0
                    textViews[1].text -> 1
                    textViews[2].text -> 2
                    textViews[3].text -> 3
                    textViews[4].text -> 4
                    textViews[5].text -> 5
                    else -> return
                }

                progressBars[index].progress = progressValue.toInt()
            } catch (e: NumberFormatException) {
                // Ignore invalid input
            }
        }

        override fun afterTextChanged(s: Editable?) {

        }
    }


/*    override fun onDestroyView() {
        super.onDestroyView()
        //binding = null
        _binding = null
    }*/



}