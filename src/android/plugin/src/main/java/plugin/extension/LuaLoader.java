package plugin.utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import com.ansca.corona.CoronaRuntimeTask;
import com.ansca.corona.CoronaRuntimeTaskDispatcher;
import com.ansca.corona.purchasing.StoreServices;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.LuaType;
import com.naef.jnlua.NamedJavaFunction;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
    private int fLibRef;
    private int fListener;
    private CoronaRuntimeTaskDispatcher fDispatcher;

    /**
     * Creates a new Lua interface to this plugin.
     * <p>
     * Note that a new LuaLoader instance will not be created for every CoronaActivity instance.
     * That is, only one instance of this class will be created for the lifetime of the application process.
     * This gives a plugin the option to do operations in the background while the CoronaActivity is destroyed.
     */
    @SuppressWarnings("unused")
    public LuaLoader() {
        // Log.d("Corona", "Google billing plugin LuaLoader, fListener = " + fListener + " fBillingClient = " + fBillingClient);
        // Initialize member variables.
        fListener = CoronaLua.REFNIL;

        // Set up this plugin to listen for Corona runtime events to be received by methods
        // onLoaded(), onStarted(), onSuspended(), onResumed(), and onExiting().
        // CoronaEnvironment.addRuntimeListener(this);
    }

    /**
     * Warning! This method is not called on the main UI thread.
     */
    @Override
    public int invoke(LuaState L) {
        // Log.d("Corona", "Google billing plugin invoke, fListener = " + fListener + ", fBillingClient = " + fBillingClient);
        fDispatcher = new CoronaRuntimeTaskDispatcher(L);

        // Add functions to library
        NamedJavaFunction[] luaFunctions = new NamedJavaFunction[]{
                new GetInfoWrapper(),
        };

        String libName = L.toString(1);
        L.register(libName, luaFunctions);

        L.pushValue(-1);
        // fLibRef = L.ref(LuaState.REGISTRYINDEX);

        return 1;
    }

    private int getInfo(LuaState L) {
        int listenerIndex = 1;

        fListener = CoronaLua.REFNIL;
        if (CoronaLua.isListener(L, listenerIndex, "storeTransaction")) {
            fListener = CoronaLua.newRef(L, listenerIndex);
        }

        final String property ;
        if (L.type(1) == LuaType.STRING) {
            property = L.toString(1);
        } else {
            property = "";
        }
        if (property.equals("androidPackageSignatures")) {
            List<String> signatures =  getSignatures();
            if (signatures != null) {
                int count = 1;
                L.newTable();
                for (String value : signatures) {
                    L.pushString(value);
                    L.rawSet(-2, count);
                    count++;
                }
                return 1;
            }
        }

        return 0;
    }

    private static List<String> getSignatures() {
        try {
            Context ctx = CoronaEnvironment.getApplicationContext();
            PackageManager pm = ctx.getPackageManager();
            String packageName = ctx.getPackageName();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                if (packageInfo == null
                        || packageInfo.signingInfo == null) {
                    return null;
                }
                if(packageInfo.signingInfo.hasMultipleSigners()){
                    return signatureDigest(packageInfo.signingInfo.getApkContentsSigners());
                }
                else{
                    return signatureDigest(packageInfo.signingInfo.getSigningCertificateHistory());
                }
           }
            else {
                @SuppressLint("PackageManagerGetSignatures")
                PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                if (packageInfo == null
                        || packageInfo.signatures == null
                        || packageInfo.signatures.length == 0
                        || packageInfo.signatures[0] == null) {
                    return null;
                }
                return signatureDigest(packageInfo.signatures);
            }
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    private static String signatureDigest(Signature sig) {
        byte[] signature = sig.toByteArray();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] digest = md.digest(signature);
            // return BaseEncoding.base16().lowerCase().encode(digest);
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    private static List<String> signatureDigest(Signature[] sigList) {
        List<String> signaturesList= new ArrayList<>();
        for (Signature signature: sigList) {
            if(signature!=null) {
                signaturesList.add(signatureDigest(signature));
            }
        }
        return signaturesList;
    }

    @Override
    public void onLoaded(CoronaRuntime runtime) {

    }

    @Override
    public void onStarted(CoronaRuntime runtime) {

    }

    @Override
    public void onSuspended(CoronaRuntime runtime) {

    }

    @Override
    public void onResumed(CoronaRuntime runtime) {

    }

    @Override
    public void onExiting(CoronaRuntime runtime) {

    }

    private class GetInfoWrapper implements NamedJavaFunction {
        @Override
        public String getName() {
            return "getInfo";
        }

        @Override
        public int invoke(LuaState L) {
            return getInfo(L);
        }
    }
}
