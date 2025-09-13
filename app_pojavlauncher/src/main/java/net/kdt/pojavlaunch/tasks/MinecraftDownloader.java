
package net.kdt.pojavlaunch.tasks;

import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.app.Activity;

import android.content.Context;

import android.net.ConnectivityManager;

import android.net.NetworkInfo;

import android.util.Log;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.JAssetInfo;

import net.kdt.pojavlaunch.JAssets;

import net.kdt.pojavlaunch.JMinecraftVersionList;

import net.kdt.pojavlaunch.NewJREUtil;

import net.kdt.pojavlaunch.R;

import net.kdt.pojavlaunch.Tools;

import net.kdt.pojavlaunch.mirrors.DownloadMirror;

import net.kdt.pojavlaunch.mirrors.MirrorTamperedException;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import net.kdt.pojavlaunch.utils.DownloadUtils;

import net.kdt.pojavlaunch.utils.FileUtils;

import net.kdt.pojavlaunch.value.DependentLibrary;

import net.kdt.pojavlaunch.value.MinecraftClientInfo;

import net.kdt.pojavlaunch.value.MinecraftLibraryArtifact;

import java.io.BufferedReader;

import java.io.File;

import java.io.FileNotFoundException;

import java.io.FileReader;

import java.io.FileWriter;

import java.io.IOException;

import java.util.ArrayList;

import java.util.Map;

import java.util.Set;

import java.util.concurrent.ArrayBlockingQueue;

import java.util.concurrent.ThreadPoolExecutor;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicLong;

import java.util.concurrent.atomic.AtomicReference;

public class MinecraftDownloader {

private static final double ONE_MEGABYTE = (1024d * 1024d);

public static final String MINECRAFT_RES = "https://resources.download.minecraft.net/";

private static final String MAVEN_CENTRAL_REPO1 = "https://repo1.maven.org/maven2/";

private AtomicReference mDownloaderThreadException;

private ArrayList mScheduledDownloadTasks;

private ArrayList mDeclaredNatives;

private AtomicLong mProcessedFileCounter;

private AtomicLong mProcessedSizeCounter; // Total bytes of processed files (passed SHA1 or downloaded)

private AtomicLong mInternetUsageCounter; // How many bytes downloaded over Internet

private long mTotalFileCount;

private long mTotalSize;

private File mSourceJarFile; // The source client JAR picked during the inheritance process

private File mTargetJarFile; // The destination client JAR to which the source will be copied to.

private boolean mUseFileCounter; // Whether a file counter or a size counter should be used for progress

private static final ThreadLocal sThreadLocalDownloadBuffer = new ThreadLocal&lt;&gt;();

private boolean isLocalProfile = false;

private boolean isOnline;

/**

* Start the game version download process on the global executor service.

* @param activity Activity, used for automatic installation of JRE 17 if needed

* @param version The JMinecraftVersionList.Version from the version list, if available

* @param realVersion The version ID (necessary)

* @param listener The download status listener

*/

public void start(@Nullable Activity activity, @Nullable JMinecraftVersionList.Version version,

@NonNull String realVersion,

@NonNull AsyncMinecraftDownloader.DoneListener listener) {

if(activity != null) {

isLocalProfile = Tools.isLocalProfile(activity);

isOnline = false; // Disable online check

Tools.switchDemo(Tools.isDemoProfile(activity));

} else {

isLocalProfile = true;

Tools.switchDemo(true);

}

sExecutorService.execute(() -> {

try {

// Force offline mode to always true

if(true /* offline mode forced for download */) {

String versionMessage = realVersion; // Use provided version unless we find its a modded instance

// See if provided version is a modded version and if that version depends on another jar, check for presence of both jar's .json.

try {

File providedJsonFile = new File(Tools.DIR_HOME_VERSION + "/" + realVersion + "/" + realVersion + ".json");

JMinecraftVersionList.Version providedJson = Tools.GLOBAL_GSON.fromJson(Tools.read(providedJsonFile.getAbsolutePath()), JMinecraftVersionList.Version.class);

File vanillaJsonFile = new File(Tools.DIR_HOME_VERSION + "/" + providedJson.inheritsFrom + "/" + providedJson.inheritsFrom + ".json");

versionMessage = providedJson.inheritsFrom != null ? providedJson.inheritsFrom : versionMessage;

if(providedJsonFile.length() == 0 || (vanillaJsonFile.exists() && vanillaJsonFile.length() == 0)) {

throw new RuntimeException("Minecraft " + versionMessage + " is needed by " + realVersion);

}

listener.onDownloadDone();

} catch(Exception e) {

String tryagain = "Please ensure you have an internet connection";

Tools.showErrorRemote(versionMessage + " is not currently installed. " + tryagain, e);

}

} else {

try {

downloadGame(activity, version, realVersion);

listener.onDownloadDone();

} catch(Exception e) {

listener.onDownloadFailed(e);

}

}

ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT);

});

}

// The rest of the class remains unchanged
