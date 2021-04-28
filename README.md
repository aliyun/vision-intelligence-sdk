# 1 概述
viapi-android-sdk-demo 是阿里达摩院推出的一款适用于 Android 平台的实时视频 SDK，提供了包括人像抠图、美颜、人像关键点检测等多种功能。

# 2 功能列表
+ 视频流实时人像分割
+ 本地图片人像分割
+ ~~美颜功能（瘦脸、大眼、美白、磨皮等）~~
+ ~~人脸关键点检测~~

# 3 SDK开发包适配及包含内容说明
## 3.1 支持的系统和硬件版本
+ 硬件要求：要求设备上有相机模块,陀螺仪模块
+ CPU架构：armeabi-v7a
+ 系统：最低支持 Android 4.0（API Level 14）需要开发者通过minSdkVersion来保证支持系统的检测

## 3.2 开发包资源说明
+ ovp-sdk-x.x.x.x.aar     ——viapi的sdk的aar包，具体版本以获取到的最终版本为准
+ damo-viapi.license      ——sdk全局license文件，对所有能力生效，名字固定不允许修改
+ damo-viapi-xxx.license  ——sdk单个能力license文件，只对单个能力生效，名字路径可以自定义
+ xxx.nn                  ——以.nn为结尾的是SDK使用到的模型文件

# 4 SDK集成步骤
## 4.1 将算法能力相关的文件包导入到工程
把sdk的aar拷贝到主工程libs目录下，把模型文件xxx.nn文件和damo-viapi.license拷贝到工程app module的assets目录下。如下图：

<img src="https://viapi-test.oss-cn-shanghai.aliyuncs.com/hanbing/viapi-sdk-android-4.1.png" width = "447" height = "466" />

**注意：不要修改全局的license文件名称及存放的路径，否则内部拷贝失败。**
## 4.2 工程gradle配置
在主工程的build.gradle文件相关配置设置，主工程的build.gradle文件在Project目录中位置如下图：

<img src="https://viapi-test.oss-cn-shanghai.aliyuncs.com/hanbing/viapi-sdk-android-4.2.png" width = "556" height = "320" />

```
android {
   defaultConfig {
       ndk {
           //设置支持的SO库架构
           abiFilters "armeabi-v7a"
           }
        }
}
```
# 5 SDK调用步骤
## 5.1 SDK初始化
#### 接口描述：
算法API使用前先调用SDK初始化接口，初始化之后，各功能才可以正常使用，否则会引起鉴权等异常，初始化建议放在app进程启动时Application onCreate中进行。

#### 初始化接口：
```
VIAPICreateApi.getInstance().getVIAPISdkCore().init(Context context);
```
#### 参数说明：
Context context 应用上下文。

#### 返回值 ：
int类型，返回0为初始化成功，其它返回为初始化失败，具体请参考6.1 错误码含义。

#### 具体代码示例如下：
```
  private void initSDK() {
    int status = VIAPICreateApi.getInstance().getVIAPISdkCore().init(this);
    if (status != 0) {
        Toast.makeText(this, VIAPIStatusCode.getErrorMsg(status), Toast.LENGTH_LONG).show();
    } else {
        Toast.makeText(this, "初始化成功！", Toast.LENGTH_LONG).show();
    }
}
```
## 5.2 人像分割API使用
### 5.2.1 创建算法实例
#### 实时视频分割接口描述：
HumanSegment：在需要用到视频实时分割算法的地方，创建HumanSegment分割实例，HumanSegment是视频实时人像分割API接口对象，通过此对象可以完成camera实时视频流分割能力的使用。

#### 算法实例化接口：
```
HumanSegment mHumanSegment = new HumanSegment();
```

#### 本地图片分割接口描述：
HumanPhotoSegment：在需要用到本地图片分割算法的地方，创建HumanPhotoSegment分割实例，HumanPhotoSegment是本地图片人像分割API接口对象，通过此对象可以完成图片分割能力的使用。

#### 算法实例化接口：
```
HumanPhotoSegment mHumanPhotoSegment = new HumanPhotoSegment();
```

#### 参数说明：
无
#### 返回值:
无
#### 具体代码示例如下：
```
private final HumanSegment mHumanSegment;
private final HumanPhotoSegment mHumanPhotoSegment;
mHumanSegment = new HumanSegment();
mHumanSegment = new HumanPhotoSegment();
```

### 5.2.2 单个能力license鉴权
#### 接口描述：
license证书验签接口，验签通过后才能成功调用算法。

#### license证书验签接口：
```
mHumanSegment.nativeCheckLicense(String licensePath);

```
#### 获取证书路径：针对所有能力生效
```
VIAPICreateApi.getInstance().getVIAPISdkCore().getLicensePath();
```
#### 参数说明：
String licensePath  传入全局证书license路径或自定义的单个能力license路径。

注：如果所有能力使用同一个全局证书默认传入全局证书路径即可，如果接入方有针对此能力的单独证书，则需要传入单独的证书文件的绝对路径

#### 返回值：
int类型，返回0为验签成功，其它返回为验签失败。
#### 具体代码示例如下：
```
int errorCode = mHumanSegment.nativeCheckLicense(licensePath);
```
### 5.2.3 创建算法实例
#### 接口描述：
创建算法内部用于图像分割的实例对象，为图像分割做准备。
#### 接口示例：
HumanSegment.nativeSegmentCreate();
#### 参数说明：
无
#### 返回值：
int类型，返回0为创建算法实例成功，其它返回为创建算法实例失败。
#### 具体代码示例如下：
```
int errorCode = = mHumanSegment.nativeSegmentCreate();
```
### 5.2.4 算法init初始化
#### 接口描述：
初始化实例之后的算法对象
#### 接口示例：
HumanSegment.nativeSegmentInit(String modelsPath);
#### 参数说明：
String modelsPath 为算法模型文件系统的绝对路径。
#### 返回值：
int类型，返回0为算法初始化成功，其它返回为算法初始化失败。
#### 具体代码示例如下：
```
int errorCode = mHumanSegment.nativeSegmentInit(modelsPath);
```
**注意：5.2.3、5.2.4步为算法初始化，init方法比较耗时，建议在后台线程执中行操作。**
### 5.2.5 分割处理
#### 5.2.5.1 视频流实时分割算法处理
#### 接口描述：
该方法为处理实时视频分割的接口，传入camera的原始nv21数据，获得分割后的视频图像rgba格式的buffer输出数据，适用于相机预览、视频播放处理。
#### 接口示例：
```
HumanSegment.nativeSegmentProcessBuffer(byte[] yuv420sp,int textureWidth,int textureHeight,int angle,int cameraFace,int step,ByteBuffer mDstBuffer);
```
#### 参数说明：
+ yuv420sp：相机输入数据。Camera1可以通过onPreviewFrame回调获得，Camera2可以通过onImageAvailable回调获得。
+ textureWidth：预览图像的宽。
+ textureHeight：预览图像的高。
+ angle：图像旋转的角度。可通过Sensor对设备旋转角度判断获得，计算方法详见demo。
+ cameraFace：相机的前后摄像头 前置为1 后置为0。
+ step：算法的步数，算法规定，算法处理帧第一帧传0，其他帧传1。
+ mDstBuffer：算法处理后的RGBA格式的输出数据。

#### 返回值:
int类型，返回0为图像分割算法处理成功，其它返回为图像分割算法处理失败。
#### 调用样例代码如下:
```
 public synchronized int processSegmentForBuffer(byte[] yuv420sp, int textureWidth, int textureHeight, int cameraFace, int angle) {
        if (!mInitSegment.get() || (blendImageBg == null)) {
            return -1;
        }

        if (mTexHeight != textureHeight || mTexWeight != textureWidth) {
            releaseGL();
            mTexHeight = textureHeight;
            mTexWeight = textureWidth;
            mDstBuffer = ByteBuffer.allocateDirect(textureWidth * textureHeight * 4);
        }
        if (mDstBuffer == null) {
            mDstBuffer = ByteBuffer.allocateDirect(textureWidth * textureHeight * 4);
        }
        mDstBuffer.clear();
        synchronized (mInitLock) {
            mHumanSegment.nativeSegmentProcessBuffer(yuv420sp, textureWidth, textureHeight, angle, cameraFace, sep, mDstBuffer);
        }
        if (sep == 0) {
            sep = 1;
        }
        //如果需要进行OpenGL渲染，所有OpenGL相关的操作必须放在OpenGL线程中，下面方法需要在有openGL上下文环境的线程调用
        mSegmentTexId = OpenGLUtil.loadTexture(mDstBuffer, textureWidth, textureHeight, mSegmentTexId);
        return mSegmentTexId;
    }
```

**注意：算法内部没有对内存进行处理，输出buffer需提前申请内存空间，初始化格式为：**
```
mDstBuffer = ByteBuffer.allocateDirect(textureWidth * textureHeight * 4);
```
#### 5.2.5.2 本地图片的分割
#### 接口描述:
HumanPhotoSegment对象的该接口可以对本地单张图片进行分割，适用于证件照等本地抠图场景。
#### 接口示例：
```
public native int nativeSegmentProcess(byte[] img, int width, int height, int channel, byte[] out);
```
#### 参数说明：
+ img：待处理图片的数据，目前仅支持rgba。
+ width：图像数据的宽。
+ height：图像数据的高。
+ channel： 数据通道数，目前只需要传4即可。
+ out：    算法处理后返回的图片buffer数据。

#### 返回值：
int类型，返回0为图像分割算法处理成功，其它返回为图像分割算法处理失败。
#### 具体代码示例如下：
```
ByteBuffer originalBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
ByteBuffer dstBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
bitmap.copyPixelsToBuffer(originalBuffer);
int status = mHumanSegment.nativeSegmentProcess(originalBuffer.array(), bitmap.getWidth(), bitmap.getHeight(), 4, dstBuffer.array());
```
**注意：算法内部没有对内存进行处理，输出buffer需提前申请内存空间，初始化格式为：**
```
ByteBuffer dstBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
```

### 5.2.6 算法Clear操作
#### 接口描述：
与init成对使用，反init操作，在不需要用到算法的时候，进行算法资源的删除。
#### 接口示例：
```
HumanSegment.nativeSegmentClear();
```
#### 参数说明：
无
#### 返回值：
int类型，返回0为删除算法相关成功，其它返回为删除算法相关处理失败。
#### 具体代码示例如下：
```
  mHumanSegment.nativeSegmentClear();
```
**注意：nativeSegmentClear调用之后，再次使用必须重新调用5.2.6 nativeSegmentInit 进行算法初始化**

### 5.2.7 算法销毁Destroy
#### 接口描述：
在不需要用到算法的时候，对算法对象的销毁内存释放操作。
#### 接口示例：
```
HumanSegment.nativeSegmentDestroy();
```
#### 参数说明:
无
#### 返回值：
int类型，返回0为销毁算法相关成功，其它返回为销毁算法相关处理失败。
#### 具体代码示例如下：
```
  mHumanSegment.nativeSegmentDestroy();
```
**注意：5.2.6、5.2.7 释放资源时要先clear再destroy**
### 5.2.8 各方法调用顺序说明
1. 先在Application中对 SDK初始化化：VIAPICreateApi.getInstance().getVIAPISdkCore().init(Context context);
2. 创建算法实例 5.2.1 ：HumanSegment mHumanSegment = new HumanSegment();
3. license验签 5.2.2 ：mHumanSegment.nativeCheckLicense(String licensePath);
4. 创建算法对象实例 5.2.3 ：HumanSegment.nativeSegmentCreate();
5. 算法init初始化操作 5.2.4 ：HumanSegment.nativeSegmentInit(String modelsPath);
6. 实时视频流分割算法处理 5.2.5 ：HumanSegment.nativeSegmentProcessBuffer(byte[] yuv420sp,int textureWidth,int textureHeight,int angle,int cameraFace,int step,ByteBuffer mDstBuffer);
7. 算法clear操作 5.2.6 ：HumanSegment.nativeSegmentClear();
8. 销毁算法对象 5.2.7 ：HumanSegment.nativeSegmentDestroy();

**注意：5.2.5为视频抠图和单张图片抠图两个方法，根据创建的对象选择调用即可**

## 5.3 美颜API使用

### 5.3.1 美颜算法实例对象

# 6 离线鉴权错误码定义
## 6.1 错误码含义
+ -211  license没有初始化直接调用API接口。
+ -212  当前的license与调用app不是绑定关系，license用在其他app中使用。
+ -213  license无效。
+ -214  license授权时间过期。
+ -215  此license中不包含调用的算法能力（未购买此能力）。
+ -216  bundle id获取失败。

# 7 Demo说明
+ 项目实时视频图像分割渲染及背景叠加是通过openGL相关操作渲染的，详见demo。
+ 运行demo时，需将正式的license替换到assets目录，且applicationID(包名)和license对应。
+ sdk的license包含了多个算法能力，如用户申请的license不包含其中的某算法能力，调用对应的算法API对应的接口时返回错误，错误说明详见<6.1 错误码含义>。

# 8 license鉴权接口
## 8.1 全局license鉴权
#### 接口描述：
目前仅提供获取license过期时间方法调用与获取全局license路径获取

#### 获取证书路径
```
VIAPICreateApi.getInstance().getVIAPISdkCore().getLicensePath();
```
#### 获取过期时间：获取当前SDK中全局license的过期时间，业务层通过此时间可以实现续费，用新的license文件替换掉此过期的license文件实现续费
```
  String sdkExpireTime = VIAPICreateApi.getInstance().getVIAPISdkCore().getLicenseExpireTime();
```

#### 返回值：
license过期时间
#### 具体代码示例如下：
```
    String sdkExpireTime = VIAPICreateApi.getInstance().getVIAPISdkCore().getLicenseExpireTime();
    if (!TextUtils.isEmpty(sdkExpireTime)) {
      Logs.i(TAG, "到期日 = " + sdkExpireTime + ", 距离到期天数 = " + licenseExpireDays(sdkExpireTime));
    }
    private int licenseExpireDays(String sdkExpireTime) {
        int days = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date expireData;
        Date today = new Date();
        try {
            expireData = sdf.parse(sdkExpireTime);
            days = (int) ((expireData.getTime() - today.getTime()) / (1000 * 3600 * 24));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return days;
    }

    private void updateLicense() {
        String sdkExpireTime = VIAPICreateApi.getInstance().getVIAPISdkCore().getLicenseExpireTime();
        if (!TextUtils.isEmpty(sdkExpireTime)) {
            int expireDays = licenseExpireDays(sdkExpireTime);
            Logs.i(TAG, "到期日 = " + sdkExpireTime + ", 距离到期天数 = " + expireDays);
            if (expireDays < 30) {
                String licensePath = VIAPICreateApi.getInstance().getVIAPISdkCore().getLicensePath();
                String licenseFilePath = VIAPICreateApi.getInstance().getVIAPISdkCore().getLicenseFilePath();
                Logs.i(TAG, "licensePath = " + licensePath);
                Logs.i(TAG, "licenseFilePath = " + licenseFilePath);
                replaceLicense(licensePath, "新的license目录");
            }
        }
    }

    private void replaceLicense(String dstLicensePath, String newLicenseFile) {
        // 用新的license替换旧的license文件
    }
```
## 8.2 单能力license鉴权
#### 接口描述：非全局license只对本算法生效，需要单独通过算法对象初始化，如果使用全局license，单个能力不用调用nativeCheckLicense进行license鉴权。
### 8.2.1 非全局license初始化方法
```
 int nativeCheckLicense(String licensePath);
 // licensePath 为license文件绝对路径
 mHumanSegment.nativeCheckLicense(licensePath);
```
### 8.2.2 非全局license获取时间获取
```
  String licenseExpireTime = mHumanSegment.nativeGetLicenseExpireTime();
```

# 9 注意事项
+ viapi-android-sdk的 minSdkVersion为 14。
+ demo工程Android Studio 3.4 及以上，Open GLES 2.0 及以上。