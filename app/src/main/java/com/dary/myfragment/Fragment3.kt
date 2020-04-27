package com.dary.myfragment

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_3.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Fragment3 : Fragment(){

    private val PERMISSION_REQUEST_CAMERA=90
    private val PERMISSION_REQUEST_GALLERY=87

    val permissions= arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE

    )

    private var photoFile:File?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addPhoto.setOnClickListener{
            requestMyPermission(PERMISSION_REQUEST_CAMERA)
        }

        addGallery.setOnClickListener{
            requestMyPermission(PERMISSION_REQUEST_GALLERY)

        }
    }

    private fun requestMyPermission(requestCode:Int){

        requestPermissions(permissions,requestCode)
    }


    //En este metodo si dan permisos se ejecuta y sino sale un toast
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_REQUEST_CAMERA->{
                if(grantResults.size==permissions.size && grantResults.count{it==PackageManager.PERMISSION_GRANTED}==permissions.size){
                    startActivityForResult(Intent().apply{
                        type="image/*"      //soporta todas el tipo de imagen
                        action=Intent.ACTION_GET_CONTENT    //Abre todas la imagenes
                    },PERMISSION_REQUEST_CAMERA)    //se puede enviar este mismo PERMISSION_REQUEST_CAMERA o crear otras 2 variables
                }else{
                    Toast.makeText(context,"permisos denegados",Toast.LENGTH_LONG).show()
                }
            }
            PERMISSION_REQUEST_CAMERA->{
                if(grantResults.size==permissions.size && grantResults.count{it==PackageManager.PERMISSION_GRANTED}==permissions.size){
                   openCamera()
                }else{
                    Toast.makeText(context,"permisos denegados",Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun openCamera() {
        val photoIntent=Intent(MediaStore.ACTION_IMAGE_CAPTURE)  //se necesita crear una vble global para que guarde
        if(photoIntent.resolveActivity(activity!!.packageManager) !=null){
                try{
                    //se llama al metodod
                    photoFile=createPhotoFile()
                }catch(ioException:IOException){
                    //mostrar error
                }

            if(photoFile!=null){
                val photoUri:Uri=FileProvider.getUriForFile(context!!,"com.dary",photoFile!!)
                val resultIntent = activity!!.packageManager.queryIntentActivities(
                    photoIntent,PackageManager.MATCH_DEFAULT_ONLY
                )
                for(i in resultIntent){
                    val packageName=i.activityInfo.packageName
                    activity!!.grantUriPermission(
                        packageName,photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(photoIntent,PERMISSION_REQUEST_CAMERA)

            }

        }
    }

    //crear archivo y tiene que tener un identificador fija
    private fun createPhotoFile(): File? {
        val imageFileName:String="myPhotos${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}"
        //para alojar esto
        val storageDir:File?=activity!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        //el return puede ir tambien en cada linea del if
        return if(storageDir?.exists()==false){
            val result = storageDir.mkdir()
             null
        }else{
             //recibe archivo, sufijo y directorio
             File.createTempFile(imageFileName,".jpg",storageDir)

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== Activity.RESULT_OK){
            when(requestCode){
                PERMISSION_REQUEST_GALLERY->{
                    data?.let{
                        Picasso.get().load(data.data).into(gallery)
                    }
                }

                PERMISSION_REQUEST_CAMERA->{
                    val thread =Thread(Runnable {
                        val bitmap:Bitmap=BitmapFactory.decodeFile(photoFile!!.absolutePath)
                        val bitmapRotated:Bitmap=rotateImage(bitmap)
                        val result:Bitmap=resizeBitmap(bitmapRotated,400,400)
                        showBitmap(result)
                    })
                    thread.start()
                }
            }
        }
    }

    private fun showBitmap(bitmap: Bitmap) {
        activity!!.runOnUiThread{
            camera.setImageBitmap(bitmap)
        }
    }


    //es para rotar la imagen
    private fun rotateImage(bitmap: Bitmap): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        val matrix = Matrix()
        matrix.postRotate(90f)
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            bitmapWidth,
            bitmapHeight,
            true
        )
        return Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrix,
            true
        )
    }


    //Para especificar un tamaño a la immagen
    fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        val ratioX = newWidth / (bitmap.getWidth()).toFloat()
        val ratioY = newHeight / (bitmap.getHeight()).toFloat()
        val middleX = newWidth / 2.0f;
        val middleY = newHeight / 2.0f;

        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        val canvas = Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(
            bitmap,
            middleX - bitmap.getWidth() / 2,
            middleY - bitmap.getHeight() / 2,
            Paint(Paint.FILTER_BITMAP_FLAG)
        );

        return scaledBitmap;
    }


}





