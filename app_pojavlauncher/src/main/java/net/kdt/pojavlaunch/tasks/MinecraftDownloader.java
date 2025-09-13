
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

private AtomicReference<Exception> mDownloaderThreadException;

private ArrayList<DownloaderTask> mScheduledDownloadTasks;

private ArrayList<File> mDeclaredNatives;

private AtomicLong mProcessedFileCounter;

private AtomicLong mProcessedSizeCounter; // Total bytes of processed files (passed SHA1 or downloaded)

private AtomicLong mInternetUsageCounter; // How many bytes downloaded over Internet

private long mTotalFileCount;

private long mTotalSize;

private File mSourceJarFile; // The source client JAR picked during the inheritance process

private File mTargetJarFile; // The destination client JAR to which the source will be copied to.

private boolean mUseFileCounter; // Whether a file counter or a size counter should be used for progress

private static final ThreadLocal<byte[]> sThreadLocalDownloadBuffer = new ThreadLocal<>();

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

if(activity != null){

isLocalProfile = Tools.isLocalProfile(activity);

// Force offline mode: disable online check

isOnline = false; // Tools.isOnline(activity);

Tools.switchDemo(Tools.isDemoProfile(activity));

} else {

isLocalProfile = true;

Tools.switchDemo(true);

}

sExecutorService.execute(() -> {

try {

// Force offline mode by always passing true

if(true /* offline mode forced for download */) {

String versionMessage = realVersion; // Use provided version unless we find its a modded instance

// See if provided version is a modded version and if that version depends on another jar, check for presence of both jar's .json.

try {

// This reads the .json associated with the provided version. If it fails, we can assume it's not installed.

File providedJsonFile = new File(Tools.DIR_HOME_VERSION + "/" + realVersion + "/" + realVersion + ".json");

JMinecraftVersionList.Version providedJson = Tools.GLOBAL_GSON.fromJson(Tools.read(providedJsonFile.getAbsolutePath()), JMinecraftVersionList.Version.class);

// This checks if running modded version that depends on other jars, so we use that for the error message.

File vanillaJsonFile = new File(Tools.DIR_HOME_VERSION + "/" + providedJson.inheritsFrom + "/" + providedJson.inheritsFrom + ".json");

versionMessage = providedJson.inheritsFrom != null ? providedJson.inheritsFrom : versionMessage;

// Ensure they're both not some 0 byte corrupted json

if (providedJsonFile.length() == 0 || (vanillaJsonFile.exists() && vanillaJsonFile.length() == 0)){

throw new RuntimeException("Minecraft "+versionMessage+ " is needed by " +realVersion);}

listener.onDownloadDone();

} catch (Exception e) {

String tryagain = "Please ensure you have an internet connection"; // Disabled Microsoft Account login check so changed message

Tools.showErrorRemote(versionMessage + " is not currently installed. "+ tryagain, e);

}

} else {

// force downloadGame without online check

try {

downloadGame(activity, version, realVersion);

listener.onDownloadDone();

} catch (Exception e) {

listener.onDownloadFailed(e);

}

}

ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT);

});

// Rest of the code remains unchanged
