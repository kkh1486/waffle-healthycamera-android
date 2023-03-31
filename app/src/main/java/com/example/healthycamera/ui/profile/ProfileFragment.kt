package com.example.healthycamera.ui.profile

import android.R
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.healthycamera.AuthActivity
import com.example.healthycamera.data.User
import com.example.healthycamera.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import kotlin.math.roundToInt

class ProfileFragment : Fragment() {
    lateinit var binding: FragmentProfileBinding
    //private var _binding: FragmentProfileBinding? = null

    private lateinit var databaseRef: DatabaseReference

    // This property is only valid between onCreateView and
    // onDestroyView.
    //private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(ProfileViewModel::class.java)

        binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.profileTitle

        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        binding.plogoutBtn.setOnClickListener {
            val intent = Intent(getActivity(), AuthActivity::class.java)
            startActivity(intent)
        }

        // 현재 유저의 ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // 유저의 정보 가져오기
        databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId")

        // 유저의 정보 읽기
        val userListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // User 개체 가져오기 및 값을 사용하여 UI 업데이트
                val user = dataSnapshot.getValue<User>()
                // ...
                if (user == null) {
                    binding.nicknameText.setText("Nickname")
                    binding.ageText.setText("0")
                    binding.genderText.setText("Gender")
                    binding.heightText.setText("0.0")
                    binding.weightText.setText("0.0")
                    binding.physicalActivityText.setText("Physical Activity")
                } else {
                    binding.nicknameText.setText(user?.nickname)
                    binding.ageText.setText(user?.age.toString())
                    binding.genderText.setText(user?.gender)
                    binding.heightText.setText(user?.height.toString())
                    binding.weightText.setText(user?.weight.toString())
                    binding.physicalActivityText.setText(user?.pa)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // 유저 가져오기 실패시 메시지 기록
                Log.w(TAG, "loadUser:onCancelled", databaseError.toException())
            }
        }
        databaseRef.addValueEventListener(userListener)

        // 성별 선택하는 스피너
        var selectedGender: String = ""
        val genders = arrayOf("Man", "Woman")

        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, genders)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.gg.adapter = adapter

        binding.gg.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 선택한 성별을 저장
                selectedGender = genders[position].toString()
                when (selectedGender) {
                    "Man" -> {
                        // 남성일 때 실행할 코드
                        binding.genderText.setText("Man")
                    }
                    "Woman" -> {
                        // 여성일 때 실행할 코드
                        binding.genderText.setText("Woman")
                    }
                    else -> {
                        // 아무것도 선택하지 않았거나, 다른 값을 선택한 경우 실행할 코드
                        Toast.makeText(requireContext(), "Please choose Man or Woman", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 아무것도 선택하지 않은 경우, selectedGender는 빈 문자열로 유지됨
            }
        }
        //-------------------

        // 신체활동도(PA) 선택하는 스피너
        var selectedPA: String = ""
        val pa = arrayOf("Inactive", "Underactive", "Active", "Very Active")

        val adapter_pa = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, pa)
        adapter_pa.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.pp.adapter = adapter_pa

        binding.pp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 선택한 신체활동도 저장
                selectedPA = pa[position].toString()
                when (selectedPA) {
                    "Inactive" -> {
                        // 비활동적일 때 실행할 코드
                        binding.physicalActivityText.setText("Inactive")
                    }
                    "Underactive" -> {
                        // 저활동적일 때 실행할 코드
                        binding.physicalActivityText.setText("Underactive")
                    }
                    "Active" -> {
                        // 활동적일 때 실행할 코드
                        binding.physicalActivityText.setText("Active")
                    }
                    "Very Active" -> {
                        // 매우 활동적일 때 실행할 코드
                        binding.physicalActivityText.setText("Very Active")
                    }
                    else -> {
                        // 아무것도 선택하지 않았거나, 다른 값을 선택한 경우 실행할 코드
                        Toast.makeText(requireContext(), "Please choose PA", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 아무것도 선택하지 않은 경우, selectedPA는 빈 문자열로 유지됨
            }
        }
        //-------------------

        changeTouchMode("false")
        changeVisibility("edit_after")

        // 수정 버튼 클릭 시
        binding.edit.setOnClickListener {
            // EditText 수정 가능 모드
            changeTouchMode("true")

            // 수정 & 로그아웃 버튼 -> 저장 & 취소 버튼 UI 바꾸기
            changeVisibility("edit_before")
        }

        // 저장 버튼 클릭 시
        binding.save.setOnClickListener {
            // 유저 정보 수정
            val nickname = binding.nicknameText.text.toString().trim()
            val age = binding.ageText.text.toString().trim().toInt()
            val gender = binding.genderText.text.toString().trim()
            val height = ((binding.heightText.text.toString().trim().toFloat() * 10.0).roundToInt() / 10.0).toFloat()
            val weight = ((binding.weightText.text.toString().trim().toFloat() * 10.0).roundToInt() / 10.0).toFloat()
            val pa = binding.physicalActivityText.text.toString().trim()

            // 유저 객체 만들기
            val user = User(nickname, age, gender, height, weight, pa)

            // EditText 수정 불가능 모드
            changeTouchMode("false")

            // Firebase Realtime DB에 유저 객체 저장
            databaseRef.setValue(user)
                .addOnCompleteListener {
                    // 유저 정보 저장 성공 메세지 출력
                    Toast.makeText(requireContext(), "User information saved successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    // 유저 정보 저장 실패 메세지 출력
                    Toast.makeText(requireContext(), "Error saving user information.", Toast.LENGTH_SHORT).show()
                }

            // 저장 & 취소 버튼 -> 수정 & 로그아웃 버튼 UI 바꾸기
            changeVisibility("edit_after")
        }

        return root
    }

/*    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }*/

    private fun changeTouchMode(mode: String){
        if (mode == "true") {
            binding.run {
                binding.nicknameText.isFocusableInTouchMode = true
                binding.ageText.isFocusableInTouchMode = true
                binding.genderText.visibility = View.INVISIBLE
                binding.gg.visibility = View.VISIBLE
                binding.heightText.isFocusableInTouchMode = true
                binding.weightText.isFocusableInTouchMode = true
                binding.physicalActivityText.visibility = View.INVISIBLE
                binding.pp.visibility = View.VISIBLE
            }
        } else if (mode == "false") {
            binding.run {
                binding.nicknameText.isFocusable = false
                binding.ageText.isFocusable = false
                binding.genderText.isFocusable = false
                binding.genderText.visibility = View.VISIBLE
                binding.gg.visibility = View.GONE
                binding.heightText.isFocusable = false
                binding.weightText.isFocusable = false
                binding.physicalActivityText.isFocusable = false
                binding.physicalActivityText.visibility = View.VISIBLE
                binding.pp.visibility = View.GONE
                binding.nicknameText.isFocusableInTouchMode = false
                binding.ageText.isFocusableInTouchMode = false
                binding.genderText.isFocusableInTouchMode = false
                binding.heightText.isFocusableInTouchMode = false
                binding.weightText.isFocusableInTouchMode = false
                binding.physicalActivityText.isFocusableInTouchMode = false
            }
        }
    }

    // ui 전환 하는 함수
    private fun changeVisibility(mode: String){
        if (mode == "edit_before") {
            binding.run {
                binding.save.visibility = View.VISIBLE
                binding.edit.visibility = View.GONE
            }
        } else if (mode == "edit_after") {
            binding.run {
                binding.save.visibility = View.GONE
                binding.edit.visibility = View.VISIBLE
            }
        }
    }

}