# Kotlin安卓开发-Broadcast

## 一、广播机制简介

Android中的每个应用程序都可以对自己感兴趣的广播进行注册，这样该程序就只会收到自己所关心的广播内容，这些广播可能是来自于系统的，也可能是来自于其他应用程序的。Android提供了一套完整的API，允许应用程序自由地发送和接收广播。发送广播可以借助之前学习过的Intent，而接收广播的方法则需要引入一个新的概念——BroadcastReceiver。

这里我们先来了解一下广播的类型，Android中的广播主要可以分为两种类型：标准广播和有序广播。

- 标准广播（normal broadcasts）：是一种完全异步执行的广播，在广播发出之后，所有的BroadcastReceiver几乎会在同一时刻收到这条广播消息，因此它们之间没有任何先后顺序可言。这种广播的效率会比较高，但同时也意味着它是无法被截断的。

![1700625229306.png](https://pic.ziyuan.wang/2023/11/22/xiheya_6318f129cb33c.png)

- 有序广播（ordered broadcasts）则是一种同步执行的广播，在广播发出之后，同一时刻只会有一个BroadcastReceiver能够收到这条广播消息，当这个BroadcastReceiver中的逻辑执行完毕后，广播才会继续传递。所以此时的BroadcastReceiver是有先后顺序的，优先级高的BroadcastReceiver就可以先收到广播消息，并且前面的BroadcastReceiver还可以截断正在传递的广播，这样后面的BroadcastReceiver就无法收到广播消息了。

![1700625787026.png](https://pic.ziyuan.wang/2023/11/22/xiheya_ffd533f3040a9.png)

## 二、接收系统广播

Android内置了很多系统级别的广播，我们可以在应用程序中通过监听这些广播来得到各种系统的状态信息。比如手机开机完成后会发出一条广播，电池的电量发生变化会发出一条广播，系统时间发生改变也会发出一条广播，等等。如果想要接收这些广播，就需要使用BroadcastReceiver，下面我们就来看一下它的具体用法。

### 2.1 动态注册监听时间变化

我们可以根据自己感兴趣的广播，自由地注册BroadcastReceiver，这样当有相应的广播发出时，相应的BroadcastReceiver就能够收到该广播，并可以在内部进行逻辑处理。注册BroadcastReceiver的方式一般有两种：在代码中注册（**动态注册**）和在AndroidManifest.xml中注册（**静态注册**）。其中前者也被称为动态注册，后者也被称为静态注册。

该如何创建一个BroadcastReceiver呢？其实只需新建一个类，让它继承自BroadcastReceiver，并重写父类的onReceive()方法就行了。这样当有广播到来时，onReceive()方法就会得到执行，具体的逻辑就可以在这个方法中处理。

```kotlin
package work.icu007.broadcasttest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import work.icu007.broadcasttest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var timeChangeReceiver: TimeChangeReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        val intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.TIME_TICK")
        timeChangeReceiver = TimeChangeReceiver()
        registerReceiver(timeChangeReceiver,intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(timeChangeReceiver)
    }

    inner class TimeChangeReceiver: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            Toast.makeText(context, "Time has changed", Toast.LENGTH_SHORT).show()
        }

    }
}
```

我们在MainActivity中定义了一个内部类TimeChangeReceiver，这个类是继承自BroadcastReceiver的，并重写了父类的onReceive()方法。这样每当系统时间发生变化时，onReceive()方法就会得到执行，这里只是简单地使用Toast提示了一段文本信息。然后观察onCreate()方法，首先我们创建了一个IntentFilter的实例，并给它添加了一个值为android.intent.action.TIME_TICK的action，为什么要添加这个值呢？因为当系统时间发生变化时，系统发出的正是一条值为android.intent.action.TIME_TICK的广播，也就是说我们的BroadcastReceiver想要监听什么广播，就在这里添加相应的action。接下来创建了一个TimeChangeReceiver的实例，然后调用registerReceiver()方法进行注册，将TimeChangeReceiver的实例和IntentFilter的实例都传了进去，这样TimeChangeReceiver就会收到所有值为android.intent.action.TIME_TICK的广播，也就实现了监听系统时间变化的功能。

动态注册的BroadcastReceiver一定要取消注册才行，一般会选择在onDestroy()方法中通过调用unregisterReceiver()方法来实现取消注册。

系统每隔一分钟就会发出一条android.intent.action.TIME_TICK的广播，因此我们最多只需要等待一分钟就可以收到这条广播了。

查看完整的系统广播列表，可以到如下的路径中去查看：

> `<Android SDK>/platforms/<任意android api版本>/data/broadcast_actions.txt`

### 2.2 静态实现开机启动

动态注册的BroadcastReceiver可以自由地控制注册与注销，在灵活性方面有很大的优势。但是它存在着一个缺点，即必须在程序启动之后才能接收广播，因为注册的逻辑是写在onCreate()方法中的。如果我们需要实现在程序未启动时也接收广播，我们就需要使用静态注册的方式。

其实从理论上来说，动态注册能监听到的系统广播，静态注册也应该能监听到，在过去的Android系统中确实是这样的。但是由于大量恶意的应用程序利用这个机制在程序未启动的情况下监听系统广播，从而使任何应用都可以频繁地从后台被唤醒，严重影响了用户手机的电量和性能，因此Android系统几乎每个版本都在削减静态注册BroadcastReceiver的功能。

在Android 8.0系统之后，所有隐式广播都不允许使用静态注册的方式来接收了。隐式广播指的是那些没有具体指定发送给哪个应用程序的广播，大多数系统广播属于隐式广播，但是少数特殊的系统广播目前仍然允许使用静态注册的方式来接收。这些特殊的系统广播列表详见： [允许使用静态注册的广播](https://developer.android.google.cn/guide/components/broadcastexceptions.
html。)。

在这些特殊的系统广播当中，有一条值为android.intent.action.BOOT_COMPLETED的广播，这是一条开机广播。

这里我们准备实现一个开机启动的功能。在开机的时候，我们的应用程序肯定是没有启动的，因此这个功能显然不能使用动态注册的方式来实现，而应该使用静态注册的方式来接收开机广播，然后在onReceive()方法里执行相应的逻辑，这样就可以实现开机启动的功能了。

右击work.icu007.broadcasttest包→New→Other→Broadcast Receiver.将创建的类命名为BootCompleteReceiver. Exported属性表示是否允许这个BroadcastReceiver接收本程序以外的广播，Enabled属性表示是否启用这个BroadcastReceiver。勾选这两个属性，点击“Finish”完成创建。

修改BootCompleteReceiver中的代码，如下所示：

```kotlin
package work.icu007.broadcasttest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class BootCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        TODO("BootCompleteReceiver.onReceive() is not implemented")
        val TAG = "BootCompleteReceiver"
        Log.d(TAG, "onReceive: boot complete")
        Toast.makeText(context, "Boot Complete", Toast.LENGTH_SHORT).show()
    }
}
```

另外，静态的BroadcastReceiver一定要在AndroidManifest.xml文件中注册才可以使用。不过，由于我们是使用Android Studio的快捷方式创建的BroadcastReceiver，因此注册这一步已经自动完成了。

目前的BootCompleteReceiver是无法收到开机广播的，因为我们还需要对AndroidManifest.xml文件进行修改，如下所示：

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<receiver
    android:name=".BootCompleteReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
    </intent-filter>
</receiver>
```

由于Android系统启动完成后会发出一条值为android.intent.action.BOOT_COMPLETED的广播，因此我们在<receiver>标签中又添加了一个<intent-filter>标签，并在里面声明了相应的action。

另外，这里有非常重要的一点需要说明。Android 系统为了保护用户设备的安全和隐私，做了严格的规定：如果程序需要进行一些对用户来说比较敏感的操作，必须在AndroidManifest.xml文件中进行权限声明，否则程序将会直接崩溃。比如这里接收系统的开机广播就是需要进行权限声明的，所以我们在上述代码中使用<uses-permission>标签声明了android.permission.RECEIVE_BOOT_COMPLETED权限。

到目前为止，我们在BroadcastReceiver的onReceive()方法中只是简单地使用Toast提示了一段文本信息，当你真正在项目中使用它的时候，可以在里面编写自己的逻辑。需要注意的是，不要在onReceive()方法中添加过多的逻辑或者进行任何的耗时操作，因为BroadcastReceiver中是不允许开启线程的，当onReceive()方法运行了较长时间而没有结束时，程序就会出现错误。

### 2.3 发送自定义广播

广播主要分为两种类型：标准广播和有序广播。现在通过实践的方式来看一下这两种广播具体的区别。

#### 2.3.1 发送标准广播

在发送广播之前，我们还是需要先定义一个BroadcastReceiver来准备接收此广播，不然发出去也是白发。因此新建一个MyBroadcastReceiver，并在onReceive()方法中加入如下代码：

```kotlin
package work.icu007.broadcasttest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MyBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "received in MyBroadcastReceiver", Toast.LENGTH_SHORT).show()
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        TODO("BroadcastReceiver.onReceive() is not implemented")
    }
}
```

当MyBroadcastReceiver收到自定义的广播时，就会弹出“received in MyBroadcastReceiver”的提示。然后在AndroidManifest.xml中对这个BroadcastReceiver进行修改：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    ...
    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.BroadcastTest"
        tools:targetApi="31">
        <receiver
            android:name=".MyBroadcastReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="work.icu007.broadcasttest.MY_BROADCAST"/>
            </intent-filter>
        </receiver>
        ...
    </application>

</manifest>
```

可以看到，这里让MyBroadcastReceiver接收一条值为work.icu007.broadcasttest.MY_BROADCAST的广播，因此待会儿在发送广播的时候，我们就需要发出这样的一条广播。

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var timeChangeReceiver: TimeChangeReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        val intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.TIME_TICK")
        timeChangeReceiver = TimeChangeReceiver()
        registerReceiver(timeChangeReceiver,intentFilter)
        activityMainBinding.btnSendBroadcast.setOnClickListener {
            val intent = Intent("work.icu007.broadcasttest.MY_BROADCAST")
            intent.setPackage(packageName)
            sendBroadcast(intent)
        }
    }
}
```

我们在按钮的点击事件里面加入了发送自定义广播的逻辑。首先构建了一个Intent对象，并把要发送的广播的值传入。然后调用Intent的setPackage()方法，并传入当前应用程序的包名。packageName是getPackageName()的语法糖写法，用于获取当前应用程序的包名。最后调用sendBroadcast()方法将广播发送出去，这样所有监听work.icu007.broadcasttest.MY_BROADCAST这条广播的BroadcastReceiver就会收到消息了。此时发出去的广播就是一条标准广播。

在Android8.0系统之后，静态注册的BroadcastReceiver是无法接收隐式广播的，而默认情况下我们发出的自定义广播恰恰都是隐式广播。因此这里一定要调用setPackage()方法，指定这条广播是发送给哪个应用程序的，从而让它变成一条显式广播，否则静态注册的BroadcastReceiver将无法接收到这条广播。

由于广播是使用Intent来发送的，因此我们还可以在Intent中携带一些数据传递给相应的BroadcastReceiver，这一点和Activity的用法是比较相似的。

#### 2.3.2 发送有序广播

和标准广播不同，有序广播是一种同步执行的广播，并且是可以被截断的。为了验证这一点，我们需要再创建一个新的BroadcastReceiver。新建AnotherBroadcastReceiver，代码如下所示：

```kotlin
package work.icu007.broadcasttest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class AnotherBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "received broadcast in AnotherBroadcastReceiver", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "onReceive: ")
    }

    companion object{
        const val TAG = "AnotherBroadcastReceiver"
    }
}
```

然后在AndroidManifest.xml中对这个BroadcastReceiver进行修改：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    ...
    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.BroadcastTest"
        tools:targetApi="31">
        <receiver
            android:name=".AnotherBroadcastReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter android:priority="50">
                <action android:name="work.icu007.broadcasttest.MY_BROADCAST" />
            </intent-filter>
        </receiver>
        <receiver
            android:name=".MyBroadcastReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="work.icu007.broadcasttest.MY_BROADCAST"/>
            </intent-filter>
        </receiver>
        ...
    </application>

</manifest>
```

AnotherBroadcastReceiver同样接收的是work.icu007.broadcasttest.MY_BROADCAST这条广播。

现在查岗你是发送广播的时候选择发送有序广播，如下：

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var timeChangeReceiver: TimeChangeReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        val intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.TIME_TICK")
        timeChangeReceiver = TimeChangeReceiver()
        registerReceiver(timeChangeReceiver,intentFilter)
        activityMainBinding.btnSendBroadcast.setOnClickListener {
            val intent = Intent("work.icu007.broadcasttest.MY_BROADCAST")
            intent.setPackage(packageName)
            sendOrderedBroadcast(intent,null)
//            sendBroadcast(intent)
        }
    }
}
```

发送有序广播只需要改动一行代码，即将sendBroadcast()方法改成sendOrderedBroadcast()方法。sendOrderedBroadcast()方法接收两个参数：第一个参数仍然是Intent；第二个参数是一个与权限相关的字符串，这里传入null就行了。现在重新运行程序，并点击“Send Broadcast”按钮，你会发现，两个BroadcastReceiver仍然都可以收到这条广播。

看上去好像和标准广播并没有什么区别嘛。不过别忘了，这个时候的BroadcastReceiver是有先后顺序的，而且前面的BroadcastReceiver还可以将广播截断，以阻止其继续传播。

该如何设定BroadcastReceiver的先后顺序呢？当然是在注册的时候进行设定了，修改AndroidManifest.xml中的代码，如下所示：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    ...
    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.BroadcastTest"
        tools:targetApi="31">
        <receiver
            android:name=".AnotherBroadcastReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter android:priority="50">
                <action android:name="work.icu007.broadcasttest.MY_BROADCAST" />
            </intent-filter>
        </receiver>
        <receiver
            android:name=".MyBroadcastReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter android:priority="50">
                <action android:name="work.icu007.broadcasttest.MY_BROADCAST"/>
            </intent-filter>
        </receiver>
        ...
    </application>

</manifest>
```

我们通过android:priority属性给BroadcastReceiver设置了优先级，优先级比较高的BroadcastReceiver就可以先收到广播。这里将MyBroadcastReceiver的优先级设成了100，以保证它一定会在AnotherBroadcastReceiver之前收到广播。

既然已经获得了接收广播的优先权，那么MyBroadcastReceiver就可以选择是否允许广播继续传递了。修改MyBroadcastReceiver中的代码，如下所示:

```kotlin
class MyBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "received in MyBroadcastReceiver", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "onReceive: ")
        abortBroadcast()
    }

    companion object{
        const val TAG = "MyBroadcastReceiver"
    }
}
```

如果在onReceive()方法中调用了abortBroadcast()方法，就表示将这条广播截断，后面的BroadcastReceiver将无法再接收到这条广播。



