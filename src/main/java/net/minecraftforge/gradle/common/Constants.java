package net.minecraftforge.gradle.common;

import groovy.lang.Closure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.minecraftforge.gradle.StringUtils;
import net.minecraftforge.gradle.dev.DevExtension;
import net.minecraftforge.gradle.json.version.OS;

import org.gradle.api.Project;

import argo.jdom.JdomParser;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;

public class Constants
{
    // OS
    public static enum SystemArch
    {
        BIT_32, BIT_64;

        public String toString()
        {
            return StringUtils.lower(name()).replace("bit_", "");
        }
    }

    public static final OS               OPERATING_SYSTEM = OS.CURRENT;
    public static final SystemArch       SYSTEM_ARCH      = getArch();

    // extension nam
    public static final String EXT_NAME_MC      = "minecraft";
    public static final String EXT_NAME_JENKINS = "jenkins";

    // json parser
    public static final JdomParser PARSER = new JdomParser();

    @SuppressWarnings("serial")
    public static final Closure<Boolean> CALL_FALSE = new Closure<Boolean>(null){ public Boolean call(Object o){ return false; }};

    // urls
    public static final String MC_JAR_URL       = "http://s3.amazonaws.com/Minecraft.Download/versions/{MC_VERSION}/{MC_VERSION}.jar";
    public static final String MC_SERVER_URL    = "http://s3.amazonaws.com/Minecraft.Download/versions/{MC_VERSION}/minecraft_server.{MC_VERSION}.jar";
    public static final String MCP_URL          = "http://files.minecraftforge.net/fernflower_temporary.zip";
    public static final String ASSETS_URL       = "http://resources.download.minecraft.net";
    public static final String LIBRARY_URL      = "https://libraries.minecraft.net/";
    public static final String FORGE_MAVEN      = "http://files.minecraftforge.net/maven";
    public static final String ASSETS_INDEX_URL = "https://s3.amazonaws.com/Minecraft.Download/indexes/{ASSET_INDEX}.json";

    public static final String LOG              = ".gradle/gradle.log";
    public static final String ASSETS_INDEX     =  "legacy";

    // things in the cache dir.
    public static final String JAR_CLIENT_FRESH = "{CACHE_DIR}/minecraft/net/minecraft/minecraft/{MC_VERSION}/minecraft-{MC_VERSION}.jar";
    public static final String JAR_SERVER_FRESH = "{CACHE_DIR}/minecraft/net/minecraft/minecraft_server/{MC_VERSION}/minecraft_server-{MC_VERSION}.jar";
    public static final String JAR_MERGED       = "{CACHE_DIR}/minecraft/net/minecraft/minecraft_merged/{MC_VERSION}/minecraft_merged-{MC_VERSION}.jar";
    public static final String FERNFLOWER       = "{CACHE_DIR}/minecraft/fernflower.jar";
    public static final String EXCEPTOR         = "{CACHE_DIR}/minecraft/exceptor.jar";
    public static final String ASSETS           = "{CACHE_DIR}/minecraft/assets";

    // util
    public static final String NEWLINE = System.getProperty("line.separator");
    private static final OutputStream NULL_OUT = new OutputStream()
    {
        public void write(int b) throws IOException{}
    };


    // helper methods
    public static File cacheFile(Project project, String... otherFiles)
    {
        return Constants.file(project.getGradle().getGradleUserHomeDir(), otherFiles);
    }

    public static File file(File file, String... otherFiles)
    {
        String othersJoined = Joiner.on('/').join(otherFiles);
        return new File(file, othersJoined);
    }

    public static File file(String... otherFiles)
    {
        String othersJoined = Joiner.on('/').join(otherFiles);
        return new File(othersJoined);
    }

    public static List<String> getClassPath()
    {
        URL[] urls = ((URLClassLoader) DevExtension.class.getClassLoader()).getURLs();

        ArrayList<String> list = new ArrayList<String>();
        for (URL url : urls)
        {
            list.add(url.getPath());
        }
        return list;
    }
    
    public static File getMinecraftDirectory()
    {
        String userDir = System.getProperty("user.home");

        switch (OPERATING_SYSTEM)
            {
                case LINUX:
                    return new File(userDir, ".minecraft/");
                case WINDOWS:
                    String appData = System.getenv("APPDATA");
                    String folder = appData != null ? appData : userDir;
                    return new File(folder, ".minecraft/");
                case OSX:
                    return new File(userDir, "Library/Application Support/minecraft");
                default:
                    return new File(userDir, "minecraft/");
            }
    }

    private static SystemArch getArch()
    {
        String name = StringUtils.lower(System.getProperty("os.arch"));
        if (name.contains("64"))
        {
            return SystemArch.BIT_64;
        }
        else
        {
            return SystemArch.BIT_32;
        }
    }

    public static String hash(File file)
    {
        if (file.getPath().endsWith(".zip") || file.getPath().endsWith(".jar"))
            return hashZip(file, "MD5");
        else
            return hash(file, "MD5");
    }
    
    public static List<String> hashAll(File file)
    {
        LinkedList<String> list = new LinkedList<String>();
        
        if (file.isDirectory())
        {
            for (File f : file.listFiles())
                hashAll(f);
        }
        else
            list.add(hash(file));
        
        return list;
    }

    public static String hash(File file, String function)
    {
        try
        {
            MessageDigest hasher = MessageDigest.getInstance(function);
            
            InputStream fis = new FileInputStream(file);
            hasher.update(ByteStreams.toByteArray(fis));
            fis.close();
            
            byte[] hash = hasher.digest();

            // convert to string
            String result = "";
            for (int i = 0; i < hash.length; i++)
                result += Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1);
            return result;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }
    
    public static String hashZip(File file, String function)
    {
        try
        {
            MessageDigest hasher = MessageDigest.getInstance(function);

            ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry = null;
            while ((entry = zin.getNextEntry()) != null)
            {
                hasher.update(entry.getName().getBytes());
                hasher.update(ByteStreams.toByteArray(zin));
            }
            zin.close();
            
            byte[] hash = hasher.digest();

            
            // convert to string
            String result = "";

            for (int i = 0; i < hash.length; i++)
            {
                result += Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1);
            }
            return result;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static String hash(String str)
    {
        try
        {
            MessageDigest complete = MessageDigest.getInstance("MD5");
            byte[] hash = complete.digest(str.getBytes());

            String result = "";

            for (int i = 0; i < hash.length; i++)
            {
                result += Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1);
            }
            return result;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * DON'T FORGET TO CLOSE
     */
    public static OutputStream getNullStream()
    {
        return NULL_OUT;
    }
}

