//
// Created by chang on 2018/4/13.
//

#include <stdio.h>
#include <stdlib.h>
#include "com_actions_voicebletest_jni_DecodeJni.h"
#include <string.h>
#include <jni.h>
#include <android/log.h>
#include "ima_dec.h"
#include<dlfcn.h>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "Decode-jni-call", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "Decode-jni-call", __VA_ARGS__))

#ifdef __cplusplus
extern "C" {
#endif

typedef short (*pFunInit)(short);
typedef short (*pFunDecode)(short *synth, short *serial, short len, short codec);
pFunDecode funDecode = NULL;
typedef short (*pFunInitTest)(int*, short);
typedef short (*pFunDecodeTest)(int* dec_buf, short *input, short input_len, short *output, short output_len);
pFunDecodeTest funDecodeTest = NULL;
int    dec_buf[32];

JNIEXPORT jshort JNICALL Java_com_actions_voicebletest_jni_DecodeJni_decodeInit
  (JNIEnv *env, jobject jobj, jshort codec)
  {
        LOGI("JNI ASC_Decoder_Init");
        void *handle = dlopen("libasc_dec.so",RTLD_LAZY);
        const char *error = NULL;
        pFunInit fun;
        if(handle==NULL){
            return 0;
        }
        dlerror();
        fun = (pFunInit)dlsym(handle,"ASC_Decoder_Init");
        funDecode = (pFunDecode)dlsym(handle,"ASC_Decoder");
        if((error=dlerror()) != NULL)
        {
           LOGI("error:dlsym method decodeInit fail ");
           return 0;
        }

        jshort ret = fun((short)codec);
        dlclose(handle);
        LOGI("JNI ASC_Decoder_Init fun() finish");
        return ret;
  }

 JNIEXPORT jshortArray JNICALL Java_com_actions_voicebletest_jni_DecodeJni_Decode
        (JNIEnv *env, jobject jobj, jshortArray arr_in, jshort len, jshort codec)
    {
        //创建一个指定大小的数组
            LOGI("start");
        	jshort *elems_in = env->GetShortArrayElements(arr_in, NULL);
        	//len = env->GetArrayLength(arr_in);
            jshortArray arr_out = (env)->NewShortArray(320/2);
            jshort *elems_out = (env)->GetShortArrayElements(arr_out, NULL);
            for(int i=0; i<320/2; i++)
            {
                    elems_out[i] = 0;
            }

            //for (int i=0;i < len; i++){
            //    LOGI("input: %x" , elems_in[i]);
           // }

            LOGI("decode start: ");
            short Samplelen = 0;
            if (funDecode != NULL)
            {
                Samplelen = funDecode(elems_in,(short*)elems_out, len, codec);
                LOGI("decode finish Samplelen: %d" , Samplelen);
            }
            jshortArray arr_ret = (env)->NewShortArray(Samplelen);
            jshort *elems_ret = (env)->GetShortArrayElements(arr_ret, NULL);
            for(int i=0; i<Samplelen; i++)
            {
                elems_ret[i] = elems_out[i];
             }

        	//ima_adpcm_decode_proc((short*)elems_out, (unsigned char *)elems_in, len);

            //同步
        	//mode
            //0, Java数组进行更新，并且释放C/C++数组
            //JNI_ABORT, Java数组不进行更新，但是释放C/C++数组
            //JNI_COMMIT，Java数组进行更新，不释放C/C++数组（函数执行完，数组还是会释放）
            (env)->ReleaseShortArrayElements( arr_out, elems_out, 0);
            (env)->ReleaseShortArrayElements( arr_ret, elems_ret, 0);
        	(env)->ReleaseShortArrayElements( arr_in, elems_in, 0);
        	LOGI("JNI ASC_Decoder fun() finish");
            return arr_ret;
    }

    JNIEXPORT jshort JNICALL Java_com_actions_voicebletest_jni_DecodeJni_decodeInitTest
      (JNIEnv *env, jobject jobj, jshort codec)
      {
            LOGI("JNI ASC_Decoder_Init");
            void *handle = dlopen("libaasc_dec.so",RTLD_LAZY);
            const char *error = NULL;
            pFunInitTest fun;
            if(handle==NULL){
                return 0;
            }
            dlerror();
            fun = (pFunInitTest)dlsym(handle,"AASC_Decoder_Init");
            funDecodeTest = (pFunDecodeTest)dlsym(handle,"AASC_Decoder");
            if((error=dlerror()) != NULL)
            {
               LOGI("error:dlsym method decodeInit fail ");
               return 0;
            }

            for(int i=0; i < 32; i++){
                dec_buf[i] = 0;
            }
            jshort ret = fun(dec_buf, (short)codec);
            dlclose(handle);
            LOGI("JNI ASC_Decoder_Init fun() finish");
            return ret;
      }

     JNIEXPORT jshortArray JNICALL Java_com_actions_voicebletest_jni_DecodeJni_DecodeTest
            (JNIEnv *env, jobject jobj, jshortArray arr_in, jshort len, jshort codec)
        {
            //创建一个指定大小的数组
                LOGI("start");
            	jshort *elems_in = env->GetShortArrayElements(arr_in, NULL);
            	//len = env->GetArrayLength(arr_in);
                jshortArray arr_out = (env)->NewShortArray(256);
                jshort *elems_out = (env)->GetShortArrayElements(arr_out, NULL);
                for(int i=0; i<256; i++)
                {
                        elems_out[i] = 0;
                }

                //for (int i=0;i < len; i++){
                //    LOGI("input: %x" , elems_in[i]);
               // }

                LOGI("decode start: ");
                unsigned int Samplelen = 0;
                short llen = len;
                short out_len = 256;
                if (funDecodeTest != NULL)
                {
                    LOGI("decode finish len: %d, %d %d" , llen, Samplelen, out_len);
                    Samplelen = funDecodeTest(dec_buf ,elems_in, llen,(short*)elems_out, out_len);
                    LOGI("decode finish Samplelen: %d" , Samplelen);
                }
                jshortArray arr_ret = (env)->NewShortArray(Samplelen/2);
                jshort *elems_ret = (env)->GetShortArrayElements(arr_ret, NULL);
                for(int i=0; i<Samplelen/2; i++)
                {
                    elems_ret[i] = elems_out[i];
                 }

            	//ima_adpcm_decode_proc((short*)elems_out, (unsigned char *)elems_in, len);

                //同步
            	//mode
                //0, Java数组进行更新，并且释放C/C++数组
                //JNI_ABORT, Java数组不进行更新，但是释放C/C++数组
                //JNI_COMMIT，Java数组进行更新，不释放C/C++数组（函数执行完，数组还是会释放）
                (env)->ReleaseShortArrayElements( arr_out, elems_out, 0);
                (env)->ReleaseShortArrayElements( arr_ret, elems_ret, 0);
            	(env)->ReleaseShortArrayElements( arr_in, elems_in, 0);
            	LOGI("JNI ASC_Decoder fun() finish");
                return arr_ret;
        }
#ifdef __cplusplus
}
#endif
