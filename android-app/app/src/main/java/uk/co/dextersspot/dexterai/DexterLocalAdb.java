package uk.co.dextersspot.dexterai;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Calendar;

import javax.security.auth.x500.X500Principal;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbStream;

public final class DexterLocalAdb extends AbsAdbConnectionManager {
    private static final String KEY_ALIAS = "dexter_local_adb";
    private static DexterLocalAdb instance;
    private final Context context;
    private PrivateKey privateKey;
    private Certificate certificate;

    public static synchronized DexterLocalAdb get(Context context) throws Exception {
        if (instance == null) instance = new DexterLocalAdb(context.getApplicationContext());
        return instance;
    }

    private DexterLocalAdb(Context context) throws Exception {
        this.context = context;
        setApi(Build.VERSION.SDK_INT);
        loadOrCreateKey();
    }

    private void loadOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);

        if (!ks.containsAlias(KEY_ALIAS)) {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 20);

            KeyPairGenerator gen = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setCertificateSubject(new X500Principal("CN=Dexter Local Admin"))
                .setCertificateSerialNumber(BigInteger.ONE)
                .setCertificateNotBefore(start.getTime())
                .setCertificateNotAfter(end.getTime())
                .build();
            gen.initialize(spec);
            gen.generateKeyPair();
        }

        privateKey = (PrivateKey) ks.getKey(KEY_ALIAS, null);
        certificate = ks.getCertificate(KEY_ALIAS);
        if (privateKey == null || certificate == null) {
            throw new IllegalStateException("Dexter local ADB key unavailable");
        }
    }

    @NonNull
    @Override
    protected PrivateKey getPrivateKey() {
        return privateKey;
    }

    @NonNull
    @Override
    protected Certificate getCertificate() {
        return certificate;
    }

    @NonNull
    @Override
    protected String getDeviceName() {
        return "Dexter Business Phone";
    }

    public synchronized JSONObject pairLocal(String host, int pairingPort, String code) {
        JSONObject out = new JSONObject();
        try {
            boolean ok = pair(host, pairingPort, code);
            out.put("paired", ok);
            out.put("host", host);
            out.put("pairingPort", pairingPort);
        } catch (Exception e) {
            try { out.put("paired", false).put("error", safe(e)); } catch (Exception ignored) {}
        }
        return out;
    }

    public synchronized JSONObject connectLocal(String host, int adbPort) {
        JSONObject out = new JSONObject();
        try {
            boolean ok = connect(host, adbPort);
            out.put("connected", ok);
            out.put("host", host);
            out.put("adbPort", adbPort);
            if (ok) {
                context.getSharedPreferences("dexter_local_adb", Context.MODE_PRIVATE)
                    .edit().putString("host", host).putInt("port", adbPort).apply();
            }
        } catch (Exception e) {
            try { out.put("connected", false).put("error", safe(e)); } catch (Exception ignored) {}
        }
        return out;
    }

    public synchronized JSONObject runOwnerDiagnostics(String host, int adbPort) {
        JSONObject out = new JSONObject();
        try {
            if (!connect(host, adbPort)) {
                return out.put("connected", false).put("error", "ADB connection failed");
            }
            out.put("connected", true);
            out.put("dpmOwners", shell("dpm list-owners"));
            out.put("devicePolicy", shell("dumpsys device_policy"));
            out.put("package", shell("dumpsys package uk.co.dextersspot.dexterai"));
        } catch (Exception e) {
            try { out.put("error", safe(e)); } catch (Exception ignored) {}
        }
        return out;
    }

    private String shell(String command) throws Exception {
        AdbStream stream = openStream("shell:" + command);
        try {
            InputStream in = stream.openInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                int allowed = Math.min(n, 32768 - total);
                if (allowed > 0) out.write(buf, 0, allowed);
                total += allowed;
                if (total >= 32768) break;
            }
            return out.toString("UTF-8");
        } finally {
            try { stream.close(); } catch (Exception ignored) {}
        }
    }

    private static String safe(Exception e) {
        String m = e.getMessage();
        return (m == null || m.length() == 0) ? e.getClass().getSimpleName() : m;
    }
}
