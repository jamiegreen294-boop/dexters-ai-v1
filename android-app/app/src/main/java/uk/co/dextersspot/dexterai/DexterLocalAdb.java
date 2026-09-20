package uk.co.dextersspot.dexterai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Random;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbStream;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.KeyIdentifier;
import sun.security.x509.PrivateKeyUsageExtension;
import sun.security.x509.SubjectKeyIdentifierExtension;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public final class DexterLocalAdb extends AbsAdbConnectionManager {
    private static final String PREFS = "dexter_local_adb_keys";
    private static final String KEY_PRIVATE = "private_pkcs8";
    private static final String KEY_CERT = "certificate_x509";
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
        loadOrCreateSoftwareKeyPair();
    }

    private void loadOrCreateSoftwareKeyPair() throws Exception {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String privateB64 = p.getString(KEY_PRIVATE, null);
        String certB64 = p.getString(KEY_CERT, null);

        if (privateB64 != null && certB64 != null) {
            try {
                byte[] privateBytes = Base64.decode(privateB64, Base64.NO_WRAP);
                byte[] certBytes = Base64.decode(certB64, Base64.NO_WRAP);
                privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
                certificate = CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(certBytes));
                return;
            } catch (Exception ignored) {
                p.edit().remove(KEY_PRIVATE).remove(KEY_CERT).apply();
            }
        }

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, SecureRandom.getInstance("SHA1PRNG"));
        KeyPair pair = generator.generateKeyPair();
        privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();

        String subject = "CN=Dexter Business Phone";
        String algorithmName = "SHA512withRSA";
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 60000L);
        Date notAfter = new Date(now + (3650L * 86400000L));

        CertificateExtensions extensions = new CertificateExtensions();
        extensions.set("SubjectKeyIdentifier",
            new SubjectKeyIdentifierExtension(new KeyIdentifier(publicKey).getIdentifier()));
        extensions.set("PrivateKeyUsage",
            new PrivateKeyUsageExtension(notBefore, notAfter));

        X500Name x500Name = new X500Name(subject);
        X509CertInfo info = new X509CertInfo();
        info.set("version", new CertificateVersion(2));
        info.set("serialNumber",
            new CertificateSerialNumber(new Random().nextInt() & Integer.MAX_VALUE));
        info.set("algorithmID",
            new CertificateAlgorithmId(AlgorithmId.get(algorithmName)));
        info.set("subject", new CertificateSubjectName(x500Name));
        info.set("key", new CertificateX509Key(publicKey));
        info.set("validity", new CertificateValidity(notBefore, notAfter));
        info.set("issuer", new CertificateIssuerName(x500Name));
        info.set("extensions", extensions);

        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(privateKey, algorithmName);
        certificate = cert;

        p.edit()
            .putString(KEY_PRIVATE, Base64.encodeToString(privateKey.getEncoded(), Base64.NO_WRAP))
            .putString(KEY_CERT, Base64.encodeToString(certificate.getEncoded(), Base64.NO_WRAP))
            .apply();
    }

    @Override
    protected PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    protected Certificate getCertificate() {
        return certificate;
    }

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
            if (ok) {
                context.getSharedPreferences("dexter_local_adb", Context.MODE_PRIVATE)
                    .edit().putBoolean("paired", true).apply();
            }
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
