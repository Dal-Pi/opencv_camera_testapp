#include <jni.h>
#include <string>

#include <opencv2/opencv.hpp>

using namespace cv;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_cameratestapp_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameratestapp_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                             jlong mat_addr_input,
                                                             jlong mat_addr_result) {
    Mat& matInput = *(Mat*)(mat_addr_input);
    Mat& matResult = *((Mat *)mat_addr_result);

    cv::cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameratestapp_MainActivity_ModifyBrightness(JNIEnv *env, jobject thiz,
                                                             jlong mat_addr_input,
                                                             jlong mat_addr_result,
                                                             jint brightness_added) {
    Mat& matInput = *(Mat*)(mat_addr_input);
    Mat& matResult = *((Mat *)mat_addr_result);

    matInput.convertTo(matResult, -1, 1, brightness_added);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameratestapp_MainActivity_Blur(JNIEnv *env, jobject thiz, jlong mat_addr_input,
                                                 jlong mat_addr_result, jint kernel_size) {
    Mat& matInput = *(Mat*)(mat_addr_input);
    Mat& matResult = *((Mat *)mat_addr_result);

    cv::blur(matInput, matResult, cv::Size(kernel_size, kernel_size));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameratestapp_MainActivity_Sharpening(JNIEnv *env, jobject thiz,
                                                       jlong mat_addr_input, jlong mat_addr_result,
                                                       jint weight) {
    Mat& matInput = *(Mat*)(mat_addr_input);
    Mat& matResult = *((Mat *)mat_addr_result);

    float data[9] = {-1, -1, -1, -1, 9, -1, -1, -1, -1};
    Mat matKernel(3, 3, CV_32FC1, data);

    Mat matSharpened;

    filter2D(matInput, matSharpened, -1, matKernel, Point(-1, -1), 0);
    addWeighted(matInput, (10-weight)/10.0, matSharpened, weight/10.0, 0, matResult);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameratestapp_MainActivity_Binarization(JNIEnv *env, jobject thiz,
                                                         jlong mat_addr_input,
                                                         jlong mat_addr_result, jint thresholdValue) {
    Mat& matInput = *(Mat*)(mat_addr_input);
    Mat& matResult = *((Mat *)mat_addr_result);

    threshold(matInput, matResult, thresholdValue, 255, THRESH_BINARY);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameratestapp_MainActivity_Erosion(JNIEnv *env, jobject thiz, jlong mat_addr_input,
                                                    jlong mat_addr_result, jint iteration) {
    Mat& matInput = *(Mat*)(mat_addr_input);
    Mat& matResult = *((Mat *)mat_addr_result);

    erode(matInput, matResult, Mat::ones(Size(3, 3), CV_8UC1), Point(-1, -1), iteration);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameratestapp_MainActivity_Dilation(JNIEnv *env, jobject thiz,
                                                     jlong mat_addr_input, jlong mat_addr_result,
                                                     jint iteration) {
    Mat& matInput = *(Mat*)(mat_addr_input);
    Mat& matResult = *((Mat *)mat_addr_result);

    dilate(matInput, matResult, Mat::ones(Size(3, 3), CV_8UC1), Point(-1, -1), iteration);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameratestapp_MainActivity_Opening(JNIEnv *env, jobject thiz, jlong mat_addr_input,
                                                    jlong mat_addr_result, jint iteration) {
    Mat& matInput = *(Mat*)(mat_addr_input);
    Mat& matResult = *((Mat *)mat_addr_result);

    Mat matEroded;

    erode(matInput, matEroded, Mat::ones(Size(3, 3), CV_8UC1), Point(-1, -1), iteration);
    dilate(matEroded, matResult, Mat::ones(Size(3, 3), CV_8UC1), Point(-1, -1), iteration);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameratestapp_MainActivity_Closing(JNIEnv *env, jobject thiz, jlong mat_addr_input,
                                                    jlong mat_addr_result, jint iteration) {
    Mat& matInput = *(Mat*)(mat_addr_input);
    Mat& matResult = *((Mat *)mat_addr_result);

    Mat matDilated;

    dilate(matInput, matDilated, Mat::ones(Size(3, 3), CV_8UC1), Point(-1, -1), iteration);
    erode(matDilated, matResult, Mat::ones(Size(3, 3), CV_8UC1), Point(-1, -1), iteration);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameratestapp_MainActivity_CannyEdge(JNIEnv *env, jobject thiz,
                                                      jlong mat_addr_input, jlong mat_addr_result,
                                                      jint threshold1, jint threshold2) {
    Mat& matInput = *(Mat*)(mat_addr_input);
    Mat& matResult = *((Mat *)mat_addr_result);

    Canny(matInput, matResult, threshold1, threshold2);
}