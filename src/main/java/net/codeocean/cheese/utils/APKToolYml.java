package net.codeocean.cheese.utils;






import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APKToolYml {
    public String version;
    public String apkFileName;
    public boolean isFrameworkApk;
    public boolean resourcesAreCompressed;
    public YamlUtils.UsesFramework usesFramework;
    public Map<String, String> sdkInfo;
    public YamlUtils.PackageInfo packageInfo;
    public YamlUtils.VersionInfo versionInfo;
    public boolean sharedLibrary;
    public boolean sparseResources;
    public Map<String, String> unknownFiles;
    public List<String> doNotCompress;

    public Map<String, String> getUnknownFiles() {
        return unknownFiles;
    }

    public void putUnknownFiles(Map<String, String> data) {
        if (data == null) return;

        if (unknownFiles == null) {
            unknownFiles = new HashMap<>();
        }

        unknownFiles.putAll(data);
    }

    public void addDoNotCompress(List<String> data) {
        if (data == null) return;
        if (doNotCompress == null) {
            doNotCompress = new ArrayList<>();
        }
        doNotCompress.addAll(data);
    }
}