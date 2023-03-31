package com.example.healthycamera.ui.plus

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.healthycamera.R
import com.example.healthycamera.data.Nutrients
import com.example.healthycamera.databinding.FragmentPlusBinding
import com.example.healthycamera.ml.MobilenetV210224Quant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.opencsv.CSVReader
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class PlusFragment : Fragment() {

    private var _binding: FragmentPlusBinding? = null
    lateinit var filePath: String

    val database = Firebase.database.reference

    var bitmaps: Bitmap? = null

    var launcher = registerForActivityResult(ActivityResultContracts.GetContent()) {
            it -> setGallery(uri = it)
    }

    var calorie by Delegates.notNull<Float>()
    var carbohydrate by Delegates.notNull<Float>()
    var protein by Delegates.notNull<Float>()
    var fat by Delegates.notNull<Float>()
    var sugar by Delegates.notNull<Float>()
    var sodium by Delegates.notNull<Float>()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 뷰모델 가져오기
        val homeViewModel =
            ViewModelProvider(this).get(PlusViewModel::class.java)

        _binding = FragmentPlusBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.plusTitle
        // 뷰모델에서 MutableLiveData인 text에 대한 Observer 추가
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        var labels = requireContext().applicationContext.assets.open("food_labels.txt").bufferedReader().readLines()

        var imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        // 유저 노드의 참조 가져오기
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        // 현재 유저의 ID에 해당하는 오늘 섭취한 영양소 데이터 가져오기
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val nutrientsRef = usersRef.child(userId!!).child("Nutrients")

        // 영양소 정보 읽기
        val nutrientsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                // Nutrients 데이터 가져오기
                val nutrients = dataSnapshot.getValue(Nutrients::class.java)
                // ...
                if (nutrients == null) {
                    calorie = 0.0.toFloat()
                    carbohydrate = 0.0.toFloat()
                    protein = 0.0.toFloat()
                    fat = 0.0.toFloat()
                    sugar = 0.0.toFloat()
                    sodium = 0.0.toFloat()
                } else {
                    calorie = nutrients?.calorie!!
                    carbohydrate = nutrients?.carbohydrate!!
                    protein = nutrients?.protein!!
                    fat = nutrients?.fat!!
                    sugar = nutrients?.sugar!!
                    sodium = nutrients?.sodium!!
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // 유저 가져오기 실패시 메시지 기록
                Log.w(ContentValues.TAG, "loadUser:onCancelled", databaseError.toException())
            }
        }
        nutrientsRef.addValueEventListener(nutrientsListener)

        // 초기 화면의 visibility 설정
        changeVisibility("none")

        // 사진 촬영 및 갤러리에서 사진 가져오기 선택 화면 띄우기
        binding.plusBtn.setOnClickListener {
            changeVisibility("click")
        }

        // 갤러리 버튼 클릭 시
        binding.galleryBtn.setOnClickListener {
            // 갤러리에서 이미지를 선택할 수 있도록 갤러리 앱을 띄운다.
            launcher.launch("image/*")
            // 이미지를 선택한 후의 화면으로 전환
            changeVisibility("after_click")
        }

        // 카메라 요청 실행
        val requestCameraFileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            // 이미지의 크기를 조정할 비율을 계산하고, 비율을 설정
            val calRatio = calculateInSampleSize(
                Uri.fromFile(File(filePath)),
                resources.getDimensionPixelSize(R.dimen.imgSize),
                resources.getDimensionPixelSize(R.dimen.imgSize)
            )
            val option = BitmapFactory.Options()
            option.inSampleSize = calRatio
            // 파일 경로에 있는 이미지 파일을 읽어들이고, 앞에서 계산한 비율 정보를 적용 후 생성된 비트맵 객체를 'bitmap' 변수에 할당
            val bitmap = BitmapFactory.decodeFile(filePath, option)
            bitmap?.let {
                // 비트맵 이미지 설정
                binding.foodimageview.setImageBitmap(bitmap)
                // 이미지 분류
                classifyImage(bitmap)
            }
        }

        // 카메라 버튼 클릭 시
        binding.cameraBtn.setOnClickListener {
            // 파일을 저장할 디렉토리 설정 후 카메라 앱 실행
            val timeStamp: String =
                SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
            filePath = file.absolutePath
            val photoURI: Uri = FileProvider.getUriForFile(
                requireContext(),
                "com.example.healthycamera.fileprovider",
                file
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

            requestCameraFileLauncher.launch(intent)
            //startActivityForResult(intent, 100)

            changeVisibility("after_click")
        }

        // 체크 버튼 클릭 시 해당 음식의 영양소를 오늘 섭취한 영양소에 추가
        binding.checkBtn.setOnClickListener {
            // 해당 음식의 영양소 (단위 제거)

            calorie += binding.caloriesValue.text.substring(0, binding.caloriesValue.text.length - 4).toFloat()
            carbohydrate += binding.carbohydrateValue.text.substring(0, binding.carbohydrateValue.text.length - 1).toFloat()
            protein += binding.proteinValue.text.substring(0, binding.proteinValue.text.length - 1).toFloat()
            fat += binding.fatValue.text.substring(0, binding.fatValue.text.length - 1).toFloat()
            sugar += binding.sugarsValue.text.substring(0, binding.sugarsValue.text.length - 1).toFloat()
            sodium += binding.sodiumValue.text.substring(0, binding.sodiumValue.text.length - 2).toFloat()

            // 영양소 객체 만들기 (소수점 2번째 자리까지 출력)
            val nutrients = Nutrients(((calorie * 100.0).roundToInt() / 100.0).toFloat(),
                ((carbohydrate * 100.0).roundToInt() / 100.0).toFloat(),
                ((protein * 100.0).roundToInt() / 100.0).toFloat(),
                ((fat * 100.0).roundToInt() / 100.0).toFloat(),
                ((sugar * 100.0).roundToInt() / 100.0).toFloat(),
                ((sodium * 100.0).roundToInt() / 100.0).toFloat())
            // Firebase Realtime DB에 유저 객체 저장
            database.child("users").child(userId!!).child("Nutrients").setValue(nutrients)
                .addOnCompleteListener {
                    // 섭취 영양소 추가 성공 메세지 출력
                    Toast.makeText(requireContext(), "Nutrients saved successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    // 섭취 영양소 추가 실패 메세지 출력
                    Toast.makeText(requireContext(), "Error saving nutrients.", Toast.LENGTH_SHORT).show()
                }

            // 이미지 선택 전 화면으로 전환
            changeVisibility("none")
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ui 전환 하는 함수
    private fun changeVisibility(mode: String){
        if (mode == "none") {
            binding.run {
                plusBtn.visibility = View.VISIBLE
                danger.visibility = View.GONE
                chooseBackground.visibility = View.GONE
                chooseBackground2.visibility = View.GONE
                foodimageview.visibility = View.GONE
                checkBtn.isEnabled = false
                result.setText("Food Name")
                caloriesValue.setText("0kcal")
                carbohydrateValue.setText("0g")
                proteinValue.setText("0g")
                fatValue.setText("0g")
                sugarsValue.setText("0g")
                sodiumValue.setText("0mg")
            }
        } else if (mode == "click") {
            binding.run {
                danger.visibility = View.VISIBLE
                chooseBackground.visibility = View.VISIBLE
                chooseBackground2.visibility = View.VISIBLE
            }
        } else if(mode == "after_click") {
            binding.run {
                plusBtn.visibility = View.INVISIBLE
                danger.visibility = View.GONE
                chooseBackground.visibility = View.GONE
                chooseBackground2.visibility = View.GONE
                foodimageview.visibility = View.VISIBLE
                checkBtn.isEnabled = true
            }
        }
    }

    // 사진 파일의 크기를 조정하기 위한 함수
    private fun calculateInSampleSize(fileUri: Uri, reqWidth: Int, reqHeight: Int): Int {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        try {
            var inputStream = requireActivity().contentResolver.openInputStream(fileUri)

            //inJustDecodeBounds 값을 true 로 설정한 상태에서 decodeXXX() 를 호출.
            //로딩 하고자 하는 이미지의 각종 정보가 options 에 설정 된다.
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream!!.close()
            inputStream = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //비율 계산........................
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        //inSampleSize 비율 계산
        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // 음식 분류 모델 함수
    private fun classifyImage(bitmaps: Bitmap?) {

        // 이미지 처리기 선언
        var imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224,224,ResizeOp.ResizeMethod.BILINEAR))
            .build()

        // 모델 초기화 및 레이블 읽어들임
        var labels = requireContext().applicationContext.assets.open("food_labels.txt").bufferedReader().readLines()

        // 이미지 처리
        var tensorImage = TensorImage(DataType.UINT8)
        tensorImage.load(bitmaps)

        tensorImage = imageProcessor.process(tensorImage)

        // 모델 실행
        val model = MobilenetV210224Quant.newInstance(requireContext())

        // 참조할 입력을 만듦
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.UINT8)
        inputFeature0.loadBuffer(tensorImage.buffer)

        // 모형 추론을 실행하고 결과를 가져옴
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

        // 가장 높은 확률의 결과를 화면에 출력
        var maxIdx = 0
        outputFeature0.forEachIndexed { index, fl ->
            if (outputFeature0[maxIdx] < fl) {
                maxIdx = index
            }
        }
        binding.result.setText(labels[maxIdx])

        // 해당 음식의 영양소 출력
        val data = readCsvData()
        binding.caloriesValue.setText(data[labels[maxIdx]]?.get(0).toString() + "kcal")
        binding.carbohydrateValue.setText(data[labels[maxIdx]]?.get(1).toString() + "g")
        binding.proteinValue.setText(data[labels[maxIdx]]?.get(2).toString() + "g")
        binding.fatValue.setText(data[labels[maxIdx]]?.get(3).toString() + "g")
        binding.sugarsValue.setText(data[labels[maxIdx]]?.get(4).toString() + "g")
        binding.sodiumValue.setText(data[labels[maxIdx]]?.get(5).toString() + "mg")

        // 모델 리소스를 더 이상 사용하지 않을 경우 해제
        model.close()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == AppCompatActivity.RESULT_OK) {
            bitmaps = data?.extras?.get("data") as Bitmap
            binding.foodimageview.setImageBitmap(bitmaps)
        }
    }

    fun setGallery(uri : Uri?) {
        bitmaps = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        binding.foodimageview.setImageBitmap(bitmaps)
        classifyImage(bitmaps)
    }

    fun readCsvData(): HashMap<String, List<Float>> {
        val inputStream: InputStream = resources.openRawResource(R.raw.food_nutrient)
        val reader = CSVReader(InputStreamReader(inputStream))
        val hashMap = HashMap<String, List<Float>>()
        var line: Array<String>?
        reader.readNext()
        while(reader.readNext().also { line = it } != null) {
            val dataList: MutableList<Float> = ArrayList()
            for (i in 1 until line!!.size) {
                dataList.add(line!![i].toFloat())
            }
            hashMap[line!![0]] = dataList
        }
        return hashMap
    }

}

